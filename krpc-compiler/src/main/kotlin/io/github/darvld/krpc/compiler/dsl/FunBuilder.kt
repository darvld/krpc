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

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName

inline fun buildFunction(name: String, block: FunSpec.Builder.() -> Unit): FunSpec {
    return FunSpec.builder(name).apply(block).build()
}

inline fun FunSpec.Builder.addParameter(
    name: String,
    returns: TypeName,
    default: CodeBlock,
    block: ParameterSpec.Builder .() -> Unit = {}
): ParameterSpec {
    val param = ParameterSpec.builder(name, returns).apply {
        defaultValue(default)
        block()
    }.build()

    addParameter(param)
    return param
}

inline fun FunSpec.Builder.addCode(block: CodeBlock.Builder.() -> Unit): CodeBlock {
    return CodeBlock.builder().apply(block).build().also {
        addCode(it)
    }
}