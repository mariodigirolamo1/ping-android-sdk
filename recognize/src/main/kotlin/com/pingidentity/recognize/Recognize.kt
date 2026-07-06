/*
 * Copyright (c) 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.pingidentity.recognize

import io.keyless.sdk.Keyless
import io.keyless.sdk.configurations.SetupConfig
import io.keyless.sdk.configurations.auth.BiomAuthConfig
import io.keyless.sdk.configurations.enroll.BiomEnrollConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.coroutines.resumeWithException

object Recognize {
    internal suspend fun setup(
        setupConfigDTO: SetupConfigDTO
    ) = suspendCancellableCoroutine { continuation ->
        Keyless.configure(
            // TODO: allow other params config if needed, this is just for quick tests 
            SetupConfig(
                apiKey = setupConfigDTO.apiKey,
                hosts = setupConfigDTO.hosts
            )
        ) { result ->
            when (result) {
                is Keyless.KeylessResult.Success -> {
                    continuation.resume(
                        value = Result.success(result.value)
                    ) { cause, _, _ ->
                        // TODO: might handle this cause in case of cancellation
                    }
                }

                is Keyless.KeylessResult.Failure -> {
                    continuation.resume(
                        value = Result.failure(result.error)
                    ) { cause, _, _ ->
                        // TODO: might handle this cause in case of cancellation
                    }
                }
            }
        }
    }

    suspend fun enroll(
        config: BiomEnrollConfigDTO
    ): JsonObject = suspendCancellableCoroutine { cont ->
        // TODO: should use incoming DTO config 
        Keyless.enroll(
            configuration = BiomEnrollConfig()
        ) { result ->
            when(result) {
                is Keyless.KeylessResult.Success -> {
                    val recognizeEnrollmentResult = with(result.value) {
                        RecognizeEnrollmentResult(
                            keylessId = keylessId,
                            signedJwt = signedJwt,
                            clientState = clientState,
                            secret = secret?.value?.rawValue,
                            secretIDs = secretIDs.map {
                                it.rawValue
                            }.toSet()
                        )
                    }

                    val jsonObjectResult = Json.encodeToJsonElement(
                        recognizeEnrollmentResult
                    ).jsonObject

                    cont.resume(jsonObjectResult) { cause, _, _ ->
                        // TODO: handle cancellation
                    }
                }
                is Keyless.KeylessResult.Failure -> {
                    // TODO: does it need wrapper eception? 
                    cont.resumeWithException(result.error)
                }
            }
        }
    }

    suspend fun authenticate(
        config: BiomAuthConfigDTO
    ): JsonObject = suspendCancellableCoroutine { cont ->
        Keyless.authenticate(configuration = BiomAuthConfig()) { result ->
            when(result) {
                is Keyless.KeylessResult.Success -> {
                    val recognizeAuthenticationResult = with(result.value) {
                        RecognizeAuthenticationResult(
                            signedJwt = signedJwt,
                            clientState = clientState,
                            secret = secret?.value?.rawValue,
                            secretIDs = secretIDs.map {
                                it.rawValue
                            }.toSet()
                        )
                    }

                    val jsonObjectResult = Json.encodeToJsonElement(
                        recognizeAuthenticationResult
                    ).jsonObject

                    cont.resume(value = jsonObjectResult) { cause, _, _ ->
                        // TODO: handle cancellation
                    }
                }

                is Keyless.KeylessResult.Failure -> {
                    // TODO: does it need wrapper eception?
                    cont.resumeWithException(result.error)
                }
            }

        }
    }
}

// TODO: missing enrollment frame
@Serializable
data class RecognizeEnrollmentResult(
    val keylessId: String?,
    val signedJwt: String?,
    val clientState: String?,
    val secret: String?,
    val secretIDs: Set<String>
)

// TODO: missing authentication frame
@Serializable
data class RecognizeAuthenticationResult(
    val signedJwt: String?,
    val clientState: String?,
    val secret: String?,
    val secretIDs: Set<String>
)
