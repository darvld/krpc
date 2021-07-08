package com.github.darvld.krpc.compiler.model

import com.github.darvld.krpc.ServerStream
import com.github.darvld.krpc.compiler.UnitClassName
import com.github.darvld.krpc.compiler.reportError
import com.github.darvld.krpc.compiler.resolveAsClassName
import com.github.darvld.krpc.compiler.resolveAsParameterizedName
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.TypeName
import io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING

/**Contains information about a service method annotated with [ServerStream].*/
class ServerStreamMethod(
    declaredName: String,
    methodName: String,
    requestName: String,
    requestType: TypeName,
    responseType: TypeName
) : ServiceMethodDefinition(
    declaredName,
    methodName,
    requestName,
    isSuspending = false,
    methodType = SERVER_STREAMING,
    requestType, responseType
) {
    companion object {
        /**The simple name of the [ServerStream] annotation.*/
        val AnnotationName = ServerStream::class.simpleName!!
        
        /**Extracts a [ServerStreamMethod] from a function [declaration] given the corresponding [ServerStream] annotation.*/
        fun extractFrom(declaration: KSFunctionDeclaration, annotation: KSAnnotation): ServerStreamMethod {
            declaration.requireSuspending(false, "ServerStream rpc methods must not be marked with 'suspend' modifier.")
            
            val methodName = declaration.extractMethodName(annotation)
            
            // Resolve the return type, which should yield a Flow<T>, and extract the 'T' type name.
            val responseType = declaration.returnType?.resolveAsParameterizedName()
                ?.typeArguments
                ?.singleOrNull()
                ?: reportError(
                    declaration,
                    message = "ServerStream rpc methods must return a Flow of a serializable type."
                )
            
            val (requestName, requestType) = declaration.extractRequestInfo { it.resolveAsClassName() }
                ?: "unit" to UnitClassName
            
            return ServerStreamMethod(
                declaredName = declaration.simpleName.asString(),
                methodName,
                requestName,
                requestType,
                responseType
            )
        }
    }
}