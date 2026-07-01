/*
 * Copyright (c) 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.pingidentity.recognize.davinci

import com.pingidentity.davinci.plugin.Collector
import com.pingidentity.recognize.Recognize
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.coroutineContext

/**
 * DaVinci [Collector] that drives the Recognize SDK data-collection step.
 *
 * The server sends a RECOGNIZE component node with a JSON payload; this collector
 * reads the parameters, triggers [Recognize.data], and returns the signal string
 * back to the DaVinci flow as the collector's payload.
 *
 * TODO: extend [init] to parse all parameters that the Recognize server node sends
 *       (analogous to how [ProtectCollector] reads `behavioralDataCollection`, `key`, etc.).
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
     * @return [Result.success] wrapping the signal string, or [Result.failure] on error.
     */
    suspend fun collect(): Result<String> {
        return try {
            // TODO: configure Recognize with any per-step parameters before calling data()
            Recognize.initialize()
            value = Recognize.data()
            Result.success(value)
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            Result.failure(e)
        }
    }
}
