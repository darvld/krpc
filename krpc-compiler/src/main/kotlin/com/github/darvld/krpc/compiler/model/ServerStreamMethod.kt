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

import com.github.darvld.krpc.ServerStream
import com.github.darvld.krpc.compiler.reportError
import com.github.darvld.krpc.compiler.resolveAsParameterizedName
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.TypeName
import io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING

/**Contains information about a service method annotated with [ServerStream].*/
class ServerStreamMethod(
    declaredName: String,
    methodName: String,
    request: RequestInfo,
    responseType: TypeName
) : ServiceMethodDefinition(
    declaredName,
    methodName,
    isSuspending = false,
    methodType = SERVER_STREAMING,
    request,
    responseType
) {
    companion object {
        /**The simple name of the [ServerStream] annotation.*/
        val AnnotationName = ServerStream::class.simpleName!!

        /**Extracts a [ServerStreamMethod] from a function [declaration] given the corresponding [ServerStream] annotation.*/
        fun extractFrom(declaration: KSFunctionDeclaration, annotation: KSAnnotation): ServerStreamMethod {
            declaration.requireSuspending(false, "ServerStream rpc methods must not be marked with 'suspend' modifier.")

            // Resolve the return type, which should yield a Flow<T>, and extract the 'T' type name.
            val responseType = declaration.returnType?.resolveAsParameterizedName()
                ?.typeArguments
                ?.singleOrNull()
                ?: reportError(
                    declaration,
                    message = "ServerStream rpc methods must return a Flow of a serializable type."
                )

            return ServerStreamMethod(
                declaredName = declaration.simpleName.asString(),
                methodName = declaration.extractMethodName(annotation),
                request = RequestInfo.extractFrom(declaration),
                responseType = responseType
            )
        }
    }
}