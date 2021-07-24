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

package io.github.darvld.krpc.compiler.generators

import com.squareup.kotlinpoet.KModifier.*
import io.github.darvld.krpc.AbstractServiceProvider
import io.github.darvld.krpc.ServiceRegistrar
import io.github.darvld.krpc.compiler.COROUTINE_CONTEXT
import io.github.darvld.krpc.compiler.COROUTINE_CONTEXT_PARAM
import io.github.darvld.krpc.compiler.DESCRIPTOR_PROPERTY
import io.github.darvld.krpc.compiler.SERIALIZATION_PROVIDER
import io.github.darvld.krpc.compiler.SERIALIZATION_PROVIDER_PARAM
import io.github.darvld.krpc.compiler.dsl.*
import io.github.darvld.krpc.compiler.model.CompositeRequest
import io.github.darvld.krpc.compiler.model.NoRequest
import io.github.darvld.krpc.compiler.model.ServiceDefinition
import io.github.darvld.krpc.compiler.model.SimpleRequest
import io.grpc.MethodDescriptor.MethodType.*
import java.io.OutputStream

internal class ServiceProviderGenerator : ServiceComponentGenerator() {

    override fun getFilename(service: ServiceDefinition): String {
        return service.providerName
    }

    override fun generateComponent(output: OutputStream, service: ServiceDefinition) {
        buildFile(service.packageName, service.providerName, output) {
            addClass {
                markAsGenerated()
                addModifiers(ABSTRACT)

                addSuperinterface(service.className)
                superclass(AbstractServiceProvider::class)

                addKdoc(
                    "Generated [%T] provider. Subclass this stub and override the service methods" +
                            " to provide your implementation of the service.",
                    service.className,
                )

                // Primary constructor
                constructor(primary = true) {
                    parameter(SERIALIZATION_PROVIDER_PARAM, SERIALIZATION_PROVIDER)
                    parameter(COROUTINE_CONTEXT_PARAM, COROUTINE_CONTEXT)
                }

                // Pass the coroutine context to the parent class
                addSuperclassConstructorParameter(COROUTINE_CONTEXT_PARAM)

                // The method definitions are pulled from here
                property(DESCRIPTOR_PROPERTY, service.descriptorClassName, PROTECTED, FINAL, OVERRIDE) {
                    initializer("%T($SERIALIZATION_PROVIDER_PARAM)", service.descriptorClassName)
                }

                // Override bindService
                function(AbstractServiceProvider::bindService.name, FINAL, OVERRIDE) {
                    markAsGenerated()
                    receiver(ServiceRegistrar::class)

                    code {
                        for (method in service.methods) {
                            val builder = when (method.methodType) {
                                UNARY -> "Unary"
                                CLIENT_STREAMING -> "ClientStream"
                                SERVER_STREAMING -> "ServerStream"
                                BIDI_STREAMING -> "BidiStream"
                                UNKNOWN -> throw IllegalStateException("Method type cannot be UNKNOWN")
                            }

                            val implementation = when (method.request) {
                                is CompositeRequest -> {
                                    val arguments = method.request.parameters.keys.joinToString { arg -> "it.$arg" }
                                    ") { ${method.declaredName}($arguments) }"
                                }
                                is SimpleRequest -> {
                                    ", ::${method.declaredName})"
                                }
                                NoRequest -> {
                                    ") { ${method.declaredName}() }"
                                }
                            }
                            addStatement("register${builder}Method(definition.${method.declaredName}$implementation")
                        }
                    }
                }
            }
        }
    }
}