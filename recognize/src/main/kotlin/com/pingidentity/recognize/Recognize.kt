/*
 * Copyright (c) 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.pingidentity.recognize

import com.pingidentity.utils.PingDsl
import kotlinx.serialization.json.JsonObject

/**
 * The Recognize object is the central entry point for the Recognize SDK (formerly Keyless).
 * It exposes four action-driven suspend operations — [setup], [enroll], [authenticate],
 * and [deenroll] — corresponding to the four server-directed actions.
 *
 * Each operation accepts a typed config DTO decoded from the server JSON and returns a
 * [JsonObject] result to be submitted back to the server.
 *
 * TODO: Replace stub implementations with real Recognize SDK calls once the artifact is
 *       wired in (see `recognize/build.gradle.kts`).
 */
object Recognize {

    private lateinit var recognizeConfig: RecognizeConfig

    /**
     * Configures the Recognize SDK with the provided configuration block.
     *
     * @param config Lambda that mutates the [RecognizeConfig].
     */
    fun config(config: RecognizeConfig.() -> Unit) {
        recognizeConfig = RecognizeConfig().apply(config)
    }

    /**
     * Resets any SDK-level state. Useful for test isolation.
     *
     * TODO: Clear real SDK state once the SDK is wired in.
     */
    fun reset() {
        // no-op stub — real SDK state teardown to be added once SDK contract is confirmed
    }

    /**
     * Executes the Recognize SDK setup operation.
     *
     * @param config The setup parameters decoded from the server JSON.
     * @return A [JsonObject] result to submit back to the server.
     * @throws NotImplementedError until the real SDK is wired in.
     */
    suspend fun setup(config: SetupConfigDTO): JsonObject {
        // TODO: wire in real SDK call — setup
        //   suspendCancellableCoroutine { cont ->
        //       RecognizeSDK.setup(config) { result ->
        //           if (result.isSuccess) cont.resume(result.toJsonObject())
        //           else cont.resumeWithException(RecognizeException(result.errorMessage))
        //       }
        //   }
        TODO("TODO: wire in real SDK call — setup")
    }

    /**
     * Executes the Recognize SDK biometric enroll operation.
     *
     * @param config The enroll parameters decoded from the server JSON.
     * @return A [JsonObject] result to submit back to the server.
     * @throws NotImplementedError until the real SDK is wired in.
     */
    suspend fun enroll(config: BiomEnrollConfigDTO): JsonObject {
        // TODO: wire in real SDK call — enroll
        //   suspendCancellableCoroutine { cont ->
        //       RecognizeSDK.enroll(config) { result ->
        //           if (result.isSuccess) cont.resume(result.toJsonObject())
        //           else cont.resumeWithException(RecognizeException(result.errorMessage))
        //       }
        //   }
        TODO("TODO: wire in real SDK call — enroll")
    }

    /**
     * Executes the Recognize SDK biometric authentication operation.
     *
     * @param config The authentication parameters decoded from the server JSON.
     * @return A [JsonObject] result to submit back to the server.
     * @throws NotImplementedError until the real SDK is wired in.
     */
    suspend fun authenticate(config: BiomAuthConfigDTO): JsonObject {
        // TODO: wire in real SDK call — authenticate
        //   suspendCancellableCoroutine { cont ->
        //       RecognizeSDK.authenticate(config) { result ->
        //           if (result.isSuccess) cont.resume(result.toJsonObject())
        //           else cont.resumeWithException(RecognizeException(result.errorMessage))
        //       }
        //   }
        TODO("TODO: wire in real SDK call — authenticate")
    }

    /**
     * Executes the Recognize SDK biometric de-enroll operation.
     *
     * @param config The de-enroll parameters decoded from the server JSON.
     * @return A [JsonObject] result to submit back to the server.
     * @throws NotImplementedError until the real SDK is wired in.
     */
    suspend fun deenroll(config: BiomDeenrollConfigDTO): JsonObject {
        // TODO: wire in real SDK call — deenroll
        //   suspendCancellableCoroutine { cont ->
        //       RecognizeSDK.deenroll(config) { result ->
        //           if (result.isSuccess) cont.resume(result.toJsonObject())
        //           else cont.resumeWithException(RecognizeException(result.errorMessage))
        //       }
        //   }
        TODO("TODO: wire in real SDK call — deenroll")
    }
}

/**
 * Configuration for the Recognize SDK.
 *
 * TODO: Add the SDK-level parameters (endpoint, API key, tenant URL, credentials, feature flags,
 *       etc.) once the Recognize SDK integration contract is confirmed. These are global/persistent
 *       parameters that apply to all operations — per-action parameters come from the server JSON
 *       and are decoded into the typed DTOs ([SetupConfigDTO], [BiomEnrollConfigDTO], etc.).
 */
@PingDsl
open class RecognizeConfig
