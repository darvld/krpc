/*
 *    Copyright 2021 Dario Valdespino.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.github.darvld.krpc

/**Marks a method inside an interface annotated with @[Service] as a *client* streaming rpc.
 *
 * Methods with this annotation *must* be marked as suspend, require a single flow as argument, and
 * can return any serializable type (or not declare a return type at all):
 * ```
 * @ClientStream
 * suspend fun streamData(upstream: Flow<String>): Int
 *
 * @ClientStream
 * suspend fun streamAndForget(upstream: Flow<String>)
 * ```
 *
 * @see BidiStream
 * @see ServerStream
 * */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ClientStream(
    /**Sets the rpc method's name within gRPC. This argument takes precedence over the declared method name.*/
    val methodName: String = ""
)
