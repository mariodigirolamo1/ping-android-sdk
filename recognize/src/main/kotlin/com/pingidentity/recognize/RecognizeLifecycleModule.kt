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
 * Orchestration module that propagates global Recognize SDK configuration into the
 * authentication flow.
 *
 * Setup is server-driven: the [RecognizeLifecycle] module does **not** call any Recognize
 * operation implicitly. Operations ([Recognize.setup], [Recognize.enroll],
 * [Recognize.authenticate], [Recognize.deenroll]) are invoked only when the server requests
 * them via the `action` discriminator in the collector or callback JSON.
 *
 * Usage (DaVinci example):
 * ```kotlin
 * daVinci {
 *     module(RecognizeLifecycle) {
 *         // TODO: add SDK-level config fields here once the contract is confirmed
 *     }
 * }
 * ```
 */
val RecognizeLifecycle =
    Module.of(::RecognizeLifecycleConfig) {
        init {
            Recognize.config {
                // TODO: forward SDK-level config fields here once RecognizeConfig carries them,
                //       e.g.: apiKey = config.apiKey
            }
        }
    }

/**
 * Configuration for [RecognizeLifecycle].
 *
 * TODO: Add lifecycle-specific options once the Recognize SDK contract is confirmed.
 *       For example: whether to eagerly preload the SDK on workflow init, timeout settings, etc.
 *       Data-collection lifecycle flags (pause/resume) are intentionally absent — setup is
 *       server-driven and each operation is requested explicitly by the server.
 */
@PingDsl
class RecognizeLifecycleConfig : RecognizeConfig()
