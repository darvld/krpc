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

@CompilerDsl
inline fun TypeSpec.Builder.constructor(
    primary: Boolean = false,
    block: FunSpec.Builder.() -> Unit = {}
): FunSpec {
    val impl = FunSpec.constructorBuilder().apply(block).build()

    if (primary) primaryConstructor(impl) else addFunction(impl)

    return impl
}

@CompilerDsl
inline fun TypeSpec.Builder.function(
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

@CompilerDsl
inline fun TypeSpec.Builder.property(
    name: String,
    type: TypeName,
    vararg modifiers: KModifier,
    block: PropertySpec.Builder.() -> Unit = {}
): PropertySpec {
    val prop = PropertySpec.builder(name, type, *modifiers).apply(block).build()

    addProperty(prop)
    return prop
}