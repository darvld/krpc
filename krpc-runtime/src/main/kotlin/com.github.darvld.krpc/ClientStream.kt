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
