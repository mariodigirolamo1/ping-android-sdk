/*
 * Copyright (c) 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.pingidentity.recognize

import com.pingidentity.orchestrate.Workflow
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class RecognizeLifecycleTest {

    @BeforeTest
    fun setUp() {
        mockkObject(Recognize)
        every { Recognize.config(any()) } answers { /* no-op mock */ }
        every { Recognize.reset() } answers { /* no-op mock */ }
    }

    @AfterTest
    fun tearDown() {
        unmockkObject(Recognize)
    }

    @Test
    fun initCallsRecognizeConfigExactlyOnce() = runTest {
        val workflow = Workflow {
            module(RecognizeLifecycle) {
                // TODO: pass config fields here once RecognizeConfig carries them
            }
        }
        workflow.init()

        verify(exactly = 1) { Recognize.config(any()) }
    }

    @Test
    fun initDoesNotCallAnyRecognizeOperation() = runTest {
        val workflow = Workflow {
            module(RecognizeLifecycle) { }
        }
        workflow.init()

        // None of the four action operations should be invoked from the lifecycle init block.
        coVerify(exactly = 0) { Recognize.setup(any()) }
        coVerify(exactly = 0) { Recognize.enroll(any()) }
        coVerify(exactly = 0) { Recognize.authenticate(any()) }
        coVerify(exactly = 0) { Recognize.deenroll(any()) }
    }

    @Test
    fun initCalledOnlyOnceOnMultipleWorkflowInits() = runTest {
        val workflow = Workflow {
            module(RecognizeLifecycle) { }
        }
        // The workflow guarantees init blocks run only once; verify no double invocation.
        workflow.init()
        workflow.init()

        verify(exactly = 1) { Recognize.config(any()) }
    }
}
