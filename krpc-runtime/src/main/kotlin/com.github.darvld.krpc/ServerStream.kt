package com.github.darvld.krpc

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
annotation class ServerStream(val methodName: String = "")
