package com.github.darvld.krpc.compiler

import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import io.grpc.MethodDescriptor

data class ServiceMethodDefinition(
    val methodName: String,
    val returnType: KSTypeReference?,
    val request: KSValueParameter,
    val methodType: MethodDescriptor.MethodType
)