/*
 * Copyright (c) 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.pingidentity.recognize

import com.pingidentity.utils.PingDsl
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The Recognize object is the central entry point for the Recognize SDK (formerly Keyless).
 * It manages initialization state and exposes the core operations that collectors and
 * callbacks delegate to: [initialize], [data], and pause/resume controls.
 *
 * TODO: Replace stub implementations with real Recognize SDK calls once the artifact is wired in.
 */
object Recognize {

    private lateinit var recognizeConfig: RecognizeConfig
    private var isInitialized: Boolean = false
    private val lock = Mutex()

    /**
     * Configures the Recognize SDK with the provided configuration.
     * Must be called before [initialize].
     */
    fun config(config: RecognizeConfig.() -> Unit) {
        recognizeConfig = RecognizeConfig().apply(config)
    }

    /**
     * Initializes the Recognize SDK. Idempotent — subsequent calls are no-ops if already
     * initialized.
     *
     * @throws RecognizeException if initialization fails.
     */
    suspend fun initialize(): Unit = lock.withLock {
        if (isInitialized) return

        // TODO: invoke the real Recognize SDK initialization here, e.g.:
        //   RecognizeSDK.init(ContextProvider.context, params) { result ->
        //       if (result.isSuccess) init.resume(Unit) else init.resumeWithException(...)
        //   }
        //
        // For now this is a no-op placeholder so the module compiles and tests can be
        // written against the contract.
        isInitialized = true
    }

    /**
     * Resets the initialization state. Useful for testing or when reconfiguration is needed.
     */
    fun reset() {
        isInitialized = false
    }

    /**
     * Collects the device signal / recognition data from the Recognize SDK.
     *
     * @return A string payload (e.g. a JWS or JSON blob) to be submitted to the server.
     * @throws RecognizeException if data collection fails.
     */
    suspend fun data(): String {
        // TODO: replace with real Recognize SDK data retrieval, e.g.:
        //   return suspendCancellableCoroutine { cont ->
        //       RecognizeSDK.getData(object : DataCallback {
        //           override fun onSuccess(result: String) { cont.resume(result) }
        //           override fun onFailure(error: String) { cont.resumeWithException(RecognizeException(error)) }
        //       })
        //   }
        throw RecognizeException("Recognize.data() is not yet implemented — wire in the SDK.")
    }

    /**
     * Pauses data collection in the Recognize SDK.
     * No-op placeholder until the SDK is wired in.
     */
    fun pauseDataCollection() {
        // TODO: RecognizeSDK.pauseDataCollection()
    }

    /**
     * Resumes data collection in the Recognize SDK.
     * No-op placeholder until the SDK is wired in.
     */
    fun resumeDataCollection() {
        // TODO: RecognizeSDK.resumeDataCollection()
    }
}

/**
 * Configuration for the Recognize SDK.
 *
 * Extend this class with the parameters required by the Recognize SDK once the integration
 * contract is known. The fields below are illustrative placeholders.
 */
@PingDsl
open class RecognizeConfig {
    /**
     * TODO: Replace / extend with real Recognize SDK configuration parameters.
     * Example: environment identifier, tenant URL, feature flags, etc.
     */
    var envId: String? = null

    /** Whether to enable verbose console logging from the Recognize SDK. */
    var isConsoleLogEnabled: Boolean = false
}
