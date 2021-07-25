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

package io.github.darvld.krpc.compiler.dsl

import com.squareup.kotlinpoet.*

inline fun buildClass(name: String, block: TypeSpec.Builder.() -> Unit): TypeSpec {
    return TypeSpec.classBuilder(name).apply(block).build()
}

inline fun TypeSpec.Builder.addConstructor(
    primary: Boolean = false,
    block: FunSpec.Builder.() -> Unit = {}
): FunSpec {
    val impl = FunSpec.constructorBuilder().apply(block).build()

    if (primary) primaryConstructor(impl) else addFunction(impl)

    return impl
}

inline fun TypeSpec.Builder.addFunction(
    name: String,
    vararg modifiers: KModifier,
    block: FunSpec.Builder.() -> Unit = {}
): FunSpec {
    val impl = FunSpec.builder(name).apply {
        addModifiers(modifiers.asIterable())
        block()
    }.build()

    addFunction(impl)
    return impl
}

inline fun TypeSpec.Builder.addProperty(
    name: String,
    type: TypeName,
    vararg modifiers: KModifier,
    block: PropertySpec.Builder.() -> Unit = {}
): PropertySpec {
    val prop = PropertySpec.builder(name, type, *modifiers).apply(block).build()

    addProperty(prop)
    return prop
}