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
@file:Suppress("nothing_to_inline")

package io.github.darvld.krpc.compiler.dsl

import com.squareup.kotlinpoet.CodeBlock

@CompilerDsl
inline fun buildCode(block: CodeBlock.Builder.() -> Unit): CodeBlock {
    return CodeBlock.builder().apply(block).build()
}

@CompilerDsl
inline fun buildCode(format: String, vararg args: Any): CodeBlock {
    return CodeBlock.of(format, args)
}