package com.github.darvld.krpc

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ServerStream(val methodName: String = "")
