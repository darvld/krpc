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

import com.github.darvld.krpc.UnaryCall
import com.github.darvld.krpc.compiler.resolveAsTypeName
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import io.grpc.MethodDescriptor.MethodType.UNARY

/**Contains information about a service method annotated with [UnaryCall].*/
class UnaryMethod(
    declaredName: String,
    methodName: String,
    request: RequestInfo,
    responseType: TypeName
) : ServiceMethodDefinition(
    declaredName,
    methodName,
    isSuspending = true,
    methodType = UNARY,
    request,
    responseType
) {
    companion object {
        /**The simple name of the [UnaryCall] annotation.*/
        val AnnotationName = UnaryCall::class.simpleName!!

        /**Extracts a [UnaryMethod] from a function [declaration] given the corresponding [UnaryCall] annotation.*/
        fun extractFrom(declaration: KSFunctionDeclaration, annotation: KSAnnotation): UnaryMethod {
            declaration.requireSuspending(true, "UnaryCall rpc methods must be marked with 'suspend' modifier.")

            return UnaryMethod(
                declaredName = declaration.simpleName.asString(),
                methodName = declaration.extractMethodName(annotation),
                request = RequestInfo.extractFrom(declaration),
                responseType = declaration.returnType?.resolveAsTypeName() ?: UNIT
            )
        }
    }
}