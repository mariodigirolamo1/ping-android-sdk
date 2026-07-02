/*
 * Copyright (c) 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.pingidentity.recognize.journey

import com.pingidentity.recognize.RecognizeException
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.coroutineContext

/**
 * Journey callback for the Recognize data-collection step.
 *
 * TODO: This file will be deleted in Task 3 and replaced by [RecognizeActionCallback].
 *       See plan.md Task 3 for details.
 */
class RecognizeDataCollectionCallback : AbstractRecognizeCallback() {

    /**
     * TODO: Field retained as stub until Task 3 deletes this file.
     */
    var pauseDataCollection: Boolean = false
        private set

    override fun init(name: String, value: JsonElement) {
        when (name) {
            "pauseDataCollection" -> pauseDataCollection =
                value.jsonPrimitive.contentOrNull?.toBooleanStrictOrNull() ?: false
            else -> {}
        }
    }

    /**
     * Runs the Recognize SDK data collection and submits the signal to the server.
     *
     * TODO: Task 3 will delete this file; the action-driven dispatch will live in
     *       [RecognizeActionCallback].
     *
     * @return [Result.success] wrapping the signal string, or [Result.failure] on error.
     */
    suspend fun collect(): Result<String> {
        return try {
            // TODO: Task 3 will delete this file and replace with action-driven dispatch.
            throw RecognizeException("RecognizeDataCollectionCallback.collect() is not yet implemented — see Task 3.")
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            signal("", e.message ?: CLIENT_ERROR)
            Result.failure(e)
        }
    }
}
