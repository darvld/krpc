package com.github.darvld.krpc.compiler.model

import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import io.grpc.MethodDescriptor

data class ServiceMethodDefinition(
    /**The name of the method as declared in the service interface.*/
    val declaredName: String,
    /**The "official" GRPC name of this method.*/
    val methodName: String,
    val returnType: KSTypeReference?,
    val request: KSValueParameter,
    val methodType: MethodDescriptor.MethodType
)