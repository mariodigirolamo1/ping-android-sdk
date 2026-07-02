/*
 * Copyright (c) 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.pingidentity.recognize

import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class RecognizeTest {

    @BeforeTest
    fun setUp() {
        Recognize.reset()
    }

    @AfterTest
    fun tearDown() {
        unmockkObject(Recognize)
    }

    @Test
    fun configDslIsCallableWithoutCrash() {
        // Verifies the config {} DSL accepts a block and does not throw.
        Recognize.config {
            // TODO: assert specific fields once RecognizeConfig carries them
        }
        // Access private field via reflection to verify the config was applied.
        val config = Recognize::class.java
            .getDeclaredField("recognizeConfig")
            .apply { isAccessible = true }
            .get(Recognize)
        assertNotNull(config, "recognizeConfig should be set after calling Recognize.config {}")
    }

    @Test
    fun resetIsCallableWithoutCrash() {
        // Smoke-test: reset() must not throw even before config() is called.
        Recognize.reset()
    }

    @Test
    fun resetAfterConfigIsCallableWithoutCrash() {
        Recognize.config { }
        Recognize.reset()
    }

    @Test
    fun setupIsMockableViaMockkObject() = runTest {
        val expected: JsonObject = buildJsonObject { put("result", "setup_ok") }
        mockkObject(Recognize)
        coEvery { Recognize.setup(any()) } returns expected

        val result = Recognize.setup(SetupConfigDTO())
        assert(result == expected) { "setup() should return the mocked value" }
    }

    @Test
    fun enrollIsMockableViaMockkObject() = runTest {
        val expected: JsonObject = buildJsonObject { put("result", "enroll_ok") }
        mockkObject(Recognize)
        coEvery { Recognize.enroll(any()) } returns expected

        val result = Recognize.enroll(BiomEnrollConfigDTO())
        assert(result == expected) { "enroll() should return the mocked value" }
    }

    @Test
    fun authenticateIsMockableViaMockkObject() = runTest {
        val expected: JsonObject = buildJsonObject { put("result", "authenticate_ok") }
        mockkObject(Recognize)
        coEvery { Recognize.authenticate(any()) } returns expected

        val result = Recognize.authenticate(BiomAuthConfigDTO())
        assert(result == expected) { "authenticate() should return the mocked value" }
    }

    @Test
    fun deenrollIsMockableViaMockkObject() = runTest {
        val expected: JsonObject = buildJsonObject { put("result", "deenroll_ok") }
        mockkObject(Recognize)
        coEvery { Recognize.deenroll(any()) } returns expected

        val result = Recognize.deenroll(BiomDeenrollConfigDTO())
        assert(result == expected) { "deenroll() should return the mocked value" }
    }

    @Test
    fun setupThrowsNotImplementedErrorWhenSdkNotWiredIn() = runTest {
        val exception = runCatching { Recognize.setup(SetupConfigDTO()) }.exceptionOrNull()
        assertNotNull(exception, "setup() should throw when SDK is not wired in")
        assert(exception is NotImplementedError) {
            "Expected NotImplementedError from TODO stub, got ${exception?.javaClass?.name}"
        }
    }

    @Test
    fun enrollThrowsNotImplementedErrorWhenSdkNotWiredIn() = runTest {
        val exception = runCatching { Recognize.enroll(BiomEnrollConfigDTO()) }.exceptionOrNull()
        assertNotNull(exception, "enroll() should throw when SDK is not wired in")
        assert(exception is NotImplementedError) {
            "Expected NotImplementedError from TODO stub, got ${exception?.javaClass?.name}"
        }
    }

    @Test
    fun authenticateThrowsNotImplementedErrorWhenSdkNotWiredIn() = runTest {
        val exception = runCatching { Recognize.authenticate(BiomAuthConfigDTO()) }.exceptionOrNull()
        assertNotNull(exception, "authenticate() should throw when SDK is not wired in")
        assert(exception is NotImplementedError) {
            "Expected NotImplementedError from TODO stub, got ${exception?.javaClass?.name}"
        }
    }

    @Test
    fun deenrollThrowsNotImplementedErrorWhenSdkNotWiredIn() = runTest {
        val exception = runCatching { Recognize.deenroll(BiomDeenrollConfigDTO()) }.exceptionOrNull()
        assertNotNull(exception, "deenroll() should throw when SDK is not wired in")
        assert(exception is NotImplementedError) {
            "Expected NotImplementedError from TODO stub, got ${exception?.javaClass?.name}"
        }
    }
}
