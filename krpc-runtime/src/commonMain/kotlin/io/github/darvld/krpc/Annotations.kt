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

/**Marker for kRPC service definitions. This annotation serves as an entry point for the kRPC symbol processor.
 *
 * See the annotation arguments for customization options.
 *
 * Service definitions must be interfaces containing only methods annotated with @[UnaryCall], @[ServerStream],
 * @[ClientStream] or @[BidiStream], and complying with the corresponding signatures. Any other method will cause
 * a processing error.*/
@Target(AnnotationTarget.CLASS)
annotation class Service(
    /**Sets the name of this service within gRPC, if none is specified, the name of the interface is used.*/
    val overrideName: String = "",
    /**Sets the name of the abstract server implementation generated by the compiler. If not specified, the
     * interface name is used in the format "${interfaceName}Provider"*/
    val providerName: String = "",
    /**Sets the name of the client implementation generated by the compiler. If not specified, the
     * interface name is used in the format "${interfaceName}Client", removing any "Service" suffix on the original name.*/
    val clientName: String = ""
)

/**Marks a method inside an interface annotated with @[Service] as a bidirectional streaming rpc.
 *
 * The rpc method name can be specified through [methodName] parameter.
 *
 * Methods with this annotation must *not* be marked as suspend, require a single flow as argument, and
 * must also return a flow:
 * ```
 * @BidiStream
 * fun openChannel(upstream: Flow<String>): Flow<String>
 * ```
 *
 * @see ClientStream
 * @see ServerStream
 * */
@Target(AnnotationTarget.FUNCTION)
annotation class BidiStream(
    /**Sets the rpc method's name within gRPC. This argument takes precedence over the declared method name.*/
    val methodName: String = ""
)


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
annotation class ClientStream(
    /**Sets the rpc method's name within gRPC. This argument takes precedence over the declared method name.*/
    val methodName: String = ""
)


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
annotation class ServerStream(
    /**Sets the rpc method's name within gRPC. This argument takes precedence over the declared method name.*/
    val methodName: String = ""
)


/**Marks a method inside an interface annotated with @[Service] as an unary rpc call.
 *
 * Methods with this annotation *must* be marked as suspend, can have zero or one parameters, and return any
 * serializable type. Return type may be omitted.
 * ```
 * @UnaryCall
 * suspend fun fireAndForget(data: String)
 *
 * @UnaryCall
 * suspend fun getData(request: String): Int
 *
 * @UnaryCall
 * suspend fun notifyReady()
 * ```
 *
 * @see ClientStream
 * @see ServerStream
 * */
@Target(AnnotationTarget.FUNCTION)
annotation class UnaryCall(
    /**Sets the rpc method's name within gRPC. This argument takes precedence over the declared method name.*/
    val methodName: String = ""
)
