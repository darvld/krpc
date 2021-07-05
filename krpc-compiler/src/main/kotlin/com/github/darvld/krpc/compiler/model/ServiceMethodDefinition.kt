package com.github.darvld.krpc.compiler.model

import com.squareup.kotlinpoet.ClassName
import io.grpc.MethodDescriptor

/**Model class used by [ServiceProcessor][com.github.darvld.krpc.compiler.ServiceProcessor] to store information
 * about individual service methods.
 *
 * @see [ServiceDefinition]*/
data class ServiceMethodDefinition(
    /**The name of the method as declared in the service interface.*/
    val declaredName: String,
    /**The "official" GRPC name of this method.*/
    val methodName: String,
    /**Return type of the method.*/
    val returnType: ClassName,
    /**The request parameter, required by GRPC.*/
    val request: Pair<String, ClassName>,
    /**The type of rpc call this method represents.*/
    val methodType: MethodDescriptor.MethodType
) {
    /**Whether this method returns [Unit].*/
    val returnsUnit: Boolean get() = returnType.simpleName == "Unit"
}