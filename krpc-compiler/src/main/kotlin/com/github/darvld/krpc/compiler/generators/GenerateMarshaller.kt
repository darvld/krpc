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

package com.github.darvld.krpc.compiler.generators

import com.github.darvld.krpc.compiler.UnitClassName
import com.github.darvld.krpc.compiler.generators.DescriptorGenerator.Companion.SERIALIZATION_PROVIDER_PARAM
import com.github.darvld.krpc.compiler.markAsGenerated
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.grpc.MethodDescriptor

internal val TypeName.uniqueSimpleName: String
    get() {
        return when (this) {
            is ClassName -> simpleName
            is ParameterizedTypeName -> {
                typeArguments.joinToString("") { it.uniqueSimpleName } + rawType.simpleName
            }
            else -> throw UnsupportedOperationException()
        }
    }

internal val TypeName.marshallerPropName: String
    get() = if (this == UnitClassName) {
        "SerializationProvider.UnitMarshaller"
    } else {
        "${uniqueSimpleName.replaceFirstChar { it.lowercaseChar() }}Marshaller"
    }

internal fun TypeSpec.Builder.addMarshaller(typeName: TypeName) {
    // Don't generate a marshaller for Unit
    if (typeName == UnitClassName) return

    val propName = typeName.marshallerPropName

    // Avoid re-generating the same marshaller
    propertySpecs.find { it.name == propName }?.let { return }

    addProperty(buildMarshaller(typeName, propName))
}

internal fun buildMarshaller(type: TypeName, name: String = type.marshallerPropName): PropertySpec {
    val marshallerType = MethodDescriptor.Marshaller::class.asTypeName()
        .parameterizedBy(type)

    return PropertySpec.builder(name, marshallerType, KModifier.PRIVATE)
        .markAsGenerated()
        .mutable(false)
        .initializer(
            "$SERIALIZATION_PROVIDER_PARAM.marshallerFor(%M())",
            MemberName("kotlinx.serialization", "serializer")
        )
        .build()
}