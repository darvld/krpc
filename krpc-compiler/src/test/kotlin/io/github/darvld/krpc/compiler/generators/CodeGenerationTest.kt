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

import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import io.github.darvld.krpc.compiler.dsl.writeFile
import io.github.darvld.krpc.compiler.model.RequestInfo
import io.github.darvld.krpc.compiler.model.ServiceDefinition
import io.github.darvld.krpc.compiler.model.ServiceMethodDefinition
import io.github.darvld.krpc.compiler.model.SimpleRequest
import io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING
import io.grpc.MethodDescriptor.MethodType.UNARY
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
            writeFile("com.test.generated", name, stream) {
                TypeSpec.objectBuilder(name).apply(block).build().also(::addType)
            }
        }
        return file
    }

    companion object {

        fun simpleRequest(name: String = "request", type: TypeName = INT): SimpleRequest {
            return SimpleRequest(name, type)
        }

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
            request: RequestInfo = simpleRequest(),
            returnType: TypeName = STRING
        ): ServiceMethodDefinition = ServiceMethodDefinition(
            declaredName,
            methodName,
            isSuspending = true,
            methodType = UNARY,
            request = request,
            responseType = returnType
        )

        fun bidiStreamMethod(
            declaredName: String = "bidiStream",
            methodName: String = "${declaredName}Test",
            request: RequestInfo = simpleRequest(),
            returnType: TypeName = STRING
        ): ServiceMethodDefinition = ServiceMethodDefinition(
            declaredName,
            methodName,
            isSuspending = false,
            methodType = BIDI_STREAMING,
            request = request,
            responseType = returnType
        )
    }
}