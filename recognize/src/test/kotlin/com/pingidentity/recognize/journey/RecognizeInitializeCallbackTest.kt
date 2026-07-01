/*
 * Copyright (c) 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.pingidentity.recognize.journey

import com.pingidentity.recognize.Recognize
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecognizeInitializeCallbackTest {

    private lateinit var typedJson: JsonObject
    private lateinit var metadataJson: JsonObject

    @BeforeTest
    fun setUp() {
        mockkObject(Recognize)
        every { Recognize.config(any()) } just runs
        coEvery { Recognize.initialize() } just runs

        typedJson = Json.parseToJsonElement(
            """
            {
              "type": "RecognizeInitializeCallback",
              "output": [
                { "name": "envId",             "value": "test-env-id" },
                { "name": "consoleLogEnabled",  "value": true }
              ],
              "input": [
                { "name": "IDToken1clientError", "value": "" }
              ]
            }
            """
        ) as JsonObject

        metadataJson = Json.parseToJsonElement(
            """
            {
              "type": "MetadataCallback",
              "output": [
                {
                  "name": "data",
                  "value": {
                    "_type": "Recognize",
                    "_action": "recognize_initialize",
                    "envId": "meta-env-id",
                    "consoleLogEnabled": false
                  }
                }
              ],
              "_id": 0
            }
            """
        ) as JsonObject
    }

    @AfterTest
    fun tearDown() {
        unmockkObject(Recognize)
    }

    @Test
    fun parsesTypedCallbackFields() = runTest {
        val callback = RecognizeInitializeCallback().apply { init(typedJson) }
        assertEquals("test-env-id", callback.envId)
        assertTrue(callback.isConsoleLogEnabled)
        assertTrue(callback.start().isSuccess)
    }

    @Test
    fun parsesMetadataCallbackFields() {
        val callback = RecognizeInitializeCallback()
        val result = callback.init(metadataJson)
        assertTrue(result is RecognizeInitializeCallback)
        assertEquals("meta-env-id", result.envId)
    }

    @Test
    fun startReturnsFailureAndSetsClientErrorOnException() = runTest {
        coEvery { Recognize.initialize() } throws RuntimeException("init failed")
        val callback = RecognizeInitializeCallback().apply { init(typedJson) }
        val result = callback.start()
        assertTrue(result.isFailure)
        assertEquals("init failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun defaultsAreUsedForMissingFields() {
        val minimal = Json.parseToJsonElement(
            """
            {
              "type": "RecognizeInitializeCallback",
              "output": [],
              "input": [{ "name": "IDToken1clientError", "value": "" }]
            }
            """
        ) as JsonObject
        val callback = RecognizeInitializeCallback().apply { init(minimal) }
        assertEquals("", callback.envId)
        assertEquals(false, callback.isConsoleLogEnabled)
    }
}
