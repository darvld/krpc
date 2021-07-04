package com.github.darvld.krpc.compiler.model

import com.squareup.kotlinpoet.ClassName

data class ServiceDefinition(
    /**The name of the interface used to define the service.*/
    val declaredName: String,
    /**The package name of the service definition, used for the generated sources.*/
    val packageName: String,
    /**The "official" GRPC name for this service.*/
    val serviceName: String,
    /**The name of the pseudo-constructor function to be generated as a client implementation.*/
    val clientName: String,
    /**The name of the generated abstract service provider.*/
    val providerName: String,
    /**A list of methods defined for the service.*/
    val methods: List<ServiceMethodDefinition>
) {
    inline val descriptorName: String get() = "${serviceName}Descriptor"

    val className by lazy { ClassName(packageName, declaredName) }
    val descriptorClassName by lazy { ClassName(packageName, descriptorName) }

    val clientClassName by lazy { ClassName(packageName, clientName) }
    val providerClassName by lazy { ClassName(packageName, providerName) }
}