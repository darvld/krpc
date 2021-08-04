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

package io.github.darvld.krpc.compiler

import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Nullability
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName


/**Resolves this type reference and attempts construct a [ParameterizedTypeName].*/
fun KSTypeReference.resolveAsParameterizedName(): ParameterizedTypeName? {
    return resolveAsTypeName() as? ParameterizedTypeName
}


/**Resolves this ksp [KSTypeReference] as a kotlinPoet [TypeName].
 *
 * For non-generic types, this method returns a simple [ClassName]. For generics, it recursively resolves
 * type arguments.*/
fun KSTypeReference.resolveAsTypeName(): TypeName {
    with(resolve()) {

        val baseName = ClassName(
            declaration.packageName.asString(),
            *declaration.getQualifiedName().split(".").toTypedArray()
        )

        val name = if (arguments.isEmpty()) {
            baseName
        } else {
            baseName.parameterizedBy(arguments.map { it.type?.resolveAsTypeName() ?: STAR })
        }

        return name.copy(nullable = nullability == Nullability.NULLABLE)
    }
}

private fun KSDeclaration.getQualifiedName(): String {
    val name = simpleName.asString()

    return parentDeclaration?.let {
        val parents = it.getQualifiedName()
        "$parents.$name"
    } ?: name
}