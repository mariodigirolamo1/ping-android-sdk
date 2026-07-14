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
import io.keyless.sdk.configurations.SetupConfig
import io.keyless.sdk.configurations.enroll.BiomEnrollConfig
import io.keyless.sdk.configurations.enroll.PresentationStyle
import io.keyless.sdk.core.actions.model.JwtSigningInfo
import io.keyless.sdk.errorshandling.EnrollmentSuccess
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Journey callback that handles PingOne Recognize **enrollment** operations.
 *
 * This callback is returned by [RecognizeCallback] when the server output contains
 * `operationType = "ENROLL"`. All output fields are parsed by [AbstractRecognizeCallback];
 * this class only adds the [enroll] operation.
 *
 * On success, the signed JWT, client state, and recognize ID are submitted to the Journey
 * via the five input fields: `IDToken1signedJwt`, `IDToken1clientState`,
 * `IDToken1recognizeId`, `IDToken1clientError`, `IDToken1clientErrorCode`.
 *
 * @see RecognizeCallback
 * @see PingOneRecognizeAuthenticateCallback
 */
class PingOneRecognizeEnrollCallback : AbstractRecognizeCallback() {

    /**
     * Performs the PingOne Recognize enrollment ceremony.
     *
     * | Callback field / mobileSDKOptions key           | EnrollConfig property              |
     * |-------------------------------------------------|------------------------------------|
     * | `transactionData`                               | `jwtSigningInfo.claimTransactionData` |
     * | `clientState`                                   | `clientState`                      |
     * | `generateClientState` (`"true"` → BACKUP)       | `generatingClientState`            |
     * | `mobileSDKOptions.operationInfoId`              | `operationInfo.operationId`        |
     * | `mobileSDKOptions.operationInfoPayload`         | `operationInfo.payload`            |
     * | `mobileSDKOptions.operationInfoExternalUserId`  | `operationInfo.externalUserId`     |
     * | `mobileSDKOptions.livenessConfiguration`        | `livenessConfiguration`            |
     * | `mobileSDKOptions.livenessEnvironmentAware`     | `livenessEnvironmentAware`         |
     * | `mobileSDKOptions.cameraDelaySeconds`           | `cameraDelaySeconds`               |
     * | `mobileSDKOptions.shouldRetrieveEnrollmentFrame`| `shouldRetrieveEnrollmentFrame`    |
     * | `mobileSDKOptions.showSuccessFeedback`          | `showSuccessFeedback`              |
     * | `mobileSDKOptions.showFailureFeedback`          | `showFailureFeedback`              |
     * | `mobileSDKOptions.showInstructionsScreen`       | `showInstructionsScreen`           |
     * | `mobileSDKOptions.presentation`                 | `presentationStyle`                |
     * | `mobileSDKOptions.numberOfEnrollmentCircuits`   | `setupConfig.numberOfEnrollmentCircuits` |
     *
     * @return [Result] containing [EnrollmentSuccess] on success, or a [Throwable] on failure.
     */
    suspend fun enroll(): Result<EnrollmentSuccess> {
        val opts = mobileSDKOptions
        val setupConfig = SetupConfig(
            apiKey = this@PingOneRecognizeEnrollCallback.apiKey,
            hosts = listOf(this@PingOneRecognizeEnrollCallback.host),
            numberOfEnrollmentCircuits = opts["numberOfEnrollmentCircuits"]?.jsonPrimitive?.contentOrNull
                ?.toIntOrNull() ?: SetupConfig.DEFAULT_ENROLLMENT_CIRCUIT_NUMBER,
        )
        val storedTransactionData = transactionData
        val storedClientState = clientState
        val storedGenerateClientState = generateClientState

        val opId = opts["operationInfoId"]?.jsonPrimitive?.contentOrNull
        val opPayload = opts["operationInfoPayload"]?.jsonPrimitive?.contentOrNull
        val opExternalUserId = opts["operationInfoExternalUserId"]?.jsonPrimitive?.contentOrNull
        val operationInfo = if (opId != null || opPayload != null || opExternalUserId != null) {
            OperationInfo(
                operationId = opId ?: "",
                payload = opPayload ?: "",
                externalUserId = opExternalUserId ?: ""
            )
        } else null

        // "true" → ClientStateType.BACKUP, "false"/empty → null
        val storedGeneratingClientState = if (storedGenerateClientState.equals("true", ignoreCase = true)) {
            ClientStateType.BACKUP
        } else null

        val storedAudience = audience
        val jwtSigningInfo = if (storedAudience.isNotBlank()) {
            JwtSigningInfo(claimTransactionData = storedTransactionData, audience = storedAudience)
        } else {
            JwtSigningInfo(claimTransactionData = storedTransactionData)
        }

        var biomEnrollConfig = BiomEnrollConfig(
            operationInfo = operationInfo,
            jwtSigningInfo = jwtSigningInfo,
            generatingClientState = storedGeneratingClientState,
            clientState = storedClientState.takeIf { it.isNotEmpty() },
        )
        opts["livenessConfiguration"]?.jsonPrimitive?.contentOrNull
            ?.let { runCatching { LivenessSettings.LivenessConfiguration.valueOf(it) }.getOrNull() }
            ?.let { biomEnrollConfig = biomEnrollConfig.copy(livenessConfiguration = it) }
        opts["livenessEnvironmentAware"]?.jsonPrimitive?.contentOrNull?.toBoolean()
            ?.let { biomEnrollConfig = biomEnrollConfig.copy(livenessEnvironmentAware = it) }
        opts["cameraDelaySeconds"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
            ?.let { biomEnrollConfig = biomEnrollConfig.copy(cameraDelaySeconds = it) }
        opts["shouldRetrieveEnrollmentFrame"]?.jsonPrimitive?.contentOrNull?.toBoolean()
            ?.let { biomEnrollConfig = biomEnrollConfig.copy(shouldRetrieveEnrollmentFrame = it) }
        opts["showSuccessFeedback"]?.jsonPrimitive?.contentOrNull?.toBoolean()
            ?.let { biomEnrollConfig = biomEnrollConfig.copy(showSuccessFeedback = it) }
        opts["showFailureFeedback"]?.jsonPrimitive?.contentOrNull?.toBoolean()
            ?.let { biomEnrollConfig = biomEnrollConfig.copy(showFailureFeedback = it) }
        opts["showInstructionsScreen"]?.jsonPrimitive?.contentOrNull?.toBoolean()
            ?.let { biomEnrollConfig = biomEnrollConfig.copy(showInstructionsScreen = it) }
        opts["presentation"]?.jsonPrimitive?.contentOrNull
            ?.let { runCatching { PresentationStyle.valueOf(it) }.getOrNull() }
            ?.let { biomEnrollConfig = biomEnrollConfig.copy(presentationStyle = it) }
        // Chain setup → enroll so a setup failure is also reported back to the server
        // via input() before propagating — no try/catch needed.
        return Recognize.setup(setupConfig)
            .fold(
                onSuccess = { Recognize.enroll(biomEnrollConfig) },
                onFailure = { Result.failure(it) }
            )
            .onSuccess { success ->
                submitResult(
                    signedJwt = success.signedJwt ?: "",
                    clientState = success.clientState ?: "",
                    recognizeId = success.keylessId,
                    clientError = "",
                    clientErrorCode = "",
                )
            }
            .onFailure { error ->
                submitResult(
                    signedJwt = "",
                    clientState = "",
                    recognizeId = "",
                    clientError = error.message ?: "UNKNOWN_ERROR",
                    clientErrorCode = "",
                )
            }
    }

    private fun submitResult(
        signedJwt: String,
        clientState: String,
        recognizeId: String,
        clientError: String,
        clientErrorCode: String,
    ) {
        if (derivedCallback) {
            setValueCallback(SIGNED_JWT_SUFFIX, signedJwt)
            setValueCallback(CLIENT_STATE_SUFFIX, clientState)
            setValueCallback(RECOGNIZE_ID_SUFFIX, recognizeId)
            setValueCallback(CLIENT_ERROR_SUFFIX, clientError)
            setValueCallback(CLIENT_ERROR_CODE_SUFFIX, clientErrorCode)
        } else {
            // Input order matches server fields: signedJwt, clientState, recognizeId,
            // devicePublicSigningKey (always empty for enroll), clientError
            input(signedJwt, clientState, recognizeId, "", clientError)
        }
    }
}
