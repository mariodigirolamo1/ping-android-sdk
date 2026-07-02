/*
 * Copyright (c) 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.pingidentity.recognize

/**
 * Exception class for handling errors in the Recognize library.
 *
 * @param message The detail message for the exception.
 */
class RecognizeException(message: String?) : Exception(message)
