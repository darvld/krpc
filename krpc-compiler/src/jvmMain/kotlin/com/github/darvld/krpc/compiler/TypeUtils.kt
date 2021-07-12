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

package com.github.darvld.krpc.compiler

import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.coroutines.flow.Flow

internal val FLOW by lazy { Flow::class.asClassName() }

/**Resolves this type reference and attempts construct a [ParameterizedTypeName].*/
internal fun KSTypeReference.resolveAsParameterizedName(): ParameterizedTypeName? {
    return resolveAsTypeName() as? ParameterizedTypeName
}

/**Resolves this ksp [KSTypeReference] as a kotlinPoet [TypeName].
 *
 * For non-generic types, this method returns a simple [ClassName]. For generics, it recursively resolves
 * type arguments.*/
internal fun KSTypeReference.resolveAsTypeName(): TypeName = with(resolve()) {
    val baseName = ClassName(declaration.packageName.asString(), declaration.simpleName.asString())

    if (arguments.isEmpty()) return baseName

    return baseName.parameterizedBy(arguments.map { it.type?.resolveAsTypeName() ?: STAR })
}