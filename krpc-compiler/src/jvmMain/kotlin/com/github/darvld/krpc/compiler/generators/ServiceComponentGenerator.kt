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

import com.github.darvld.krpc.compiler.FlowClassName
import com.github.darvld.krpc.compiler.model.BidiStreamMethod
import com.github.darvld.krpc.compiler.model.ServerStreamMethod
import com.github.darvld.krpc.compiler.model.ServiceDefinition
import com.github.darvld.krpc.compiler.model.ServiceMethodDefinition
import com.google.devtools.ksp.processing.CodeGenerator
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName

internal fun interface ServiceComponentGenerator {
    fun generate(codeGenerator: CodeGenerator, definition: ServiceDefinition)

    companion object {
        val ServiceMethodDefinition.returnType: TypeName
            get() = if (this is ServerStreamMethod || this is BidiStreamMethod) {
                FlowClassName.parameterizedBy(responseType)
            } else {
                responseType
            }
    }
}