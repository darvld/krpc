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

package io.github.darvld.krpc.compiler.model

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import io.github.darvld.krpc.MethodType
import io.github.darvld.krpc.compiler.FLOW

/**Model class used by [ServiceProcessor][io.github.darvld.krpc.compiler.ServiceProcessor] to store information
 * about individual service methods.
 *
 * @see [ServiceDefinition]*/
data class ServiceMethodDefinition(
    /**The name of the method as declared in the service interface.*/
    val declaredName: String,
    /**The simple (unqualified) gRPC name of this method.*/
    val methodName: String,
    /**Whether the method must be marked with the 'suspend' modifier.*/
    val isSuspending: Boolean,
    /**The rpc type of this method.*/
    val methodType: MethodType,
    /**The definition of this method's request. Could be a [SimpleRequest] (single argument)
     *  or a [CompositeRequest] (multiple arguments).
     *
     *  For client-streaming and bidi-streaming methods, this will be the Flow's type argument.*/
    val request: RequestInfo,
    /**The type of the method's *response*. For server-streaming and bidi-streaming methods, this will be
     * the Flow's type argument.
     *
     * This is not the method's return type, see [returnType] instead for a [TypeName] with Flow wrappers.*/
    val responseType: TypeName
) {
    /**The return type of this method (wrapped in Flow for server-stream and bidi-stream).*/
    val returnType: TypeName
        get() = if (methodType.serverSendsOneMessage()) {
            responseType
        } else {
            FLOW.parameterizedBy(responseType)
        }
}
