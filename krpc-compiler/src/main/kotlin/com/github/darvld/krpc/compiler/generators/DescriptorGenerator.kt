package com.github.darvld.krpc.compiler.generators

import com.github.darvld.krpc.SerializationProvider
import com.github.darvld.krpc.compiler.addClass
import com.github.darvld.krpc.compiler.buildFile
import com.github.darvld.krpc.compiler.markAsGenerated
import com.github.darvld.krpc.compiler.model.ServiceDefinition
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.grpc.MethodDescriptor
import java.io.OutputStream

internal class DescriptorGenerator : ServiceComponentGenerator {
    override fun generate(codeGenerator: CodeGenerator, definition: ServiceDefinition) {
        codeGenerator.createNewFile(
            Dependencies(true),
            definition.packageName,
            definition.descriptorName
        ).use { stream ->
            generateServiceDescriptor(stream, definition)
        }
    }

    fun generateServiceDescriptor(output: OutputStream, service: ServiceDefinition) {
        buildFile(service.packageName, service.descriptorName, output) {
            addClass {
                addModifiers(KModifier.INTERNAL)
                markAsGenerated()

                addKdoc(
                    "Internal helper class generated by the Krpc compiler," +
                            " This class is intended to be used only by generated declarations and should not" +
                            " be used in general code.\n\n" +
                            "@param $SERIALIZATION_PROVIDER_PARAM A provider implementing a serialization format." +
                            " Used to generate marshallers for rpc methods."
                )

                // Constructor with serialization provider as parameter (used to initialized method descriptors)
                primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter(SERIALIZATION_PROVIDER_PARAM, SerializationProvider::class)
                        .build()
                )

                // Necessary for the `serializer` calls
                addImport("kotlinx.serialization", "serializer")

                // Generate helper method definitions
                for (method in service.methods) {
                    val descriptor = buildMethodDescriptor(
                        method.declaredName,
                        method.methodName,
                        method.methodType,
                        service.serviceName,
                        method.requestType,
                        method.responseType
                    )
                    addProperty(descriptor)
                }
            }
        }
    }

    fun buildMethodDescriptor(
        name: String,
        methodName: String,
        methodType: MethodDescriptor.MethodType,
        serviceName: String,
        requestType: TypeName,
        responseType: TypeName,
    ): PropertySpec {
        /*.addKdoc(
            """
            A generated [MethodDescriptor] for the [%L] method.

            This descriptor is used by generated client and server implementations. It should not be
            used in general code.
            """.trimIndent(),
            "${service.declaredName}.${method.declaredName}"
        )*/
        return PropertySpec.builder(
            name,
            MethodDescriptor::class.asTypeName().parameterizedBy(requestType, responseType)
        ).run {
            markAsGenerated()
            mutable(false)
            initializer(
                CodeBlock.builder()
                    .addStatement("MethodDescriptor")
                    .addStatement("  .newBuilder<%T, %T>()", requestType, responseType)
                    .addStatement("  .setFullMethodName(%S)", "$serviceName/$methodName")
                    .addStatement("  .setType(%L)", "MethodDescriptor.MethodType.${methodType.name}")
                    .addStatement("  .setRequestMarshaller(%L)", requestType.marshallerPropName)
                    .addStatement("  .setResponseMarshaller(%L)", responseType.marshallerPropName)
                    .addStatement("  .build()")
                    .build()
            )
            build()
        }
    }

    companion object {
        const val SERIALIZATION_PROVIDER_PARAM = "serializationProvider"
    }
}