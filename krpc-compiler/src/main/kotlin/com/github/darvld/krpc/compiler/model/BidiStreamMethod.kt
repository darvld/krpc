package com.github.darvld.krpc.compiler.model

import com.github.darvld.krpc.BidiStream
import com.github.darvld.krpc.compiler.reportError
import com.github.darvld.krpc.compiler.resolveParameterizedName
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ParameterizedTypeName
import io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING

/**Contains information about a service method annotated with [BidiStream].*/
class BidiStreamMethod(
    declaredName: String,
    methodName: String,
    requestName: String,
    override val requestType: ParameterizedTypeName,
    override val returnType: ParameterizedTypeName,
) : ServiceMethodDefinition(
    declaredName,
    methodName,
    requestName,
    isSuspending = false,
    methodType = BIDI_STREAMING
) {
    companion object {
        /**The simple name of the [BidiStream] annotation.*/
        val AnnotationName = BidiStream::class.simpleName!!

        /**Extracts a [BidiStreamMethod] from a function [declaration] given the corresponding [BidiStream] annotation.*/
        fun extractFrom(declaration: KSFunctionDeclaration, annotation: KSAnnotation): BidiStreamMethod {
            declaration.requireSuspending(false, "BidiStream rpc methods must not be marked with 'suspend' modifier.")
            
            val methodName = declaration.extractMethodName(annotation)
            val returnType = declaration.returnType?.resolveParameterizedName {
                it.declaration.simpleName.asString() == "Flow"
            } ?: reportError(declaration, "BidiStream rpc methods must return a Flow of a serializable type.")

            val (requestName, requestType) = declaration.extractRequestInfo { reference ->
                reference.resolveParameterizedName { it.declaration.simpleName.asString() == "Flow" }
            } ?: reportError(
                declaration,
                "BidiStream rpc methods must declare a Flow of a serializable type as the only parameter."
            )

            return BidiStreamMethod(
                declaredName = declaration.simpleName.asString(),
                methodName,
                requestName,
                requestType,
                returnType
            )
        }
    }
}