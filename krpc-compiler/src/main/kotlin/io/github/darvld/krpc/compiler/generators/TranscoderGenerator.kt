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
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.darvld.krpc.compiler.generators.DescriptorGenerator.Companion.SERIALIZATION_PROVIDER_PARAM
import io.github.darvld.krpc.compiler.generators.DescriptorGenerator.Companion.TRANSCODER
import io.github.darvld.krpc.compiler.generators.DescriptorGenerator.Companion.TRANSCODER_EXTENSION_MEMBER
import io.github.darvld.krpc.compiler.markAsGenerated

private val TypeName.uniqueSimpleName: String
    get() {
        return when (this) {
            is ClassName -> simpleName
            is ParameterizedTypeName -> {
                typeArguments.joinToString("") { it.uniqueSimpleName } + rawType.simpleName
            }
            else -> throw UnsupportedOperationException()
        }
    }

internal val TypeName.transcoderName: String
    get() = if (this == UNIT) {
        "UnitTranscoder"
    } else {
        "${uniqueSimpleName.replaceFirstChar { it.lowercaseChar() }}Transcoder"
    }

internal fun TypeSpec.Builder.addTranscoder(typeName: TypeName) {
    // Don't generate a marshaller for Unit
    if (typeName == UNIT) return

    val propName = typeName.transcoderName

    // Avoid re-generating the same marshaller
    propertySpecs.find { it.name == propName }?.let { return }

    addProperty(
        PropertySpec.builder(propName, TRANSCODER.parameterizedBy(typeName))
            .addModifiers(KModifier.PRIVATE)
            .markAsGenerated()
            .mutable(false)
            .initializer("$SERIALIZATION_PROVIDER_PARAM.%M()", TRANSCODER_EXTENSION_MEMBER)
            .build()
    )
}
