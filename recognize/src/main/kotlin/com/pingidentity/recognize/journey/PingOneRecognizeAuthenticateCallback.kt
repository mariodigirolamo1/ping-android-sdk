/*
 * Copyright (c) 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.pingidentity.recognize.journey

import com.pingidentity.recognize.Recognize
import io.keyless.sdk.biom.liveness.LivenessSettings
import io.keyless.sdk.configurations.ClientStateType
import io.keyless.sdk.configurations.OperationInfo
import io.keyless.sdk.configurations.PresentationStyle
import io.keyless.sdk.configurations.SetupConfig
import io.keyless.sdk.configurations.auth.BiomAuthConfig
import io.keyless.sdk.core.actions.model.JwtSigningInfo
import io.keyless.sdk.errorshandling.AuthenticationSuccess
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Journey callback that handles PingOne Recognize **authentication** operations.
 *
 * This callback is returned by [RecognizeCallback] when the server output contains
 * `operationType = "AUTHENTICATE"`. All output fields are parsed by [AbstractRecognizeCallback];
 * this class only adds the [authenticate] operation.
 *
 * On success, the signed JWT and client state are submitted to the Journey via the six
 * input fields: `IDToken1signedJwt`, `IDToken1clientState`, `IDToken1recognizeId` (empty),
 * `IDToken1devicePublicSigningKey`, `IDToken1clientError`, `IDToken1clientErrorCode`.
 *
 * @see RecognizeCallback
 * @see PingOneRecognizeEnrollCallback
 */
class PingOneRecognizeAuthenticateCallback : AbstractRecognizeCallback() {

    /**
     * Performs the PingOne Recognize authentication ceremony.
     *
     * Automatically maps every server-supplied output field to the corresponding
     * [BiomAuthConfig] parameter:
     *
     * | Callback field / mobileSDKOptions key           | BiomAuthConfig property              |
     * |-------------------------------------------------|--------------------------------------|
     * | `transactionData`                               | `jwtSigningInfo.claimTransactionData` |
     * | `generateClientState` (`"true"` → BACKUP)       | `generatingClientState`              |
     * | `mobileSDKOptions.operationInfoId`              | `operationInfo.operationId`          |
     * | `mobileSDKOptions.operationInfoPayload`         | `operationInfo.payload`              |
     * | `mobileSDKOptions.operationInfoExternalUserId`  | `operationInfo.externalUserId`       |
     * | `mobileSDKOptions.livenessConfiguration`        | `livenessConfiguration`              |
     * | `mobileSDKOptions.livenessEnvironmentAware`     | `livenessEnvironmentAware`           |
     * | `mobileSDKOptions.cameraDelaySeconds`           | `cameraDelaySeconds`                 |
     * | `mobileSDKOptions.showSuccessFeedback`          | `showSuccessFeedback`                |
     * | `mobileSDKOptions.shouldRetriveAuthenticationFrame` | `shouldRetrieveAuthenticationFrame` |
     * | `mobileSDKOptions.presentationStyle`            | `presentationStyle`                  |
     * | `mobileSDKOptions.shouldRemovePin`              | `shouldRemovePin`                    |
     *
     * Note: `showFailureFeedback`, `shouldRetrieveSecret`, and `shouldDeleteSecret` are present
     * in the server spec but are not mapped — `BiomAuthConfig` (SDK 5.8.4) does not expose
     * boolean fields for these; they are handled via typed `KeylessSecret` parameters instead.
     *
     * @return [Result] containing [AuthenticationSuccess] on success, or a [Throwable] on failure.
     */
    suspend fun authenticate(): Result<AuthenticationSuccess> {
        val setupConfig = SetupConfig(
            apiKey = this@PingOneRecognizeAuthenticateCallback.apiKey,
            hosts = listOf(this@PingOneRecognizeAuthenticateCallback.host)
        )

        val opts = mobileSDKOptions
        val storedTransactionData = transactionData
        val storedAudience = audience
        val storedGenerateClientState = generateClientState

        val opId = opts["operationInfoId"]?.jsonPrimitive?.contentOrNull
        val opPayload = opts["operationInfoPayload"]?.jsonPrimitive?.contentOrNull
        val opExternalUserId = opts["operationInfoExternalUserId"]?.jsonPrimitive?.contentOrNull
        val operationInfo = if (opId != null || opPayload != null || opExternalUserId != null) {
            OperationInfo(
                /* operationId = */ opId ?: "",
                /* payload = */ opPayload ?: "",
                /* externalUserId = */ opExternalUserId ?: ""
            )
        } else null

        // "true" → ClientStateType.BACKUP, "false"/empty → null
        val storedGeneratingClientState = if (storedGenerateClientState.equals("true", ignoreCase = true)) {
            ClientStateType.BACKUP
        } else null

        // Seed a base instance to read SDK defaults for fields not supplied by the server
        val base = BiomAuthConfig()
        val jwtSigningInfo = if (storedAudience.isNotBlank()) {
            JwtSigningInfo(claimTransactionData = storedTransactionData, audience = storedAudience)
        } else {
            JwtSigningInfo(claimTransactionData = storedTransactionData)
        }

        val biomAuthConfig = BiomAuthConfig(
            shouldRemovePin = opts["shouldRemovePin"]?.jsonPrimitive?.contentOrNull
                ?.toBoolean() ?: base.shouldRemovePin,
            operationInfo = operationInfo,
            jwtSigningInfo = jwtSigningInfo,
            dynamicLinkingInfo = base.dynamicLinkingInfo,
            shouldRetrieveTemporaryState = base.shouldRetrieveTemporaryState,
            livenessConfiguration = opts["livenessConfiguration"]?.jsonPrimitive?.contentOrNull
                ?.let { runCatching { LivenessSettings.LivenessConfiguration.valueOf(it) }.getOrNull() }
                ?: base.livenessConfiguration,
            livenessEnvironmentAware = opts["livenessEnvironmentAware"]?.jsonPrimitive?.contentOrNull
                ?.toBoolean() ?: base.livenessEnvironmentAware,
            cameraDelaySeconds = opts["cameraDelaySeconds"]?.jsonPrimitive?.contentOrNull
                ?.toIntOrNull() ?: base.cameraDelaySeconds,
            showSuccessFeedback = opts["showSuccessFeedback"]?.jsonPrimitive?.contentOrNull
                ?.toBoolean() ?: base.showSuccessFeedback,
            deviceToRevoke = base.deviceToRevoke,
            // Auth uses key "presentationStyle"; enroll uses "presentation"
            presentationStyle = opts["presentationStyle"]?.jsonPrimitive?.contentOrNull
                ?.let { runCatching { PresentationStyle.valueOf(it) }.getOrNull() }
                ?: base.presentationStyle,
            generatingClientState = storedGeneratingClientState,
            // Server sends this key with a typo (missing 'e' in 'Retrieve')
            shouldRetrieveAuthenticationFrame = opts["shouldRetriveAuthenticationFrame"]
                ?.jsonPrimitive?.contentOrNull?.toBoolean() ?: base.shouldRetrieveAuthenticationFrame,
            savingSecret = base.savingSecret,
            deletingSecret = base.deletingSecret,
            retrievingSecret = base.retrievingSecret,
            shouldRetrieveSecretIDs = base.shouldRetrieveSecretIDs,
        )

        // Chain setup → authenticate so a setup failure is also reported back to the server
        // via input() before propagating — no try/catch needed.
        return Recognize.setup(setupConfig)
            .fold(
                onSuccess = { Recognize.authenticate(biomAuthConfig) },
                onFailure = { Result.failure(it) }
            )
            .onSuccess { success ->
                submitResult(
                    signedJwt = success.signedJwt ?: "",
                    clientState = success.clientState ?: "",
                    devicePublicSigningKey = "",
                    clientError = "",
                    clientErrorCode = "",
                )
            }
            .onFailure { error ->
                submitResult(
                    signedJwt = "",
                    clientState = "",
                    devicePublicSigningKey = "",
                    clientError = error.message ?: "UNKNOWN_ERROR",
                    clientErrorCode = "",
                )
            }
    }

    private fun submitResult(
        signedJwt: String,
        clientState: String,
        devicePublicSigningKey: String,
        clientError: String,
        clientErrorCode: String,
    ) {
        if (derivedCallback) {
            setValueCallback(SIGNED_JWT_SUFFIX, signedJwt)
            setValueCallback(CLIENT_STATE_SUFFIX, clientState)
            // recognizeId is not returned for authentication
            setValueCallback(DEVICE_PUBLIC_SIGNING_KEY_SUFFIX, devicePublicSigningKey)
            setValueCallback(CLIENT_ERROR_SUFFIX, clientError)
            setValueCallback(CLIENT_ERROR_CODE_SUFFIX, clientErrorCode)
        } else {
            // Auth input order: signedJwt, clientState, recognizeId (empty), devicePublicSigningKey,
            // clientError, clientErrorCode
            input(signedJwt, clientState, "", devicePublicSigningKey, clientError, clientErrorCode)
        }
    }
}
