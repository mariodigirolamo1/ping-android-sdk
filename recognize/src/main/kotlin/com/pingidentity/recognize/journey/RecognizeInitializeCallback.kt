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
 * Journey callback for the Recognize initialization step.
 *
 * TODO: This file will be deleted in Task 3 and replaced by [RecognizeActionCallback].
 *       See plan.md Task 3 for details.
 */
class RecognizeInitializeCallback : AbstractRecognizeCallback() {

    /**
     * TODO: Fields retained as stubs until Task 3 deletes this file.
     */
    var envId: String = ""
        private set

    var isConsoleLogEnabled: Boolean = false
        private set

    override fun init(name: String, value: JsonElement) {
        when (name) {
            "envId" -> envId = value.jsonPrimitive.contentOrNull ?: ""
            "consoleLogEnabled" -> isConsoleLogEnabled =
                value.jsonPrimitive.contentOrNull?.toBooleanStrictOrNull() ?: false
            else -> {}
        }
    }

    /**
     * Configures and initializes the Recognize SDK using parameters received from the server.
     *
     * TODO: Task 3 will delete this file; the action-driven dispatch will live in
     *       [RecognizeActionCallback].
     *
     * @return [Result.success] on successful initialization, [Result.failure] otherwise.
     */
    suspend fun start(): Result<Unit> {
        return try {
            Recognize.config {
                // TODO: forward fields once RecognizeConfig carries them
            }
            Result.success(Unit)
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            error(e.message ?: CLIENT_ERROR)
            Result.failure(e)
        }
    }
}
