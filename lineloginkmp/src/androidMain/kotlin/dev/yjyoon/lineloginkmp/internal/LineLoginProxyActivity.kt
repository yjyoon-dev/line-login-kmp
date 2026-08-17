/*
 * Copyright 2026 yjyoon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.yjyoon.lineloginkmp.internal

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.linecorp.linesdk.Scope
import com.linecorp.linesdk.auth.LineAuthenticationParams
import com.linecorp.linesdk.auth.LineLoginApi
import dev.yjyoon.lineloginkmp.LineBotPrompt
import dev.yjyoon.lineloginkmp.LineLoginErrorCode
import dev.yjyoon.lineloginkmp.LineLoginRequest
import dev.yjyoon.lineloginkmp.LineLoginResult
import dev.yjyoon.lineloginkmp.LineScope

/**
 * A headless, translucent Activity that bridges the LINE SDK's `startActivityForResult` contract
 * to a coroutine in shared code.
 *
 * It exists because the SDK publishes no `ActivityResultContract` and shared code holds no
 * Activity. Rather than making the consumer expose one from their own Activity — which goes stale
 * on every recreation and pins it in a process-level field — each login starts one of these and
 * finishes it as soon as there is a result.
 *
 * Both of the SDK's own auth Activities are translucent too, so the whole flow draws over the
 * consumer's UI without a white flash.
 *
 * The class name is effectively public API: it appears in every consumer's merged manifest.
 * Renaming or moving it is a breaking change.
 */
internal class LineLoginProxyActivity : Activity() {
    private val requestId: String?
        get() = intent?.getStringExtra(EXTRA_REQUEST_ID)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requestId = this.requestId
        val channelId = intent?.getStringExtra(EXTRA_CHANNEL_ID)
        val pending = requestId?.let(LineLoginRequestBus::find)

        // Nobody is waiting: either the process was killed and restored, or the caller's coroutine
        // was cancelled while this was backgrounded. Do not reopen a login no one will receive.
        if (requestId == null || channelId.isNullOrBlank() || pending == null) {
            finish()
            return
        }

        pending.attach(this)

        // A recreation redelivers the in-flight result to onActivityResult; starting again here
        // would stack a second authentication on top of the first.
        if (savedInstanceState != null) return

        try {
            startActivityForResult(buildLoginIntent(channelId), REQUEST_CODE_LOGIN)
        } catch (error: Exception) {
            // Anything thrown here would otherwise escape onCreate, crashing the app *and* leaving
            // the caller suspended forever.
            pending.outcome.complete(
                LineLoginResult.Failure(
                    code = LineLoginErrorCode.Internal,
                    message = error.message ?: defaultMessageFor(LineLoginErrorCode.Internal),
                ),
            )
            finish()
        }
    }

    /**
     * The safety net for every way this Activity can die without an activity result.
     *
     * `onActivityResult` is the happy path, but it is not the only path: a deep link launched with
     * `FLAG_ACTIVITY_CLEAR_TOP`, a `finishAffinity()`, the system tearing the task down — all of
     * them destroy this Activity mid-login with no result. Without this, `outcome` would never
     * complete, `login()` would suspend forever, and because the caller holds `LineLogin`'s login
     * mutex across that suspension, **every subsequent login in the process** would fail with
     * `LoginInProgress`.
     *
     * It belongs in `onDestroy` rather than in `finish()`: a system-initiated teardown never calls
     * the app-side `finish()` override at all.
     *
     * `complete` is idempotent, so this never overwrites a real result — including one delivered by
     * `onActivityResult` moments earlier — and `isChangingConfigurations` keeps a rotation from
     * being mistaken for the user giving up.
     */
    override fun onDestroy() {
        super.onDestroy()
        if (!isFinishing || isChangingConfigurations) return
        requestId?.let(LineLoginRequestBus::find)?.outcome?.complete(LineLoginResult.Cancelled)
    }

    private fun buildLoginIntent(channelId: String): Intent {
        val scopeCodes = intent?.getStringArrayListExtra(EXTRA_SCOPES).orEmpty()
        val params =
            LineAuthenticationParams
                .Builder()
                .scopes(scopeCodes.map { Scope(it) })
                .apply {
                    intent?.getStringExtra(EXTRA_NONCE)?.let { nonce(it) }
                    intent?.getStringExtra(EXTRA_PROMPT_BOT_ID)?.let { promptBotID(it) }
                    intent?.getStringExtra(EXTRA_BOT_PROMPT)?.let { name ->
                        botPrompt(
                            when (LineBotPrompt.valueOf(name)) {
                                LineBotPrompt.Normal -> LineAuthenticationParams.BotPrompt.normal
                                LineBotPrompt.Aggressive -> LineAuthenticationParams.BotPrompt.aggressive
                            },
                        )
                    }
                }.build()

        return if (intent?.getBooleanExtra(EXTRA_FORCE_WEB_LOGIN, false) == true) {
            LineLoginApi.getLoginIntentWithoutLineAppAuth(this, channelId, params)
        } else {
            LineLoginApi.getLoginIntent(this, channelId, params)
        }
    }

    // `startActivityForResult` / `onActivityResult` are deprecated on AndroidX's ComponentActivity,
    // not on the platform Activity used here. Extending the platform class keeps androidx.activity
    // out of every consumer's dependency graph for the sake of one call.
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CODE_LOGIN) return

        val pending = requestId?.let(LineLoginRequestBus::find)
        if (pending == null) {
            finish()
            return
        }

        // The SDK reports a user cancel as RESULT_OK carrying LineApiResponseCode.CANCEL, so the
        // result code says nothing on its own. RESULT_CANCELED with no data is different: the
        // SDK's Activity died without setting any result, and feeding that null Intent to
        // getLoginResultFromIntent would manufacture an INTERNAL_ERROR out of a plain cancel.
        val result =
            if (resultCode == RESULT_CANCELED && data == null) {
                LineLoginResult.Cancelled
            } else {
                LineLoginApi.getLoginResultFromIntent(data).toLineLoginResult()
            }

        pending.outcome.complete(result)
        finish()
    }

    override fun finish() {
        requestId?.let(LineLoginRequestBus::find)?.detach()
        super.finish()
        // Nothing is drawn here, so an enter/exit animation would only flash over the app's UI.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    companion object {
        /** Arbitrary, but must stay below 0xFFFF and constant across releases. */
        private const val REQUEST_CODE_LOGIN = 0x11E5

        fun intent(
            context: Context,
            requestId: String,
            channelId: String,
            request: LineLoginRequest,
        ): Intent =
            Intent(context, LineLoginProxyActivity::class.java)
                // Required: the caller has an application Context, not an Activity one.
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_REQUEST_ID, requestId)
                .putExtra(EXTRA_CHANNEL_ID, channelId)
                .putStringArrayListExtra(EXTRA_SCOPES, ArrayList(request.scopes.map(LineScope::code)))
                .putExtra(EXTRA_NONCE, request.nonce)
                .putExtra(EXTRA_FORCE_WEB_LOGIN, request.forceWebLogin)
                .putExtra(EXTRA_BOT_PROMPT, request.botPrompt?.name)
                .putExtra(EXTRA_PROMPT_BOT_ID, request.promptBotId)
    }
}
