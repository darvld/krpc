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

import com.github.darvld.krpc.compiler.reportError
import com.github.darvld.krpc.compiler.resolveAsParameterizedName
import com.github.darvld.krpc.compiler.resolveAsTypeName
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT

/**An abstraction used to handle different types of rpc method requests:
 *  - [SimpleRequest] handles methods with a single argument (the default in gRPC).
 *  - [CompositeRequest] handles methods with multiple arguments (unary and server-stream methods only).
 *  - [NoRequest] handles methods without arguments (unary and server-stream methods only).
 *
 *  Use [RequestInfo.extractFrom] to automatically extract the correct information from a method declaration.*/
sealed interface RequestInfo {
    companion object {
        /**Extract the request information from a method declaration.
         *
         * When [flowExpected] is true, this method will look for a single parameter with type Flow<T>, and extract
         * the T type argument as the request type.*/
        fun extractFrom(declaration: KSFunctionDeclaration, flowExpected: Boolean = false): RequestInfo {
            return if (declaration.parameters.size > 1) {
                if (flowExpected) {
                    reportError(
                        declaration,
                        "Multiple arguments are not supported for methods using client-side streaming."
                    )
                }

                // Create a composite request by matching every argument's name with its resolved type
                val params = declaration.parameters.associate { param ->
                    param.name!!.asString() to param.type.resolveAsTypeName()
                }

                val wrapperName = declaration.simpleName.asString().replaceFirstChar { it.uppercase() } + "Request"
                CompositeRequest(params, wrapperName)
            } else declaration.parameters.singleOrNull()?.let {
                // If the single parameter should be a Flow, extract the argument from the Flow<T> declaration
                val resolvedType = if (flowExpected) {
                    it.type.resolveAsParameterizedName()?.typeArguments?.singleOrNull()
                        ?: reportError(declaration, "Expected a single Flow<T> argument.")
                } else {
                    it.type.resolveAsTypeName()
                }

                SimpleRequest(it.name!!.asString(), resolvedType)
            } ?: if (!flowExpected) NoRequest else reportError(
                declaration,
                "No arguments provided (expected a single Flow<T>)."
            )
        }

        /**Returns a [TypeName] used by the component generators to specify the rpc method response type.*/
        fun ServiceDefinition.requestTypeFor(method: ServiceMethodDefinition): TypeName {
            return when (method.request) {
                is CompositeRequest -> {
                    ClassName(packageName, descriptorName, method.request.wrapperName)
                }
                is SimpleRequest -> method.request.type
                NoRequest -> UNIT
            }
        }
    }
}

/**Represents a single-parameter request. Compatible with all method types.*/
data class SimpleRequest(val parameterName: String, val type: TypeName) : RequestInfo

/**Represents a request with multiple parameters. Only available for unary and server-stream methods.*/
data class CompositeRequest(val parameters: Map<String, TypeName>, val wrapperName: String) : RequestInfo

/**Represents a request without parameters. Only available for unary and server-stream methods.*/
object NoRequest : RequestInfo