package com.github.darvld.krpc

/**Marks a method inside an interface annotated with @[Service] as a bidirectional streaming rpc.
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
@Retention(AnnotationRetention.SOURCE)
annotation class BidiStream(val methodName: String = "")
