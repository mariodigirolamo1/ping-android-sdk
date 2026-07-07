/*
 * Copyright (c) 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.pingidentity.recognize.davinci

import com.pingidentity.recognize.Recognize
import com.pingidentity.recognize.RecognizeException
import io.keyless.sdk.errorshandling.AuthenticationSuccess
import io.keyless.sdk.errorshandling.EnrollmentSuccess
import io.mockk.coEvery
import io.mockk.mockk
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecognizeCollectorTest {

    @BeforeTest
    fun setUp() {
        mockkObject(Recognize)
        coEvery { Recognize.setup(any()) } returns Result.success(Unit)
        coEvery { Recognize.enroll(any()) } returns Result.success(mockk<EnrollmentSuccess>())
        coEvery { Recognize.authenticate(any()) } returns Result.success(mockk<AuthenticationSuccess>())
    }

    @AfterTest
    fun tearDown() {
        unmockkObject(Recognize)
    }

    // ── init / id / payload ──────────────────────────────────────────────────

    @Test
    fun initParsesKeyAndActionFromJson() {
        val collector = RecognizeCollector()
        collector.init(buildJsonObject {
            put("key", "recognizeKey")
            put("action", "setup")
        })
        assertEquals("recognizeKey", collector.key)
        assertEquals("setup", collector.action)
    }

    @Test
    fun initUsesEmptyStringsWhenFieldsMissing() {
        val collector = RecognizeCollector()
        collector.init(buildJsonObject { })
        assertEquals("", collector.key)
        assertEquals("", collector.action)
    }

    @Test
    fun idReturnsKey() {
        val collector = RecognizeCollector()
        collector.init(buildJsonObject { put("key", "k1") })
        assertEquals("k1", collector.id())
    }

    @Test
    fun payloadReturnsNullBeforeCollect() {
        val collector = RecognizeCollector()
        collector.init(buildJsonObject { put("key", "k") ; put("action", "setup") })
        assertNull(collector.payload())
    }

    // ── collect — success paths ──────────────────────────────────────────────

    @Test
    fun collectDispatchesSetupAndStoresResult() = runTest {
        val collector = RecognizeCollector()
        collector.init(buildJsonObject { put("key", "k") ; put("action", "setup") })

        val result = collector.collect()

        assertTrue(result.isSuccess)
        assertNotNull(collector.payload())
    }

    @Test
    fun collectDispatchesBiomEnrollAndStoresResult() = runTest {
        val collector = RecognizeCollector()
        collector.init(buildJsonObject { put("key", "k") ; put("action", "biom_enroll") })

        val result = collector.collect()

        assertTrue(result.isSuccess)
        assertNotNull(collector.payload())
    }

    @Test
    fun collectDispatchesBiomAuthAndStoresResult() = runTest {
        val collector = RecognizeCollector()
        collector.init(buildJsonObject { put("key", "k") ; put("action", "biom_auth") })

        val result = collector.collect()

        assertTrue(result.isSuccess)
        assertNotNull(collector.payload())
    }

    // ── collect — failure paths ──────────────────────────────────────────────

    @Test
    fun collectReturnsFailureOnSdkException() = runTest {
        coEvery { Recognize.setup(any()) } throws RuntimeException("sdk error")

        val collector = RecognizeCollector()
        collector.init(buildJsonObject { put("key", "k") ; put("action", "setup") })

        val result = collector.collect()

        assertTrue(result.isFailure)
        assertEquals("sdk error", result.exceptionOrNull()?.message)
        assertNull(collector.payload())
    }

    @Test
    fun collectReturnsFailureForUnknownAction() = runTest {
        val collector = RecognizeCollector()
        collector.init(buildJsonObject { put("key", "k") ; put("action", "unsupported_action") })

        val result = collector.collect()

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertIs<RecognizeException>(exception)
        assertTrue(
            exception.message?.contains("unsupported_action") == true,
            "Exception message should include the unknown action value"
        )
    }

    @Test
    fun payloadRemainsNullAfterFailure() = runTest {
        coEvery { Recognize.enroll(any()) } returns Result.failure(RecognizeException("fail"))

        val collector = RecognizeCollector()
        collector.init(buildJsonObject { put("key", "k") ; put("action", "biom_enroll") })
        collector.collect()

        assertNull(collector.payload())
    }
}
