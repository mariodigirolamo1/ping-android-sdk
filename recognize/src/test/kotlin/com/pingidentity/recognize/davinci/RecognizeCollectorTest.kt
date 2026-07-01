/*
 * Copyright (c) 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.pingidentity.recognize.davinci

import com.pingidentity.recognize.Recognize
import com.pingidentity.recognize.RecognizeException
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecognizeCollectorTest {

    @BeforeTest
    fun setUp() {
        mockkObject(Recognize)
    }

    @AfterTest
    fun tearDown() {
        unmockkObject(Recognize)
    }

    @Test
    fun initParsesKeyFromJson() {
        val collector = RecognizeCollector()
        collector.init(buildJsonObject { put("key", "recognizeKey") })
        assertEquals("recognizeKey", collector.key)
    }

    @Test
    fun initUsesEmptyKeyWhenFieldMissing() {
        val collector = RecognizeCollector()
        collector.init(buildJsonObject { })
        assertEquals("", collector.key)
    }

    @Test
    fun idReturnsKey() {
        val collector = RecognizeCollector()
        collector.init(buildJsonObject { put("key", "k1") })
        assertEquals("k1", collector.id())
    }

    @Test
    fun payloadReturnsNullBeforeCollect() {
        assertNull(RecognizeCollector().payload())
    }

    @Test
    fun collectReturnsSuccessWithSignalData() = runTest {
        coEvery { Recognize.initialize() } returns Unit
        coEvery { Recognize.data() } returns "signal-payload"

        val collector = RecognizeCollector()
        val result = collector.collect()

        assertTrue(result.isSuccess)
        assertEquals("signal-payload", result.getOrNull())
        assertEquals("signal-payload", collector.payload())
    }

    @Test
    fun collectReturnsFailureOnException() = runTest {
        coEvery { Recognize.initialize() } returns Unit
        coEvery { Recognize.data() } throws RecognizeException("sdk error")

        val result = RecognizeCollector().collect()

        assertTrue(result.isFailure)
        assertIs<RecognizeException>(result.exceptionOrNull())
    }
}
