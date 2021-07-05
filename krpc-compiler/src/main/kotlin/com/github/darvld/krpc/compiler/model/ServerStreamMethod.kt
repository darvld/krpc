package com.github.darvld.krpc.compiler.model

import com.github.darvld.krpc.BidiStream
import com.github.darvld.krpc.ServerStream
import com.github.darvld.krpc.compiler.UnitClassName
import com.github.darvld.krpc.compiler.reportError
import com.github.darvld.krpc.compiler.resolveAsClassName
import com.github.darvld.krpc.compiler.resolveParameterizedName
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING

/**Contains information about a service method annotated with [ServerStream].*/
class ServerStreamMethod(
    declaredName: String,
    methodName: String,
    requestName: String,
    override val returnType: ParameterizedTypeName,
    override val requestType: ClassName
) : ServiceMethodDefinition(
    declaredName,
    methodName,
    requestName,
    isSuspending = false,
    methodType = SERVER_STREAMING
) {
    companion object {
        /**The simple name of the [ServerStream] annotation.*/
        val AnnotationName = ServerStream::class.simpleName!!

        /**Extracts a [ServerStreamMethod] from a function [declaration] given the corresponding [ServerStream] annotation.*/
        fun extractFrom(declaration: KSFunctionDeclaration, annotation: KSAnnotation): ServerStreamMethod {
            val methodName = declaration.extractMethodName(annotation)
            val returnType = declaration.returnType?.resolveParameterizedName {
                it.declaration.simpleName.asString() == "Flow"
            } ?: reportError(declaration, "ServerStream rpc methods must return a Flow of a serializable type.")

            val (requestName, requestType) = declaration.extractRequestInfo { it.resolveAsClassName() }
                ?: "unit" to UnitClassName

            return ServerStreamMethod(
                declaredName = declaration.simpleName.asString(),
                methodName,
                requestName,
                returnType,
                requestType
            )
        }
    }
}