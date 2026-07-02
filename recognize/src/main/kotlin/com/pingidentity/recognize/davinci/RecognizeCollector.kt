/*
 * Copyright (c) 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.pingidentity.recognize.davinci

import com.pingidentity.davinci.plugin.Collector
import com.pingidentity.recognize.RecognizeException
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.coroutineContext

/**
 * DaVinci [Collector] that drives the Recognize SDK data-collection step.
 *
 * TODO: This file will be fully replaced in Task 2 with an action-driven dispatch model.
 *       See plan.md Task 2 for details.
 */
class RecognizeCollector : Collector<String> {

    /** The form-field key used to submit the signal value back to the server. */
    var key: String = ""
        private set

    private var value: String = ""

    override fun init(input: JsonObject): Collector<String> {
        key = input["key"]?.jsonPrimitive?.content ?: ""
        // TODO: parse additional Recognize-specific parameters from `input`
        return this
    }

    override fun id(): String = key

    override fun payload(): String? = value.ifEmpty { null }

    /**
     * Runs the Recognize SDK data collection and stores the result.
     *
     * TODO: This will be replaced by action-driven dispatch in Task 2.
     *
     * @return [Result.success] wrapping the signal string, or [Result.failure] on error.
     */
    suspend fun collect(): Result<String> {
        return try {
            // TODO: Task 2 will replace this with action-driven dispatch.
            throw RecognizeException("RecognizeCollector.collect() is not yet implemented — see Task 2.")
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            Result.failure(e)
        }
    }
}
