/*
 * Copyright (c) 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.pingidentity.recognize.journey

import com.pingidentity.recognize.Recognize
import com.pingidentity.recognize.RecognizeException
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecognizeDataCollectionCallbackTest {

    private lateinit var jsonPauseTrue: JsonObject
    private lateinit var jsonPauseFalse: JsonObject

    @BeforeTest
    fun setUp() {
        mockkObject(Recognize)
        every { Recognize.pauseDataCollection() } just runs
        every { Recognize.resumeDataCollection() } just runs
        coEvery { Recognize.data() } returns "the-signal"

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
    fun collectSucceedsAndPausesWhenFlagIsTrue() = runTest {
        val callback = RecognizeDataCollectionCallback().apply { init(jsonPauseTrue) }
        assertTrue(callback.pauseDataCollection)
        assertTrue(callback.collect().isSuccess)
        verify(exactly = 1) { Recognize.pauseDataCollection() }
    }

    @Test
    fun collectSucceedsWithoutPauseWhenFlagIsFalse() = runTest {
        val callback = RecognizeDataCollectionCallback().apply { init(jsonPauseFalse) }
        assertFalse(callback.pauseDataCollection)
        assertTrue(callback.collect().isSuccess)
        verify(exactly = 0) { Recognize.pauseDataCollection() }
    }

    @Test
    fun collectReturnsFailureOnSdkError() = runTest {
        coEvery { Recognize.data() } throws RecognizeException("data error")
        val callback = RecognizeDataCollectionCallback().apply { init(jsonPauseFalse) }
        val result = callback.collect()
        assertTrue(result.isFailure)
    }

    @Test
    fun defaultPauseDataCollectionIsFalse() {
        val callback = RecognizeDataCollectionCallback().apply {
            init(Json.parseToJsonElement(
                """{"type":"RecognizeDataCollectionCallback","output":[],"input":[]}"""
            ) as JsonObject)
        }
        assertFalse(callback.pauseDataCollection)
    }
}
