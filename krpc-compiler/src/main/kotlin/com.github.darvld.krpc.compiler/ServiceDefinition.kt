package com.github.darvld.krpc.compiler

data class ServiceDefinition(
    val packageName: String,
    val serviceName: String,
    val clientName: String,
    val providerName: String,
    val methods: List<ServiceMethodDefinition>
) {
    inline val definitionsHelperName: String get() = "${serviceName}Definitions"
}