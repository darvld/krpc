package com.github.darvld.krpc

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class BidiStream(val methodName: String = "")
