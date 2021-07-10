package com.github.darvld.krpc.compiler.generators

import com.github.darvld.krpc.SerializationProvider
import com.github.darvld.krpc.compiler.UnitClassName
import com.github.darvld.krpc.compiler.addClass
import com.github.darvld.krpc.compiler.buildFile
import com.github.darvld.krpc.compiler.generators.DescriptorGenerator.Companion.SERIALIZATION_PROVIDER_PARAM
import com.github.darvld.krpc.compiler.markAsGenerated
import com.github.darvld.krpc.compiler.model.ServiceDefinition
import com.github.darvld.krpc.compiler.model.ServiceMethodDefinition
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.squareup.kotlinpoet.*
import io.grpc.MethodDescriptor.MethodType.*
import io.grpc.ServerServiceDefinition
import io.grpc.kotlin.AbstractCoroutineServerImpl
import io.grpc.kotlin.ServerCalls
import java.io.OutputStream
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal class ServiceProviderGenerator : ServiceComponentGenerator {

    override fun generate(codeGenerator: CodeGenerator, definition: ServiceDefinition) {
        codeGenerator.createNewFile(
            Dependencies(true),
            definition.packageName,
            definition.providerName
        ).use { stream ->
            generateServiceProviderBase(stream, definition)
        }
    }

    fun generateServiceProviderBase(output: OutputStream, service: ServiceDefinition) {
        buildFile(service.packageName, service.providerName, output) {
            addClass {
                markAsGenerated()

                addModifiers(KModifier.ABSTRACT)
                superclass(AbstractCoroutineServerImpl::class)

                // Primary constructor
                FunSpec.constructorBuilder()
                    .addParameter(SERIALIZATION_PROVIDER_PARAM, SerializationProvider::class)
                    .addParameter(
                        ParameterSpec.builder("context", CoroutineContext::class)
                            .defaultValue("%T", EmptyCoroutineContext::class)
                            .build()
                    )
                    .build()
                    .let(::primaryConstructor)

                // Pass the coroutine context to the parent class
                addSuperclassConstructorParameter("context")

                // Implement the service interface
                addSuperinterface(service.className)

                // The method definitions are pulled from here
                PropertySpec.builder("definitions", service.descriptorClassName)
                    .addModifiers(KModifier.PRIVATE)
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
        FunSpec.builder("bindService")
            .markAsGenerated()
            .addModifiers(KModifier.FINAL, KModifier.OVERRIDE)
            .addCode(
                CodeBlock.builder()
                    .beginControlFlow("return run {")
                    .addStatement("ServerServiceDefinition.builder(%S)", service.serviceName)
                    .add(buildServiceBinderFor(service.methods))
                    .addStatement("    .build()")
                    .endControlFlow()
                    .build()
            )
            .returns(ServerServiceDefinition::class)
            .build()
            .let(::addFunction)
    }

    private fun buildServiceBinderFor(methods: List<ServiceMethodDefinition>): CodeBlock {
        // Cache this so it isn't resolved in every iteration
        val serverCalls = ServerCalls::class

        return methods.fold(CodeBlock.builder()) { block, method ->
            val builder = when (method.methodType) {
                UNARY -> "unary"
                CLIENT_STREAMING -> "clientStreaming"
                SERVER_STREAMING -> "serverStreaming"
                BIDI_STREAMING -> "bidiStreaming"
                UNKNOWN -> throw IllegalStateException("Method type cannot be UNKNOWN")
            }

            val implementation = if (method.requestType == UnitClassName) {
                "implementation = { ${method.declaredName}() }"
            } else {
                "::${method.declaredName}"
            }

            /*
            Note: the indentation inside the raw string below *looks* wrong, but when used by KotlinPoet,
            it will generate code with the correct format. Please don't touch it.
            It should generate something like this:
            ```
            .addMethod(
                ServerCalls.unaryServerMethodDefinition(
                  context,
                  definitions.unaryCall,
                  ::unaryCall
                )
            )
            ```
             */
            block.addStatement(
                """
                .addMethod(
              %T.${builder}ServerMethodDefinition(
                context,
                definitions.${method.declaredName},
                $implementation
              )
            )
                """.trimIndent(),
                serverCalls,
            )
        }.build()
    }
}