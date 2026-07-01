/*
 * Copyright (c) 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.pingidentity.recognize

import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class RecognizeTest {

    @BeforeTest
    fun setUp() {
        Recognize.reset()
    }

    @Test
    fun configStoresProvidedValues() {
        Recognize.config {
            envId = "test-env"
            isConsoleLogEnabled = true
        }
        // Access private field via reflection to verify — matches the Protect test pattern.
        val config = Recognize::class.java
            .getDeclaredField("recognizeConfig")
            .apply { isAccessible = true }
            .get(Recognize) as RecognizeConfig
        assertEquals("test-env", config.envId)
        assertEquals(true, config.isConsoleLogEnabled)
    }

    @Test
    fun initializeIsIdempotent() = runTest {
        Recognize.config { }
        Recognize.initialize()
        Recognize.initialize() // second call must not throw
    }

    @Test
    fun dataThrowsRecognizeExceptionWhenSdkNotWiredIn() = runTest {
        Recognize.config { }
        Recognize.initialize()
        val exception = runCatching { Recognize.data() }.exceptionOrNull()
        assertIs<RecognizeException>(exception)
    }

    @Test
    fun pauseAndResumeDataCollectionDoNotThrow() {
        // Placeholder smoke-test — verifies no crash until the real SDK is wired in.
        Recognize.pauseDataCollection()
        Recognize.resumeDataCollection()
    }

    @Test
    fun resetClearsInitializationState() = runTest {
        Recognize.config { }
        Recognize.initialize()
        Recognize.reset()
        val isInitialized = Recognize::class.java
            .getDeclaredField("isInitialized")
            .apply { isAccessible = true }
            .get(Recognize) as Boolean
        assertFalse(isInitialized)
    }
}
