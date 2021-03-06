/*
 * Copyright 2017 Adam Jordens
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jordens.sleepybaby

/**
 * Based on ideas described @ https://medium.com/@shazow/how-i-design-json-api-responses-71900f00f2db
 */
data class GenericResponse(val status: String,
                           val code: Int,
                           val messages: List<String>,
                           val result: Map<String, Any>) {

  companion object Factory {
    fun ok(result: Map<String, Any>) = GenericResponse("ok", 200, emptyList(), result)
    fun ok(result: Pair<String, Any>) = GenericResponse("ok", 200, emptyList(), mapOf(result))
  }
}
