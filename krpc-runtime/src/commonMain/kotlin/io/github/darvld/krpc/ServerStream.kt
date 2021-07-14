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

package io.github.darvld.krpc

/**Marks a method inside an interface annotated with @[Service] as a *server* streaming rpc.
 *
 * Methods with this annotation must *not* be marked as suspend, can have zero or one arguments, and
 * must return a flow of any serializable type:
 * ```
 * @ServerStream
 * fun streamRoute(request: String): Flow<Int>
 *
 * @ServerStream
 * fun getStream(): Flow<String>
 * ```
 *
 * @see BidiStream
 * @see ClientStream
 * */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ServerStream(
    /**Sets the rpc method's name within gRPC. This argument takes precedence over the declared method name.*/
    val methodName: String = ""
)
