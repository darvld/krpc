package com.github.darvld.krpc.compiler.generators

import com.github.darvld.krpc.SerializationProvider
import com.github.darvld.krpc.compiler.asClassName
import com.github.darvld.krpc.compiler.markAsGenerated
import com.github.darvld.krpc.compiler.model.ServiceDefinition
import com.github.darvld.krpc.compiler.model.ServiceMethodDefinition
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.grpc.MethodDescriptor
import java.io.OutputStream

/**The name of the [SerializationProvider] parameter used in the constructor and as a property.*/
const val SERIALIZATION_PROVIDER_PARAM = "serializationProvider"

/**The name of the generated marshaller for this type.*/
private inline val ClassName.marshallerPropName: String
    get() = "${simpleName.replaceFirstChar { it.lowercaseChar() }}Marshaller"

/**Generate a helper class containing descriptors and marshallers required to implement a [service]. The class is
 * written to a file through [output].
 *
 * @see generateServiceProviderBase
 * @see generateClientImplementation*/
fun generateDescriptorContainer(output: OutputStream, service: ServiceDefinition) {
    FileSpec.builder(service.packageName, service.descriptorName).apply {
        // Helper class definition
        val descriptorContainer = TypeSpec.classBuilder(service.descriptorName).apply {
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
            service.methods.forEach {
                addServiceMethodDescriptor(it)
            }
        }.build()

        addType(descriptorContainer)
    }.build().let { spec ->
        output.writer().use(spec::writeTo)
    }
}

/**Add a marshaller implementation for the given [typeName] to the descriptor container, using the serializationProvider.*/
private fun TypeSpec.Builder.addMarshaller(typeName: ClassName): PropertySpec {
    val propName = typeName.marshallerPropName

    // Avoid re-generating the same marshaller
    propertySpecs.find { it.name == propName }?.let { return it }

    val marshallerType = MethodDescriptor.Marshaller::class.asTypeName()
        .parameterizedBy(typeName)

    return PropertySpec.builder(propName, marshallerType, KModifier.PRIVATE)
        .markAsGenerated()
        .addKdoc(
            """
            |A generated [MethodDescriptor.Marshaller] obtained using the serializationProvider constructor param.
            |""".trimMargin(),
        )
        .mutable(false)
        .initializer("$SERIALIZATION_PROVIDER_PARAM.marshallerFor(serializer())")
        .build()
        .also(::addProperty)
}

/**Add a method descriptor to the container.*/
private fun TypeSpec.Builder.addServiceMethodDescriptor(
    definition: ServiceMethodDefinition
) {
    val requestType = definition.request.type.resolve().asClassName()
    val responseType = (definition.returnType!!.resolve()).asClassName()

    val type = MethodDescriptor::class.asTypeName()
        .parameterizedBy(requestType, responseType)

    PropertySpec.builder(definition.declaredName, type)
        .addKdoc(
            """
            |A generated [MethodDescriptor] for the [%L] service method.
            |
            |This descriptor is used by generated client and server implementations. It should not be
            |used in general code.
            |""".trimMargin(),
            definition.declaredName
        )
        .markAsGenerated()
        .mutable(false)
        .initializer(
            """
                |MethodDescriptor.newBuilder<%T,%T>()
                |    .setFullMethodName(%S)
                |    .setType(%L)
                |    .setRequestMarshaller(%N)
                |    .setResponseMarshaller(%N)
                |    .build()
                |""".trimMargin(),
            requestType,
            responseType,
            definition.methodName, // The "official" name for this method in the GRPC definition
            "MethodDescriptor.MethodType.${definition.methodType.name}", // The appropriate enum entry
            addMarshaller(requestType),
            addMarshaller(responseType)
        )
        .build()
        .let(::addProperty)
}