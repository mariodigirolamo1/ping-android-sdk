/*
 * Copyright (c) 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.pingidentity.recognize.journey

import com.pingidentity.recognize.Recognize
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.coroutineContext

/**
 * Journey callback for the Recognize data-collection step.
 *
 * Triggers [Recognize.data] and submits the resulting signal payload back to PingAM.
 * Mirrors [PingOneProtectEvaluationCallback] in structure.
 *
 * TODO: extend [init] with any additional fields the Recognize evaluation node sends.
 */
class RecognizeDataCollectionCallback : AbstractRecognizeCallback() {

    /**
     * TODO: add Recognize-specific evaluation parameters here.
     * Example (mirroring Protect's pauseBehavioralData flag):
     */
    var pauseDataCollection: Boolean = false
        private set

    override fun init(name: String, value: JsonElement) {
        when (name) {
            "pauseDataCollection" -> pauseDataCollection = value.jsonPrimitive.contentOrNull?.toBooleanStrictOrNull() ?: false
            // TODO: map additional server-provided fields
            else -> {}
        }
    }

    /**
     * Runs the Recognize SDK data collection and submits the signal to the server.
     *
     * @return [Result.success] wrapping the signal string, or [Result.failure] on error.
     *         On failure the client error is automatically submitted via [signal].
     */
    suspend fun collect(): Result<String> {
        return try {
            val signalData = Recognize.data()
            if (pauseDataCollection) {
                Recognize.pauseDataCollection()
            }
            signal(signalData, "")
            Result.success(signalData)
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            signal("", e.message ?: CLIENT_ERROR)
            Result.failure(e)
        }
    }
}
