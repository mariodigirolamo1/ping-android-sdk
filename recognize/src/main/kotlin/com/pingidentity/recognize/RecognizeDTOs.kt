/*
 * Copyright (c) 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.pingidentity.recognize

import kotlinx.serialization.Serializable

/**
 * Config DTO for the Recognize SDK setup operation.
 *
 * TODO: Populate constructor parameters once the server JSON contract for the setup action
 *       is confirmed. Expected fields may include: tenant URL, application ID, API key,
 *       environment ID, feature flags, etc. Map each field to the corresponding Recognize SDK
 *       native type.
 */
@Serializable
class SetupConfigDTO

/**
 * Config DTO for the Recognize SDK biometric enroll operation.
 *
 * TODO: Populate constructor parameters once the server JSON contract for the biometric enroll
 *       action is confirmed. Expected fields may include: user ID, enrollment token, biometric
 *       type, challenge, etc. Map each field to the corresponding Recognize SDK native enroll
 *       config type.
 */
@Serializable
class BiomEnrollConfigDTO

/**
 * Config DTO for the Recognize SDK biometric authentication operation.
 *
 * TODO: Populate constructor parameters once the server JSON contract for the biometric
 *       authenticate action is confirmed. Expected fields may include: user ID, challenge,
 *       session token, etc. Map each field to the corresponding Recognize SDK native auth
 *       config type.
 */
@Serializable
class BiomAuthConfigDTO

/**
 * Config DTO for the Recognize SDK biometric de-enroll operation.
 *
 * TODO: Populate constructor parameters once the server JSON contract for the biometric de-enroll
 *       action is confirmed. Expected fields may include: user ID, enrollment ID, de-enroll
 *       token, etc. Map each field to the corresponding Recognize SDK native de-enroll config type.
 */
@Serializable
class BiomDeenrollConfigDTO
