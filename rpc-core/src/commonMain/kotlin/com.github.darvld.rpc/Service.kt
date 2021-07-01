package com.github.darvld.rpc

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Service(
    val serviceName: String = "",
    val serverStubName: String = "",
    val clientConstructorName: String = ""
)