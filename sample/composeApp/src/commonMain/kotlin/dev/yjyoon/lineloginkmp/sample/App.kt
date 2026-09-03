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
package dev.yjyoon.lineloginkmp.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.yjyoon.lineloginkmp.LineLogin
import dev.yjyoon.lineloginkmp.LineLoginResult
import dev.yjyoon.lineloginkmp.LineLogoutResult
import dev.yjyoon.lineloginkmp.compose.LineLoginButton
import dev.yjyoon.lineloginkmp.compose.LineLoginButtonDefaults
import kotlinx.coroutines.launch

/**
 * The entire sample: configure once, call `login()`, render whatever comes back.
 *
 * Deliberately one file and no architecture — every line here is either the library's API or the
 * least Compose needed to show its result.
 */
@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            var status by remember { mutableStateOf("Not signed in") }
            var busy by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            // Two different questions, and only the first one is about *this* start.
            //
            // resumePendingLogin() is the web's other half: a browser login completes during
            // configure() on the page LINE redirects back to, with no login() call left to receive
            // it. Without this the sample holds a valid session and still says "Not signed in"
            // until someone presses the button a second time — which is the bug this API exists to
            // remove. It is null on Android and iOS, where a login always returns to its caller.
            //
            // isLoggedIn() is the older question — "has this device signed in before?" — and it is
            // answered without performing a login, because on Android performing one would launch
            // an Activity for a login nobody asked for.
            LaunchedEffect(Unit) {
                val resumed = LineLogin.resumePendingLogin()
                status =
                    when {
                        resumed != null -> "Signed in · " + describe(resumed)
                        LineLogin.isLoggedIn() -> "Already signed in · tap to refresh the token"
                        else -> status
                    }
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                // At least the isolation zone the guidelines require around the login button.
                verticalArrangement = Arrangement.spacedBy(LineLoginButtonDefaults.isolationZone() * 2),
            ) {
                Text(
                    text = "line-login-kmp",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )

                // Which route a login takes depends on this, so it is worth seeing while testing:
                // app-to-app when LINE is installed, the browser when it is not. Needs no
                // configure() call, and needs no platform code here — it is the same shared API on
                // both platforms. A real screen that cares would re-check on resume, since the user
                // can install or remove LINE while the app sits in the background.
                val lineAppInstalled by
                    produceState<Boolean?>(initialValue = null) {
                        value = LineLogin.isLineAppInstalled()
                    }

                Text(
                    text =
                        when (lineAppInstalled) {
                            true -> "LINE app installed · login goes app-to-app"
                            false -> "LINE app not installed · login falls back to the browser"
                            null -> "Looking for the LINE app…"
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                // The login button comes from :lineloginkmp-compose: LINE's own icon and caption,
                // and colours, divider, radius and padding taken from LINE's button artwork. The
                // caption follows the device's language.
                //
                // While a login is running the button is disabled rather than showing a spinner:
                // the guidelines define a disabled state, and inventing a loading one would mean
                // painting a colour they do not allow.
                LineLoginButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            status = describe(LineLogin.login())
                            busy = false
                        }
                    },
                    enabled = !busy,
                    // Deliberately stretched, because that is how a real sign-in screen uses it —
                    // and because nothing here rendering a stretched button is exactly how the icon
                    // square came to drift inward once.
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            status =
                                when (val result = LineLogin.logout()) {
                                    LineLogoutResult.Success -> "Signed out"
                                    is LineLogoutResult.Failure ->
                                        "Signed out locally, but LINE was not told: ${result.message}"
                                }
                            busy = false
                        }
                    },
                    enabled = !busy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Log out")
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val token = LineLogin.currentAccessToken()
                            status =
                                if (token == null) {
                                    "No stored token"
                                } else {
                                    "Stored token expires at ${token.expiresAtEpochMilliseconds}"
                                }
                        }
                    },
                    enabled = !busy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Check stored token")
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = status,
                        modifier = Modifier.background(Color.Transparent).padding(16.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

private fun describe(result: LineLoginResult): String =
    when (result) {
        is LineLoginResult.Success ->
            buildString {
                appendLine("Signed in")
                appendLine("userId: ${result.profile?.userId}")
                appendLine("displayName: ${result.profile?.displayName}")
                appendLine("pictureUrl: ${result.profile?.pictureUrl}")
                appendLine("idToken: ${if (result.idToken == null) "none" else "present"}")
                appendLine("email: ${result.idToken?.email ?: "not granted"}")
                append("expiresAt: ${result.accessToken.expiresAtEpochMilliseconds}")
            }

        // Nothing went wrong — the user simply changed their mind.
        LineLoginResult.Cancelled -> "Cancelled"

        is LineLoginResult.Failure ->
            buildString {
                appendLine("Failed: ${result.code}")
                appendLine(result.message)
                append("raw: ${result.rawCode} / ${result.rawMessage}")
            }
    }
