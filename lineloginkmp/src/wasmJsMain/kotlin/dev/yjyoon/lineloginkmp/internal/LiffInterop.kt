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
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package dev.yjyoon.lineloginkmp.internal

import kotlin.js.Promise

// The whole LIFF surface this library touches, as top-level js() wrappers — Kotlin/Wasm allows
// js() only as the entire body of a top-level function, so there is one function per call rather
// than an external object. Every wrapper assumes the SDK script has already loaded; the only one
// safe before that is [loadLiffScript] itself.

/** LINE's own CDN for the LIFF SDK. `edge/2` tracks the latest 2.x, which is what LINE documents. */
internal const val LIFF_SDK_URL: String = "https://static.line-scdn.net/liff/edge/2/sdk.js"

/**
 * Injects the SDK `<script>` once. Resolves immediately when `globalThis.liff` already exists, so
 * an app that ships the SDK itself — a CSP that forbids LINE's CDN, an npm bundle — wins and this
 * loader steps aside.
 */
internal fun loadLiffScript(url: String): Promise<JsAny?> =
    js(
        """(new Promise(function(resolve, reject) {
        if (typeof globalThis.liff !== 'undefined') { resolve(null); return; }
        var script = document.createElement('script');
        script.src = url;
        script.onload = function() { resolve(null); };
        script.onerror = function() { reject(new Error('Could not load the LIFF SDK from ' + url)); };
        document.head.appendChild(script);
    }))""",
    )

internal fun liffInit(liffId: String): Promise<JsAny?> = js("globalThis.liff.init({ liffId: liffId })")

internal fun liffIsLoggedIn(): Boolean = js("globalThis.liff.isLoggedIn()")

internal fun liffIsInClient(): Boolean = js("globalThis.liff.isInClient()")

/** Starts a full-page redirect to LINE's login in an external browser. It does not return a result. */
internal fun liffLogin(): Unit = js("globalThis.liff.login()")

internal fun liffLogout(): Unit = js("globalThis.liff.logout()")

internal fun liffGetAccessToken(): String? = js("globalThis.liff.getAccessToken()")

internal fun liffGetIdToken(): String? = js("globalThis.liff.getIDToken()")

internal fun liffGetDecodedIdToken(): JsAny? = js("globalThis.liff.getDecodedIDToken()")

internal fun liffGetProfile(): Promise<JsAny?> = js("globalThis.liff.getProfile()")

/**
 * Asks LINE how long the access token has left, because LIFF does not say. `expires_in` arrives in
 * seconds. The endpoint sends `access-control-allow-origin: *`, verified against production —
 * without that this call would be impossible from a browser at all.
 */
internal fun fetchTokenExpiry(accessToken: String): Promise<JsAny?> =
    js(
        """(fetch('https://api.line.me/oauth2/v2.1/verify?access_token=' + encodeURIComponent(accessToken))
        .then(function(response) {
            if (!response.ok) { throw new Error('verify returned HTTP ' + response.status); }
            return response.json();
        }))""",
    )

// Field access on values that came back from JS. Nulls and absent keys both surface as Kotlin null.

internal fun jsStringField(
    obj: JsAny,
    key: String,
): String? = js("(obj[key] == null ? null : String(obj[key]))")

internal fun jsDoubleField(
    obj: JsAny,
    key: String,
): Double = js("(obj[key] == null ? 0 : Number(obj[key]))")

internal fun epochMilliseconds(): Double = js("Date.now()")
