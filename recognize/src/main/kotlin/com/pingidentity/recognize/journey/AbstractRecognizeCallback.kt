/*
 * Copyright (c) 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.pingidentity.recognize.journey

import com.pingidentity.journey.plugin.AbstractCallback
import com.pingidentity.journey.plugin.Callback
import com.pingidentity.journey.plugin.ValueCallback
import com.pingidentity.journey.plugin.callbacks
import com.pingidentity.orchestrate.ContinueNode
import com.pingidentity.orchestrate.ContinueNodeAware
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * ID suffix used to locate the signal [ValueCallback] in the sibling callback list.
 *
 * TODO: confirm the exact field name(s) the Recognize server node uses (analogous to
 *       `pingone_risk_evaluation_signals` in Protect).
 */
const val RECOGNIZE_SIGNALS = "recognize_signals"

/** ID suffix used to locate the client-error [ValueCallback]. */
const val CLIENT_ERROR = "clientError"

/**
 * Abstract base for Recognize Journey callbacks.
 *
 * Handles the dual-mode nature of Recognize callbacks: they can arrive either as first-class
 * typed callbacks (e.g. `RecognizeInitializeCallback`) or wrapped inside a `MetadataCallback`
 * — mirroring the [AbstractProtectCallback] pattern exactly.
 *
 * Subclasses override [init] to parse their specific JSON fields and expose typed properties.
 */
abstract class AbstractRecognizeCallback : ContinueNodeAware, AbstractCallback() {

    override lateinit var continueNode: ContinueNode
    private var derivedCallback: Boolean = false

    override fun init(jsonObject: JsonObject): Callback {
        val type = jsonObject["type"]?.jsonPrimitive?.content
        if (type == "MetadataCallback") {
            derivedCallback = true
            jsonObject["output"]?.jsonArray?.let { output ->
                output[0].jsonObject.let { nameValuePair ->
                    if (nameValuePair["name"]?.jsonPrimitive?.contentOrNull == "data") {
                        nameValuePair["value"]?.jsonObject?.let { value ->
                            value.forEach { attr -> init(attr.key, attr.value) }
                        }
                    }
                }
            }
            return this
        } else {
            return super.init(jsonObject)
        }
    }

    /**
     * Submits a client-side error string to the server.
     *
     * @param value Error description / error-type token.
     */
    fun error(value: String) {
        if (derivedCallback) valueCallbackError(value) else input(value)
    }

    /**
     * Submits the recognition signal (and optionally an error) to the server.
     *
     * @param signal The JWS / signal payload returned by the Recognize SDK.
     * @param error  An error string; empty if no error occurred.
     */
    fun signal(signal: String, error: String) {
        if (derivedCallback) {
            if (signal.isNotEmpty()) valueCallbackSignal(signal)
            if (error.isNotEmpty()) valueCallbackError(error)
        } else {
            input(signal, error)
        }
    }

    private fun valueCallbackSignal(value: String) {
        continueNode.callbacks.forEach { callback ->
            if (callback is ValueCallback && callback.id.contains(RECOGNIZE_SIGNALS)) {
                callback.value = value
            }
        }
    }

    private fun valueCallbackError(value: String) {
        continueNode.callbacks.forEach { callback ->
            if (callback is ValueCallback && callback.id.contains(CLIENT_ERROR)) {
                callback.value = value
            }
        }
    }
}
