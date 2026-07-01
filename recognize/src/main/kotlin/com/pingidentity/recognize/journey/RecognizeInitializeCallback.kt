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
 * Reads SDK configuration parameters sent by the server, configures [Recognize], and
 * calls [Recognize.initialize]. Mirrors [PingOneProtectInitializeCallback] in structure.
 *
 * TODO: extend [init] with the full set of parameters the Recognize server node sends.
 *       Add the corresponding typed properties (backed by `private set`) for each one.
 */
class RecognizeInitializeCallback : AbstractRecognizeCallback() {

    /**
     * TODO: add real Recognize SDK initialization parameters here.
     * Example fields mirroring Protect (replace / remove as the contract is defined):
     */
    var envId: String = ""
        private set

    var isConsoleLogEnabled: Boolean = false
        private set

    override fun init(name: String, value: JsonElement) {
        when (name) {
            "envId" -> envId = value.jsonPrimitive.contentOrNull ?: ""
            "consoleLogEnabled" -> isConsoleLogEnabled = value.jsonPrimitive.contentOrNull?.toBooleanStrictOrNull() ?: false
            // TODO: map additional server-provided fields
            else -> {}
        }
    }

    /**
     * Configures and initializes the Recognize SDK using parameters received from the server.
     *
     * @return [Result.success] on successful initialization, [Result.failure] otherwise.
     *         On failure the client error is automatically submitted via [error].
     */
    suspend fun start(): Result<Unit> {
        return try {
            Recognize.config {
                envId = this@RecognizeInitializeCallback.envId.nullIfEmpty()
                isConsoleLogEnabled = this@RecognizeInitializeCallback.isConsoleLogEnabled
            }
            Recognize.initialize()
            Result.success(Unit)
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            error(e.message ?: CLIENT_ERROR)
            Result.failure(e)
        }
    }
}

private fun String.nullIfEmpty(): String? = takeIf { it.isNotEmpty() }
