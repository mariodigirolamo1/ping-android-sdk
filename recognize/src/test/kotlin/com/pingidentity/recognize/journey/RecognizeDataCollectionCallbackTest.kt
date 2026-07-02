/*
 * Copyright (c) 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.pingidentity.recognize.journey

import com.pingidentity.recognize.Recognize
import com.pingidentity.recognize.RecognizeException
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * TODO: This test file will be deleted in Task 3 when [RecognizeDataCollectionCallback] is removed.
 *       Tests below cover only the stub until then.
 */
class RecognizeDataCollectionCallbackTest {

    private lateinit var jsonPauseTrue: JsonObject
    private lateinit var jsonPauseFalse: JsonObject

    @BeforeTest
    fun setUp() {
        mockkObject(Recognize)

        jsonPauseTrue = Json.parseToJsonElement(
            """
            {
              "type": "RecognizeDataCollectionCallback",
              "output": [{ "name": "pauseDataCollection", "value": true }],
              "input": [
                { "name": "IDToken1recognize_signals", "value": "" },
                { "name": "IDToken1clientError",       "value": "" }
              ]
            }
            """
        ) as JsonObject

        jsonPauseFalse = Json.parseToJsonElement(
            """
            {
              "type": "RecognizeDataCollectionCallback",
              "output": [{ "name": "pauseDataCollection", "value": false }],
              "input": [
                { "name": "IDToken1recognize_signals", "value": "" },
                { "name": "IDToken1clientError",       "value": "" }
              ]
            }
            """
        ) as JsonObject
    }

    @AfterTest
    fun tearDown() {
        unmockkObject(Recognize)
    }

    @Test
    fun parsesPauseDataCollectionTrueFromJson() {
        val callback = RecognizeDataCollectionCallback().apply { init(jsonPauseTrue) }
        assertTrue(callback.pauseDataCollection)
    }

    @Test
    fun parsesPauseDataCollectionFalseFromJson() {
        val callback = RecognizeDataCollectionCallback().apply { init(jsonPauseFalse) }
        assertFalse(callback.pauseDataCollection)
    }

    @Test
    fun collectReturnsFailureWithStubException() = runTest {
        // The stub callback always returns failure until Task 3 wires in real action-driven dispatch.
        val callback = RecognizeDataCollectionCallback().apply { init(jsonPauseFalse) }
        val result = callback.collect()
        assertTrue(result.isFailure)
        assertIs<RecognizeException>(result.exceptionOrNull())
    }

    @Test
    fun defaultPauseDataCollectionIsFalse() {
        val callback = RecognizeDataCollectionCallback().apply {
            init(
                Json.parseToJsonElement(
                    """{"type":"RecognizeDataCollectionCallback","output":[],"input":[]}"""
                ) as JsonObject
            )
        }
        assertFalse(callback.pauseDataCollection)
    }
}
