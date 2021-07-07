package com.github.darvld.krpc.compiler.model

import com.github.darvld.krpc.ClientStream
import com.github.darvld.krpc.compiler.UnitClassName
import com.github.darvld.krpc.compiler.reportError
import com.github.darvld.krpc.compiler.resolveAsClassName
import com.github.darvld.krpc.compiler.resolveParameterizedName
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING

/**Contains information about a service method annotated with [ClientStream].*/
class ClientStreamMethod(
    declaredName: String,
    methodName: String,
    requestName: String,
    override val requestType: ParameterizedTypeName,
    override val returnType: ClassName
) : ServiceMethodDefinition(
    declaredName,
    methodName,
    requestName,
    isSuspending = true,
    methodType = CLIENT_STREAMING
) {
    companion object {
        /**The simple name of the [ClientStream] annotation.*/
        val AnnotationName = ClientStream::class.simpleName!!
        
        /**Extracts a [ClientStreamMethod] from a function [declaration] given the corresponding [ClientStream] annotation.*/
        fun extractFrom(declaration: KSFunctionDeclaration, annotation: KSAnnotation): ClientStreamMethod {
            val methodName = declaration.extractMethodName(annotation)
            val returnType = declaration.returnType?.resolveAsClassName() ?: UnitClassName
            
            val (requestName, requestType) = declaration.extractRequestInfo { it.resolveParameterizedName() }
                ?: reportError(declaration, "ClientStream rpc methods must have a single Flow parameter.")
            
            return ClientStreamMethod(
                declaredName = declaration.simpleName.asString(),
                methodName,
                requestName,
                requestType,
                returnType
            )
        }
    }
}