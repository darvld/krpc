package com.github.darvld.krpc

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ClientStream(val methodName: String = "")
