package com.github.darvld.krpc

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class UnaryCall(val methodName: String = "")
