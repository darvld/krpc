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

import com.github.darvld.krpc.compiler.buildFile
import com.github.darvld.krpc.compiler.model.BidiStreamMethod
import com.github.darvld.krpc.compiler.model.ServiceDefinition
import com.github.darvld.krpc.compiler.model.ServiceMethodDefinition
import com.github.darvld.krpc.compiler.model.UnaryMethod
import com.github.darvld.krpc.compiler.testing.ClassNames
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

abstract class CodeGenerationTest {

    @Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()

    protected inline fun TemporaryFolder.newObject(
        name: String,
        block: TypeSpec.Builder.() -> Unit
    ): File {
        val file = newFile("$name.kt")
        file.outputStream().use { stream ->
            buildFile("com.test.generated", name, stream) {
                TypeSpec.objectBuilder(name).apply(block).build().also(::addType)
            }
        }
        return file
    }

    companion object {


        fun serviceDefinition(
            declaredName: String = "TestService",
            packageName: String = "com.test.generated",
            serviceName: String = declaredName,
            clientName: String = "TestClient",
            providerName: String = "TestServiceProvider",
            methods: List<ServiceMethodDefinition> = emptyList()
        ): ServiceDefinition = ServiceDefinition(
            declaredName,
            packageName,
            serviceName,
            clientName,
            providerName,
            methods
        )

        fun unaryMethod(
            declaredName: String = "unary",
            methodName: String = "${declaredName}Test",
            requestName: String = "request",
            requestType: TypeName = ClassNames.Int,
            returnType: TypeName = ClassNames.String
        ): UnaryMethod = UnaryMethod(
            declaredName,
            methodName,
            requestName,
            requestType,
            returnType
        )

        fun bidiStreamMethod(
            declaredName: String = "bidiStream",
            methodName: String = "${declaredName}Test",
            requestName: String = "request",
            requestType: TypeName = ClassNames.Int,
            returnType: TypeName = ClassNames.String
        ) = BidiStreamMethod(
            declaredName,
            methodName,
            requestName,
            requestType,
            returnType
        )
    }
}