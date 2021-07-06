package com.github.darvld.krpc

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
@Retention(AnnotationRetention.SOURCE)
annotation class UnaryCall(
    /**Sets the rpc method's name within gRPC. This argument takes precedence over the declared method name.*/
    val methodName: String = ""
)
