/*
 * Copyright (c) 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.pingidentity.recognize.davinci

import com.pingidentity.davinci.plugin.Collector
import com.pingidentity.recognize.BiomAuthConfigDTO
import com.pingidentity.recognize.BiomDeenrollConfigDTO
import com.pingidentity.recognize.BiomEnrollConfigDTO
import com.pingidentity.recognize.Recognize
import com.pingidentity.recognize.RecognizeException
import com.pingidentity.recognize.SetupConfigDTO
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.coroutineContext

/**
 * DaVinci [Collector] that drives Recognize SDK action-driven operations.
 *
 * The server JSON payload contains an `action` discriminator field (TODO: confirm exact field
 * name with server team) that selects which of the four Recognize SDK operations to execute:
 * - `"setup"` → [Recognize.setup]
 * - `"biom_enroll"` → [Recognize.enroll]
 * - `"biom_auth"` → [Recognize.authenticate]
 * - `"biom_deenroll"` → [Recognize.deenroll]
 *
 * TODO: Confirm exact action discriminator field name and all four string values with server team.
 *       Current values ("setup", "biom_enroll", "biom_auth", "biom_deenroll") are placeholders.
 */
class RecognizeCollector : Collector<JsonObject> {

    /**
     * The form-field key used to submit the SDK result back to the server.
     */
    var key: String = ""
        private set

    /**
     * The action discriminator decoded from the server JSON.
     *
     * TODO: Confirm exact field name with server team — using `"action"` as placeholder.
     */
    var action: String = ""
        private set

    private var result: JsonObject? = null

    /**
     * Initializes the collector from the server JSON.
     *
     * Reads the `key` field for form submission and the `action` discriminator field for
     * SDK operation dispatch.
     *
     * TODO: Decode per-action DTO fields from [input] into stored vars once the server JSON
     *       contract is confirmed. Each action branch will construct its DTO from those fields.
     *
     * @param input The JSON object received from the server.
     */
    override fun init(input: JsonObject): Collector<JsonObject> {
        key = input["key"]?.jsonPrimitive?.content ?: ""
        // TODO: confirm exact discriminator field name with server team — using "action" as placeholder
        action = input["action"]?.jsonPrimitive?.content ?: ""
        // TODO: decode per-action DTO fields from `input` once server JSON contract is confirmed
        return this
    }

    override fun id(): String = key

    /**
     * Returns the [JsonObject] result from the most recent [collect] call, or `null` before
     * [collect] has been called.
     */
    override fun payload(): JsonObject? = result

    /**
     * Dispatches to the appropriate Recognize SDK operation based on the decoded [action].
     * Stores the returned [JsonObject] result for subsequent [payload] calls.
     *
     * An unrecognised [action] value returns [Result.failure] with a [RecognizeException]
     * rather than silently succeeding.
     *
     * @return [Result.success] wrapping the SDK result, or [Result.failure] on error or unknown action.
     */
    suspend fun collect(): Result<JsonObject> {
        return try {
            // TODO: confirm action discriminator values with server team
            val sdkResult = when (action) {
                "setup" ->
                    // TODO: construct SetupConfigDTO from fields decoded in init() once server contract confirmed
                    Recognize.setup(SetupConfigDTO())
                "biom_enroll" ->
                    // TODO: construct BiomEnrollConfigDTO from fields decoded in init() once server contract confirmed
                    Recognize.enroll(BiomEnrollConfigDTO())
                "biom_auth" ->
                    // TODO: construct BiomAuthConfigDTO from fields decoded in init() once server contract confirmed
                    Recognize.authenticate(BiomAuthConfigDTO())
                "biom_deenroll" ->
                    // TODO: construct BiomDeenrollConfigDTO from fields decoded in init() once server contract confirmed
                    Recognize.deenroll(BiomDeenrollConfigDTO())
                else -> return Result.failure(RecognizeException("Unknown action: $action"))
            }
            result = sdkResult
            Result.success(sdkResult)
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            Result.failure(e)
        }
    }
}
