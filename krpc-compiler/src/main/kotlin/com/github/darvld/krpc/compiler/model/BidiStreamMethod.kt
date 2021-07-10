/*
 *    Copyright 2021 Dario Valdespino.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.github.darvld.krpc.compiler.model

import com.github.darvld.krpc.BidiStream
import com.github.darvld.krpc.compiler.reportError
import com.github.darvld.krpc.compiler.resolveAsParameterizedName
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.TypeName
import io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING

/**Contains information about a service method annotated with [BidiStream].*/
class BidiStreamMethod(
    declaredName: String,
    methodName: String,
    requestName: String,
    requestType: TypeName,
    responseType: TypeName,
) : ServiceMethodDefinition(
    declaredName,
    methodName,
    requestName,
    isSuspending = false,
    methodType = BIDI_STREAMING,
    requestType, responseType
) {
    companion object {
        /**The simple name of the [BidiStream] annotation.*/
        val AnnotationName = BidiStream::class.simpleName!!

        /**Extracts a [BidiStreamMethod] from a function [declaration] given the corresponding [BidiStream] annotation.*/
        fun extractFrom(declaration: KSFunctionDeclaration, annotation: KSAnnotation): BidiStreamMethod {
            declaration.requireSuspending(false, "BidiStream rpc methods must not be marked with 'suspend' modifier.")

            val methodName = declaration.extractMethodName(annotation)

            // Resolve the return type, which should yield a Flow<T>, and extract the 'T' type name.
            val responseType = declaration.returnType?.resolveAsParameterizedName()
                ?.typeArguments
                ?.singleOrNull()
                ?: reportError(
                    declaration,
                    message = "BidiStream rpc methods must return a Flow of a serializable type."
                )

            val (requestName, requestType) = declaration.extractRequestInfo { reference ->
                // Resolve the request type, which should be a Flow<T>, and extract the 'T' type name.
                reference.resolveAsParameterizedName()?.typeArguments?.singleOrNull()
            } ?: reportError(
                declaration,
                message = "BidiStream rpc methods must declare a Flow of a serializable type as the only parameter."
            )

            return BidiStreamMethod(
                declaredName = declaration.simpleName.asString(),
                methodName,
                requestName,
                requestType,
                responseType
            )
        }
    }
}