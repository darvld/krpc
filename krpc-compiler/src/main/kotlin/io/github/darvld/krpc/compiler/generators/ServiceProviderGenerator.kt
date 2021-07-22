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

import com.squareup.kotlinpoet.*
import io.github.darvld.krpc.AbstractServiceProvider
import io.github.darvld.krpc.SerializationProvider
import io.github.darvld.krpc.ServiceRegistrar
import io.github.darvld.krpc.compiler.addClass
import io.github.darvld.krpc.compiler.buildFile
import io.github.darvld.krpc.compiler.generators.DescriptorGenerator.Companion.SERIALIZATION_PROVIDER_PARAM
import io.github.darvld.krpc.compiler.markAsGenerated
import io.github.darvld.krpc.compiler.model.CompositeRequest
import io.github.darvld.krpc.compiler.model.NoRequest
import io.github.darvld.krpc.compiler.model.ServiceDefinition
import io.github.darvld.krpc.compiler.model.SimpleRequest
import io.grpc.MethodDescriptor.MethodType.*
import java.io.OutputStream
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal class ServiceProviderGenerator : ServiceComponentGenerator() {

    override fun getFilename(service: ServiceDefinition): String {
        return service.providerName
    }

    override fun generateComponent(output: OutputStream, service: ServiceDefinition) {
        buildFile(service.packageName, service.providerName, output) {
            addClass {
                markAsGenerated()

                addModifiers(KModifier.ABSTRACT)
                superclass(AbstractServiceProvider::class)

                addKdoc(
                    "Generated [%T] provider. Subclass this stub and override the service methods" +
                            " to provide your implementation of the service.",
                    service.className,
                )

                // Primary constructor
                FunSpec.constructorBuilder().run {
                    ParameterSpec.builder(SERIALIZATION_PROVIDER_PARAM, SerializationProvider::class)
                        .build()
                        .let(::addParameter)

                    ParameterSpec.builder(COROUTINE_CONTEXT_PARAM, CoroutineContext::class)
                        .defaultValue("%T", EmptyCoroutineContext::class)
                        .build()
                        .let(::addParameter)
                    build()
                }.let(::primaryConstructor)


                // Pass the coroutine context to the parent class
                addSuperclassConstructorParameter(COROUTINE_CONTEXT_PARAM)

                // Implement the service interface
                addSuperinterface(service.className)

                // The method definitions are pulled from here
                PropertySpec.builder("definition", service.descriptorClassName)
                    .addModifiers(KModifier.PROTECTED, KModifier.OVERRIDE, KModifier.FINAL)
                    .mutable(false)
                    .initializer("%T($SERIALIZATION_PROVIDER_PARAM)", service.descriptorClassName)
                    .build()
                    .let(::addProperty)

                // The `bindService` implementation
                overrideServiceBinder(service)
            }
        }
    }

    private fun TypeSpec.Builder.overrideServiceBinder(service: ServiceDefinition) {
        FunSpec.builder("bindMethods")
            .markAsGenerated()
            .addModifiers(KModifier.FINAL, KModifier.OVERRIDE)
            .receiver(ServiceRegistrar::class)
            .addCode(
                CodeBlock.builder().add(service.methods.fold(CodeBlock.builder()) { block, method ->
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

                    block.addStatement("register${builder}Method(definition.${method.declaredName}$implementation")
                }.build())
                    .build()
            )
            .build()
            .let(::addFunction)
    }

    private companion object {
        const val COROUTINE_CONTEXT_PARAM = "context"
    }
}