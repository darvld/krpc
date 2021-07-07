package com.github.darvld.krpc.compiler.generators

import com.github.darvld.krpc.compiler.UnitClassName
import com.github.darvld.krpc.compiler.markAsGenerated
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.grpc.MethodDescriptor

internal fun TypeSpec.Builder.getOrAddMarshaller(typeName: TypeName): String {
    // Don't generate a marshaller for Unit
    if (typeName == UnitClassName) return "SerializationProvider.UnitMarshaller"
    
    val propName = when (typeName) {
        is ClassName -> typeName.marshallerPropName
        is ParameterizedTypeName -> typeName.rawType.marshallerPropName
        else -> throw IllegalStateException("Unable to generate marshaller for type $typeName")
    }
    
    // Avoid re-generating the same marshaller
    propertySpecs.find { it.name == propName }?.let { return propName }
    
    addProperty(buildMarshaller(propName, typeName))
    return propName
}

internal fun buildMarshaller(name: String, type: TypeName): PropertySpec {
    val marshallerType = MethodDescriptor.Marshaller::class.asTypeName()
        .parameterizedBy(type)
    
    return PropertySpec.builder(name, marshallerType, KModifier.PRIVATE)
        .markAsGenerated()
        .addKdoc(
            """
            |A generated [MethodDescriptor.Marshaller] obtained using the `serializationProvider` constructor parameter.
            |""".trimMargin(),
        )
        .mutable(false)
        .initializer(
            "$SERIALIZATION_PROVIDER_PARAM.marshallerFor(%M())",
            MemberName("kotlinx.serialization", "serializer")
        )
        .build()
}