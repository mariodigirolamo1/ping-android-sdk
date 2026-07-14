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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Abstract base for Recognize Journey callbacks.
 *
 * Handles the dual-mode nature of Recognize callbacks: they can arrive either as first-class
 * typed callbacks (e.g. `PingOneRecognizeCallback`) or wrapped inside a `MetadataCallback`.
 *
 * Subclasses override [init] to parse their specific JSON fields and expose typed properties.
 */
abstract class AbstractRecognizeCallback : ContinueNodeAware, AbstractCallback() {

    override lateinit var continueNode: ContinueNode
    protected var derivedCallback: Boolean = false

    // ── Common output fields ──────────────────────────────────────────────────

    /** URL of the PingOne Recognize authentication service WebSocket. */
    var websocketURL: String = ""
        private set

    /** Customer / tenant name configured on the server. */
    var customerName: String = ""
        private set

    /** RSA public key used to encrypt images before transmission. */
    var imageEncryptionPublicKey: String = ""
        private set

    /** Key ID that corresponds to [imageEncryptionPublicKey]. */
    var imageEncryptionKeyId: String = ""
        private set

    /** PingOne Recognize node host URL. */
    var host: String = ""
        private set

    /** API key for the Keyless / Recognize SDK. */
    var apiKey: String = ""
        private set

    /** Username of the subject. */
    var username: String = ""
        private set

    /** Opaque transaction data to be signed by the SDK. */
    var transactionData: String = ""
        private set

    /** JWT audience claim forwarded to the Keyless SDK. */
    var audience: String = ""
        private set

    /** Indicates whether the SDK should generate a new client state. */
    var generateClientState: String = ""
        private set

    /** Existing client state payload supplied by the server. */
    var clientState: String = ""
        private set

    /**
     * Options forwarded to the PingOne Recognize **mobile** SDK.
     *
     * Relevant keys include `livenessConfiguration`, `operationInfoId`,
     * `operationInfoPayload`, `customSecret`, `presentation`, etc.
     */
    var mobileSDKOptions: JsonObject = JsonObject(emptyMap())
        private set

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
     * Parses a single named output field and stores it in the corresponding property.
     *
     * Called once per field in the server JSON output array (or per key in the `data` envelope).
     * Subclasses may override this and call `super.init(name, value)` to handle any
     * operation-specific fields in addition to these common ones.
     *
     * @param name Field name as returned by the server (e.g. `"transactionData"`).
     * @param value Raw JSON element for the field value.
     */
    override fun init(name: String, value: JsonElement) {
        when (name) {
            "websocketURL" -> websocketURL = value.jsonPrimitive.content
            "customerName" -> customerName = value.jsonPrimitive.content
            "imageEncryptionPublicKey" -> imageEncryptionPublicKey = value.jsonPrimitive.content
            "imageEncryptionKeyId" -> imageEncryptionKeyId = value.jsonPrimitive.content
            "host" -> host = value.jsonPrimitive.content
            "apiKey" -> apiKey = value.jsonPrimitive.content
            "username" -> username = value.jsonPrimitive.content
            "transactionData" -> transactionData = value.jsonPrimitive.content
            "audience" -> audience = value.jsonPrimitive.content
            "generateClientState" -> generateClientState = value.jsonPrimitive.content
            "clientState" -> clientState = value.jsonPrimitive.content
            "mobileSDKOptions" -> if (value is JsonObject) mobileSDKOptions = value
            else -> {}
        }
    }

    protected fun setValueCallback(idSuffix: String, value: String) {
        continueNode.callbacks.forEach { callback ->
            if (callback is ValueCallback && callback.id.contains(idSuffix)) {
                callback.value = value
            }
        }
    }

    internal companion object {
        const val SIGNED_JWT_SUFFIX = "signedJwt"
        const val CLIENT_STATE_SUFFIX = "clientState"
        const val RECOGNIZE_ID_SUFFIX = "recognizeId"
        const val DEVICE_PUBLIC_SIGNING_KEY_SUFFIX = "devicePublicSigningKey"
        const val CLIENT_ERROR_SUFFIX = "clientError"
        const val CLIENT_ERROR_CODE_SUFFIX = "clientErrorCode"
    }
}
