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

import com.github.darvld.krpc.ClientStream
import com.github.darvld.krpc.compiler.UnitClassName
import com.github.darvld.krpc.compiler.reportError
import com.github.darvld.krpc.compiler.resolveAsParameterizedName
import com.github.darvld.krpc.compiler.resolveAsTypeName
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.TypeName
import io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING

/**Contains information about a service method annotated with [ClientStream].*/
class ClientStreamMethod(
    declaredName: String,
    methodName: String,
    requestName: String,
    requestType: TypeName,
    responseType: TypeName
) : ServiceMethodDefinition(
    declaredName,
    methodName,
    requestName,
    isSuspending = true,
    methodType = CLIENT_STREAMING,
    requestType, responseType
) {
    companion object {
        /**The simple name of the [ClientStream] annotation.*/
        val AnnotationName = ClientStream::class.simpleName!!

        /**Extracts a [ClientStreamMethod] from a function [declaration] given the corresponding [ClientStream] annotation.*/
        fun extractFrom(declaration: KSFunctionDeclaration, annotation: KSAnnotation): ClientStreamMethod {
            declaration.requireSuspending(true, "ClientStream rpc methods must be marked with 'suspend' modifier.")

            val methodName = declaration.extractMethodName(annotation)
            val responseType = declaration.returnType?.resolveAsTypeName() ?: UnitClassName

            val (requestName, requestType) = declaration.extractRequestInfo { reference ->
                // Resolve the request type, which should be a Flow<T>, and extract the 'T' type name.
                reference.resolveAsParameterizedName()?.typeArguments?.singleOrNull()
            } ?: reportError(
                declaration,
                message = "ClientStream rpc methods must declare a Flow of a serializable type as the only parameter."
            )

            return ClientStreamMethod(
                declaredName = declaration.simpleName.asString(),
                methodName,
                requestName,
                requestType,
                responseType
            )
        }
    }
}