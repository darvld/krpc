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
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.TypeName
import io.grpc.MethodDescriptor

/**Model class used by [ServiceProcessor][com.github.darvld.krpc.compiler.ServiceProcessor] to store information
 * about individual service methods.
 *
 * @see [ServiceDefinition]*/
sealed class ServiceMethodDefinition(
    /**The name of the method as declared in the service interface.*/
    val declaredName: String,
    /**The "official" GRPC name of this method.*/
    val methodName: String,
    /**Whether the method needs to be marked with the 'suspend' modifier.*/
    val isSuspending: Boolean,
    /**The rpc type of this method.*/
    val methodType: MethodDescriptor.MethodType,
    /**The definition of this method's request. Could be a [SimpleRequest] (single argument)
     *  or a [CompositeRequest] (multiple arguments).*/
    val request: RequestInfo,
    /**Return type of the method.*/
    val responseType: TypeName
) {

    /**Returns the full gRPC name for this method, consisting of the name of the service and the name of the method itself.*/
    fun qualifiedName(serviceName: String): String {
        return "$serviceName/$methodName"
    }

    companion object {
        /**Checks that this declaration is marked with the 'suspend' modifier.*/
        fun KSFunctionDeclaration.requireSuspending(required: Boolean, message: String) {
            if (required && Modifier.SUSPEND !in modifiers)
                reportError(this, message)
            else if (!required && Modifier.SUSPEND in modifiers)
                reportError(this, message)
        }

        /**Extracts the name of a service method given its declaration and the corresponding [annotation].
         *
         * The method name defined through annotation parameters will be used if present, otherwise the declared
         * name will be used.*/
        fun KSFunctionDeclaration.extractMethodName(annotation: KSAnnotation): String {
            return annotation.arguments.first().value?.toString()?.takeUnless { it.isBlank() }
                ?: simpleName.asString()
        }
    }
}
