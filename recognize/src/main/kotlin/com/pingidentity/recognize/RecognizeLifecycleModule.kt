/*
 * Copyright (c) 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.pingidentity.recognize

import com.pingidentity.orchestrate.Module
import com.pingidentity.utils.PingDsl

/**
 * Orchestration module that ties the Recognize SDK lifecycle into the authentication flow.
 *
 * Usage (DaVinci example):
 * ```kotlin
 * daVinci {
 *     module(RecognizeLifecycle) {
 *         envId = "your-env-id"
 *         resumeDataCollectionOnStart = true
 *         pauseDataCollectionOnSuccess = true
 *     }
 * }
 * ```
 *
 * The module initializes [Recognize] on flow start, optionally resumes data collection at the
 * beginning of each flow step, and optionally pauses it on successful authentication.
 */
val RecognizeLifecycle =
    Module.of(::RecognizeLifecycleConfig) {
        init {
            Recognize.config {
                envId = config.envId
                isConsoleLogEnabled = config.isConsoleLogEnabled
            }
            Recognize.initialize()
        }

        start {
            if (config.resumeDataCollectionOnStart) {
                Recognize.resumeDataCollection()
            }
            it
        }

        success {
            if (config.pauseDataCollectionOnSuccess) {
                Recognize.pauseDataCollection()
            }
            it
        }
    }

/**
 * Configuration for [RecognizeLifecycle].
 * Extends [RecognizeConfig] with lifecycle-specific flags.
 */
@PingDsl
class RecognizeLifecycleConfig : RecognizeConfig() {
    /** Pause data collection when authentication succeeds. Default: false. */
    var pauseDataCollectionOnSuccess: Boolean = false

    /** Resume data collection when the flow starts a new step. Default: false. */
    var resumeDataCollectionOnStart: Boolean = false
}
