/*
 * Copyright (c) 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.pingidentity.recognize.journey

import com.pingidentity.journey.plugin.Callback
import com.pingidentity.journey.plugin.ValueCallback
import com.pingidentity.orchestrate.ContinueNode
import com.pingidentity.recognize.Recognize
import com.pingidentity.recognize.RecognizeException
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for [RecognizeActionCallback].
 *
 * Two delivery modes are exercised:
 * - **Typed-callback mode**: first-class JSON with `output[]` array and `input[]` array.
 *   [signal]/[error] write to `json["input"]` via [AbstractCallback.input].
 * - **MetadataCallback mode**: `type == "MetadataCallback"` with `data` object. [signal]/[error]
 *   write to the sibling [ValueCallback] instances on [continueNode].
 */
class RecognizeActionCallbackTest {

    private val sdkResult: JsonObject = buildJsonObject { put("status", "ok") }

    @BeforeTest
    fun setUp() {
        mockkObject(Recognize)
        coEvery { Recognize.setup(any()) } returns sdkResult
        coEvery { Recognize.enroll(any()) } returns sdkResult
        coEvery { Recognize.authenticate(any()) } returns sdkResult
        coEvery { Recognize.deenroll(any()) } returns sdkResult
    }

    @AfterTest
    fun tearDown() {
        unmockkObject(Recognize)
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    /**
     * Typed-callback JSON. The `input` array carries two entries in order:
     * [0] → recognize_signals, [1] → clientError.
     */
    private fun typedJson(action: String): JsonObject = Json.parseToJsonElement(
        """
        {
          "type": "RecognizeActionCallback",
          "output": [
            { "name": "action", "value": "$action" }
          ],
          "input": [
            { "name": "IDToken1recognize_signals", "value": "" },
            { "name": "IDToken1clientError",       "value": "" }
          ]
        }
        """
    ) as JsonObject

    /**
     * MetadataCallback JSON. [signal]/[error] write to sibling [ValueCallback] instances on
     * [continueNode] (derivedCallback mode).
     */
    private fun metadataJson(action: String): JsonObject = Json.parseToJsonElement(
        """
        {
          "type": "MetadataCallback",
          "output": [
            {
              "name": "data",
              "value": {
                "_type": "Recognize",
                "_action": "$action"
              }
            }
          ],
          "_id": 0
        }
        """
    ) as JsonObject

    /**
     * Creates a [ContinueNode] mock with two sibling [ValueCallback] instances:
     * one for `recognize_signals` and one for `clientError`.
     */
    private fun makeContinueNode(): Triple<ContinueNode, ValueCallback, ValueCallback> {
        val signalsCallback = object : ValueCallback {
            override val id: String = "IDToken1recognize_signals"
            override var value: String = ""
            override fun init(jsonObject: JsonObject): Callback = this
            override fun payload(): JsonObject = buildJsonObject {}
        }
        val errorCallback = object : ValueCallback {
            override val id: String = "IDToken1clientError"
            override var value: String = ""
            override fun init(jsonObject: JsonObject): Callback = this
            override fun payload(): JsonObject = buildJsonObject {}
        }
        val continueNode = mockk<ContinueNode>()
        every { continueNode.actions } returns listOf(signalsCallback, errorCallback)
        return Triple(continueNode, signalsCallback, errorCallback)
    }

    // ---------------------------------------------------------------------------
    // Typed-callback mode — action field read from output array
    // ---------------------------------------------------------------------------

    @Test
    fun `typed-callback setup action parsed and dispatched`() = runTest {
        val callback = RecognizeActionCallback().apply { init(typedJson("setup")) }
        assertEquals("setup", callback.action)
        assertTrue(callback.collect().isSuccess)
    }

    @Test
    fun `typed-callback biom_enroll action parsed and dispatched`() = runTest {
        val callback = RecognizeActionCallback().apply { init(typedJson("biom_enroll")) }
        assertEquals("biom_enroll", callback.action)
        assertTrue(callback.collect().isSuccess)
    }

    @Test
    fun `typed-callback biom_auth action parsed and dispatched`() = runTest {
        val callback = RecognizeActionCallback().apply { init(typedJson("biom_auth")) }
        assertEquals("biom_auth", callback.action)
        assertTrue(callback.collect().isSuccess)
    }

    @Test
    fun `typed-callback biom_deenroll action parsed and dispatched`() = runTest {
        val callback = RecognizeActionCallback().apply { init(typedJson("biom_deenroll")) }
        assertEquals("biom_deenroll", callback.action)
        assertTrue(callback.collect().isSuccess)
    }

    /**
     * In typed-callback mode, [signal] writes to `json["input"]` via [AbstractCallback.input].
     * The first input entry receives the serialized sdkResult; the second remains empty.
     */
    @Test
    fun `typed-callback collect writes serialized result to first input entry`() = runTest {
        val callback = RecognizeActionCallback().apply { init(typedJson("setup")) }
        val result = callback.collect()
        assertTrue(result.isSuccess)
        // signal goes to input[0] (recognize_signals); must be non-empty
        val signalValue = callback.payload()["input"]?.jsonArray?.get(0)
            ?.jsonObject?.get("value")?.jsonPrimitive?.content
        assertTrue(signalValue != null && signalValue.isNotEmpty())
        // clientError input[1] should remain empty on success
        val errorValue = callback.payload()["input"]?.jsonArray?.get(1)
            ?.jsonObject?.get("value")?.jsonPrimitive?.content
        assertTrue(errorValue == null || errorValue.isEmpty())
    }

    // ---------------------------------------------------------------------------
    // MetadataCallback mode — _action field from data object
    // ---------------------------------------------------------------------------

    @Test
    fun `MetadataCallback _action setup parsed and dispatched`() = runTest {
        val (continueNode, _, _) = makeContinueNode()
        val callback = RecognizeActionCallback().apply {
            init(metadataJson("setup"))
            this.continueNode = continueNode
        }
        assertEquals("setup", callback.action)
        assertTrue(callback.collect().isSuccess)
    }

    @Test
    fun `MetadataCallback _action biom_enroll parsed and dispatched`() = runTest {
        val (continueNode, _, _) = makeContinueNode()
        val callback = RecognizeActionCallback().apply {
            init(metadataJson("biom_enroll"))
            this.continueNode = continueNode
        }
        assertEquals("biom_enroll", callback.action)
        assertTrue(callback.collect().isSuccess)
    }

    @Test
    fun `MetadataCallback _action biom_auth parsed and dispatched`() = runTest {
        val (continueNode, _, _) = makeContinueNode()
        val callback = RecognizeActionCallback().apply {
            init(metadataJson("biom_auth"))
            this.continueNode = continueNode
        }
        assertEquals("biom_auth", callback.action)
        assertTrue(callback.collect().isSuccess)
    }

    @Test
    fun `MetadataCallback _action biom_deenroll parsed and dispatched`() = runTest {
        val (continueNode, _, _) = makeContinueNode()
        val callback = RecognizeActionCallback().apply {
            init(metadataJson("biom_deenroll"))
            this.continueNode = continueNode
        }
        assertEquals("biom_deenroll", callback.action)
        assertTrue(callback.collect().isSuccess)
    }

    /**
     * In MetadataCallback mode, [signal] writes to the sibling [ValueCallback] instances
     * on [continueNode] (derivedCallback path).
     */
    @Test
    fun `MetadataCallback collect writes serialized result to signals ValueCallback`() = runTest {
        val (continueNode, signalsCallback, errorCallback) = makeContinueNode()
        val callback = RecognizeActionCallback().apply {
            init(metadataJson("setup"))
            this.continueNode = continueNode
        }
        assertTrue(callback.collect().isSuccess)
        // signal ValueCallback must receive the serialized sdkResult
        assertTrue(signalsCallback.value.isNotEmpty())
        // clientError ValueCallback must remain empty on success
        assertTrue(errorCallback.value.isEmpty())
    }

    // ---------------------------------------------------------------------------
    // Error paths
    // ---------------------------------------------------------------------------

    @Test
    fun `unknown action returns Result failure with RecognizeException`() = runTest {
        val callback = RecognizeActionCallback().apply { init(typedJson("unknown_op")) }
        val result = callback.collect()
        assertTrue(result.isFailure)
        assertIs<RecognizeException>(result.exceptionOrNull())
    }

    /**
     * Unknown action in typed-callback mode: [error] writes to `json["input"]` via
     * [AbstractCallback.input], populating the first input entry (clientError slot at index 0
     * when signal is absent).
     */
    @Test
    fun `unknown action sets clientError in typed-callback mode`() = runTest {
        val callback = RecognizeActionCallback().apply { init(typedJson("unknown_op")) }
        callback.collect()
        // In unknown-action branch, error(message) calls input(message) with one argument
        // → only input[0] is touched; it receives the error message
        val inputValue = callback.payload()["input"]?.jsonArray?.get(0)
            ?.jsonObject?.get("value")?.jsonPrimitive?.content
        assertTrue(inputValue != null && inputValue.isNotEmpty())
    }

    /**
     * Unknown action in MetadataCallback mode: [error] writes to the clientError [ValueCallback].
     */
    @Test
    fun `unknown action sets clientError ValueCallback in MetadataCallback mode`() = runTest {
        val (continueNode, signalsCallback, errorCallback) = makeContinueNode()
        val callback = RecognizeActionCallback().apply {
            init(metadataJson("unknown_op"))
            this.continueNode = continueNode
        }
        callback.collect()
        assertTrue(signalsCallback.value.isEmpty())
        assertTrue(errorCallback.value.isNotEmpty())
    }

    /**
     * SDK exception in typed-callback mode: [error] writes to `json["input"]`.
     */
    @Test
    fun `SDK exception in typed-callback mode returns failure and sets error in input`() = runTest {
        coEvery { Recognize.setup(any()) } throws IOException("SDK call failed")
        val callback = RecognizeActionCallback().apply { init(typedJson("setup")) }
        val result = callback.collect()
        assertTrue(result.isFailure)
        assertIs<IOException>(result.exceptionOrNull())
        // error("SDK call failed") → input("SDK call failed") → input[0]
        val inputValue = callback.payload()["input"]?.jsonArray?.get(0)
            ?.jsonObject?.get("value")?.jsonPrimitive?.content
        assertEquals("SDK call failed", inputValue)
    }

    /**
     * SDK exception in MetadataCallback mode: [error] writes to the clientError [ValueCallback].
     */
    @Test
    fun `SDK exception in MetadataCallback mode returns failure and sets clientError ValueCallback`() = runTest {
        coEvery { Recognize.enroll(any()) } throws IOException("enroll failed")
        val (continueNode, signalsCallback, errorCallback) = makeContinueNode()
        val callback = RecognizeActionCallback().apply {
            init(metadataJson("biom_enroll"))
            this.continueNode = continueNode
        }
        val result = callback.collect()
        assertTrue(result.isFailure)
        assertIs<IOException>(result.exceptionOrNull())
        assertTrue(signalsCallback.value.isEmpty())
        assertEquals("enroll failed", errorCallback.value)
    }
}
