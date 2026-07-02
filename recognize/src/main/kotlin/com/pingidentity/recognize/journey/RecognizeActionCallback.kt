/*
 * Copyright (c) 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.pingidentity.recognize.journey

import com.pingidentity.recognize.BiomAuthConfigDTO
import com.pingidentity.recognize.BiomDeenrollConfigDTO
import com.pingidentity.recognize.BiomEnrollConfigDTO
import com.pingidentity.recognize.Recognize
import com.pingidentity.recognize.RecognizeException
import com.pingidentity.recognize.SetupConfigDTO
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.coroutineContext

/**
 * Journey callback for Recognize SDK action-driven operations.
 *
 * Handles both delivery modes:
 * - **Typed-callback mode**: the server sends a first-class callback JSON with an `output` array;
 *   the action discriminator is read from the output fields.
 *   TODO: Confirm the exact output field name that carries the action value.
 * - **MetadataCallback mode**: the server wraps the payload in a `MetadataCallback`; the base
 *   class extracts the `data` object and dispatches field-by-field via [init]; the `_action`
 *   key in that data object is the discriminator (value e.g. `"recognize_initialize"`).
 *
 * On success, the JsonObject result is serialized to a JSON string and submitted via
 * [signal]. On failure, [error] is called with the exception message (or [CLIENT_ERROR]) and
 * [Result.failure] is returned.
 *
 * The action discriminator string values and per-action DTO fields are TODO-marked placeholders;
 * they must be confirmed with the server team before the real SDK is wired in.
 */
class RecognizeActionCallback : AbstractRecognizeCallback() {

    /**
     * The action discriminator decoded from the server JSON.
     *
     * In typed-callback mode, this is populated from the output field named `"action"`.
     * In MetadataCallback mode, this is populated from the `"_action"` key in the data object.
     *
     * TODO: Confirm the exact field name(s) and all four action string values with server team.
     *       Current placeholders: `"setup"`, `"biom_enroll"`, `"biom_auth"`, `"biom_deenroll"`.
     *       MetadataCallback placeholder: `"_action"` key (observed in test fixture as
     *       `"recognize_initialize"`).
     */
    var action: String = ""
        private set

    /**
     * Accumulates all server JSON fields received via [init].
     *
     * Per-action DTO fields are stored here with TODO comments; once the server JSON contract
     * is confirmed, the relevant entries will be passed as constructor parameters to the
     * appropriate DTO.
     *
     * TODO: Replace the raw field map with typed lateinit/nullable vars per action once the
     *       server JSON field names are confirmed.
     */
    private val fields: MutableMap<String, JsonElement> = mutableMapOf()

    /**
     * Receives one name/value pair from the server JSON output array (typed-callback mode)
     * or from the MetadataCallback data object.
     *
     * The `"action"` key (typed-callback mode) and `"_action"` key (MetadataCallback mode)
     * both populate [action]. All other fields are stored in [fields] for DTO construction.
     *
     * TODO: Confirm the exact output field name for the action discriminator in typed-callback
     *       mode. Using `"action"` as a placeholder.
     */
    override fun init(name: String, value: JsonElement) {
        fields[name] = value
        when (name) {
            // Typed-callback mode discriminator
            // TODO: confirm exact field name with server team — using "action" as placeholder
            "action" -> action = value.jsonPrimitive.contentOrNull ?: ""
            // MetadataCallback mode discriminator (observed: "_action": "recognize_initialize")
            "_action" -> action = value.jsonPrimitive.contentOrNull ?: ""
            // All other fields stored in `fields` for DTO construction
            else -> {}
        }
    }

    /**
     * Dispatches to the appropriate Recognize SDK operation based on the decoded [action].
     *
     * On success, calls [signal] with the serialized [JsonObject] result.
     * On failure, calls [coroutineContext.ensureActive] then [error] with the exception message.
     * An unknown action value calls [error] and returns [Result.failure].
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
                else -> {
                    val ex = RecognizeException("Unknown action: $action")
                    error(ex.message ?: CLIENT_ERROR)
                    return Result.failure(ex)
                }
            }
            signal(sdkResult.toString(), "")
            Result.success(sdkResult)
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            error(e.message ?: CLIENT_ERROR)
            Result.failure(e)
        }
    }
}
