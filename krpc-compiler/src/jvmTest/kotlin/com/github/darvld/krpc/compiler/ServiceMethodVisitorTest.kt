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

import com.github.darvld.krpc.compiler.model.*
import com.github.darvld.krpc.compiler.testing.assertIs
import com.github.darvld.krpc.compiler.testing.shouldBe
import com.github.darvld.krpc.compiler.testing.shouldContain
import com.github.darvld.krpc.compiler.testing.whenCompiling
import com.google.devtools.ksp.getFunctionDeclarationsByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import com.tschuchort.compiletesting.SourceFile
import io.grpc.MethodDescriptor
import io.grpc.MethodDescriptor.MethodType.*
import org.intellij.lang.annotations.Language
import org.junit.Test
import kotlin.test.fail

class ServiceMethodVisitorTest {
    private val methodVisitor = ServiceMethodVisitor()

    private fun simpleRequest() = SimpleRequest("request", INT)

    private fun singleMethodProcessorProvider(
        declaredName: String,
        includeTopLevel: Boolean = true,
        testBlock: KSFunctionDeclaration.() -> Unit
    ): SymbolProcessorProvider = SymbolProcessorProvider {
        object : SymbolProcessor {
            override fun process(resolver: Resolver): List<KSAnnotated> {
                resolver.getFunctionDeclarationsByName("com.test.generated.$declaredName", includeTopLevel)
                    .single()
                    .testBlock()

                return emptyList()
            }
        }
    }

    private inline fun <reified T : ServiceMethodDefinition> validateMethodExtraction(
        declaredName: String,
        methodName: String,
        type: MethodDescriptor.MethodType,
        request: RequestInfo = simpleRequest(),
        responseType: TypeName = STRING,
        suspending: Boolean,
        @Language("kotlin") imports: String = "",
        @Language("kotlin") definitionBlock: String
    ) {
        val definition = SourceFile.kotlin(
            name = "TestService.kt",
            contents = """
            package com.test.generated
            import com.github.darvld.krpc.*
            $imports

            $definitionBlock
            """
        )

        val processorProvider = singleMethodProcessorProvider(declaredName) {
            accept(methodVisitor, Unit).assertIs<T>().let {
                it.declaredName shouldBe declaredName
                it.methodName shouldBe methodName
                it.methodType shouldBe type
                it.request shouldBe request
                it.responseType shouldBe responseType
                it.isSuspending shouldBe suspending
            }
        }

        whenCompiling(processorProvider, definition) {
            if (exitCode != OK) fail(messages)
        }
    }

    private fun assertExtractionFailsWith(
        errorMessage: String,
        declaredName: String = "invalid",
        @Language("kotlin") imports: String = "",
        @Language("kotlin") definitionBlock: String
    ) {
        val source = SourceFile.kotlin(
            name = "TestService.kt",
            contents = """
            package com.test.generated
            import com.github.darvld.krpc.*
            $imports

            $definitionBlock
            """
        )

        val provider = singleMethodProcessorProvider(declaredName) {
            accept(methodVisitor, Unit)
        }

        whenCompiling(using = provider, source) {
            exitCode shouldBe COMPILATION_ERROR
            messages shouldContain errorMessage
        }
    }

    @Test
    fun `fails for method without annotations`() = assertExtractionFailsWith(
        errorMessage = "Service methods must provide the corresponding type annotation",
        definitionBlock = "suspend fun invalid(request: Int): String",
    )

    @Test
    fun `fails to extract unary call without suspend modifier`() = assertExtractionFailsWith(
        errorMessage = "UnaryCall rpc methods must be marked with 'suspend' modifier",
        definitionBlock = """
        @UnaryCall
        fun invalid(request: Int): String
        """.trimIndent()
    )

    @Test
    fun `fails to extract client stream call without suspend modifier`() = assertExtractionFailsWith(
        errorMessage = "ClientStream rpc methods must be marked with 'suspend' modifier",
        imports = "import kotlinx.coroutines.flow.Flow",
        definitionBlock = """
        @ClientStream
        fun invalid(request: Flow<Int>): String
        """.trimIndent()
    )

    @Test
    fun `fails to extract client stream call with non-flow request`() = assertExtractionFailsWith(
        errorMessage = "Expected a single Flow<T> argument.",
        imports = "import kotlinx.coroutines.flow.Flow",
        definitionBlock = """
        @ClientStream
        suspend fun invalid(request: Int): String
        """.trimIndent()
    )

    @Test
    fun `fails to extract client stream call with multiple arguments`() = assertExtractionFailsWith(
        errorMessage = "Multiple arguments are not supported for methods using client-side streaming.",
        imports = "import kotlinx.coroutines.flow.Flow",
        definitionBlock = """
        @ClientStream
        suspend fun invalid(request: Int, data: String, messages: Flow<Int>): String
        """.trimIndent()
    )

    @Test
    fun `fails to extract server stream call with non-flow return type`() = assertExtractionFailsWith(
        errorMessage = "ServerStream rpc methods must return a Flow of a serializable type.",
        definitionBlock = """
        @ServerStream
        fun invalid(request: Int): String
        """.trimIndent()
    )

    @Test
    fun `fails to extract server stream call with suspend modifier`() = assertExtractionFailsWith(
        errorMessage = "ServerStream rpc methods must not be marked with 'suspend' modifier.",
        imports = "import kotlinx.coroutines.flow.Flow",
        definitionBlock = """
        @ServerStream
        suspend fun invalid(request: Int): Flow<String>
        """.trimIndent()
    )

    @Test
    fun `fails to extract bidi stream call with non-flow request`() = assertExtractionFailsWith(
        errorMessage = "Expected a single Flow<T> argument.",
        imports = "import kotlinx.coroutines.flow.Flow",
        definitionBlock = """
        @BidiStream
        fun invalid(request: Int): Flow<String>
        """.trimIndent()
    )

    @Test
    fun `fails to extract bidi stream call with multiple arguments`() = assertExtractionFailsWith(
        errorMessage = "Multiple arguments are not supported for methods using client-side streaming.",
        imports = "import kotlinx.coroutines.flow.Flow",
        definitionBlock = """
        @BidiStream
        fun invalid(request: Int, data: String, messages: Flow<Int>): Flow<String>
        """.trimIndent()
    )

    @Test
    fun `fails to extract bidi stream call with non-flow return type`() = assertExtractionFailsWith(
        errorMessage = "BidiStream rpc methods must return a Flow of a serializable type.",
        imports = "import kotlinx.coroutines.flow.Flow",
        definitionBlock = """
        @BidiStream
        fun invalid(request: Flow<Int>): String
        """.trimIndent()
    )

    @Test
    fun `fails to extract bidi stream call with suspend modifier`() = assertExtractionFailsWith(
        errorMessage = "BidiStream rpc methods must not be marked with 'suspend' modifier.",
        imports = "import kotlinx.coroutines.flow.Flow",
        definitionBlock = """
        @BidiStream
        suspend fun invalid(request: Flow<Int>): Flow<String>
        """.trimIndent()
    )

    @Test
    fun `extracts valid unary call definition`() = validateMethodExtraction<UnaryMethod>(
        declaredName = "unary",
        methodName = "unaryCall",
        type = UNARY,
        suspending = true,
        definitionBlock = """
        @UnaryCall("unaryCall")
        suspend fun unary(request: Int): String
        """.trimIndent()
    )

    @Test
    fun `extracts valid unary call definition with multiple arguments`() = validateMethodExtraction<UnaryMethod>(
        declaredName = "unary",
        methodName = "unaryCall",
        type = UNARY,
        suspending = true,
        request = CompositeRequest(mapOf("id" to LONG, "name" to STRING, "age" to INT), "UnaryRequest"),
        imports = "import kotlin.Long",
        definitionBlock = """
        @UnaryCall("unaryCall")
        suspend fun unary(id: Long, name: String, age: Int): String
        """.trimIndent()
    )

    @Test
    fun `extracts valid unary call definition without arguments`() =
        validateMethodExtraction<UnaryMethod>(
            declaredName = "unary",
            methodName = "unaryCall",
            type = UNARY,
            request = NoRequest,
            suspending = true,
            definitionBlock = """
            @UnaryCall("unaryCall")
            suspend fun unary(): String
            """.trimIndent()
        )

    @Test
    fun `extracts valid unary call definition without return type`() =
        validateMethodExtraction<UnaryMethod>(
            declaredName = "unary",
            methodName = "unaryCall",
            type = UNARY,
            responseType = UnitClassName,
            suspending = true,
            definitionBlock = """
            @UnaryCall("unaryCall")
            suspend fun unary(request: Int)
            """.trimIndent()
        )

    @Test
    fun `extracts valid unary call definition with generic argument`() =
        validateMethodExtraction<UnaryMethod>(
            declaredName = "unary",
            methodName = "unaryCall",
            type = UNARY,
            request = SimpleRequest("request", LIST.parameterizedBy(INT)),
            suspending = true,
            imports = "import kotlin.collections.List",
            definitionBlock = """
            @UnaryCall("unaryCall")
            suspend fun unary(request: List<Int>): String
            """.trimIndent()
        )

    @Test
    fun `extracts valid unary call definition with generic return type`() =
        validateMethodExtraction<UnaryMethod>(
            declaredName = "unary",
            methodName = "unaryCall",
            type = UNARY,
            responseType = LIST.parameterizedBy(STRING),
            suspending = true,
            imports = "import kotlin.collections.List",
            definitionBlock = """
            @UnaryCall("unaryCall")
            suspend fun unary(request: Int): List<String>
            """.trimIndent()
        )

    @Test
    fun `extracts valid server stream call definition`() =
        validateMethodExtraction<ServerStreamMethod>(
            declaredName = "stream",
            methodName = "serverStream",
            type = SERVER_STREAMING,
            suspending = false,
            responseType = STRING,
            imports = "import kotlinx.coroutines.flow.Flow",
            definitionBlock = """
            @ServerStream("serverStream")
            fun stream(request: Int): Flow<String>
            """.trimIndent()
        )

    @Test
    fun `extracts valid server stream call definition without arguments`() =
        validateMethodExtraction<ServerStreamMethod>(
            declaredName = "stream",
            methodName = "serverStream",
            type = SERVER_STREAMING,
            request = NoRequest,
            suspending = false,
            responseType = STRING,
            imports = "import kotlinx.coroutines.flow.Flow",
            definitionBlock = """
            @ServerStream("serverStream")
            fun stream(): Flow<String>
            """.trimIndent()
        )

    @Test
    fun `extracts valid server stream call definition with generic return type`() =
        validateMethodExtraction<ServerStreamMethod>(
            declaredName = "stream",
            methodName = "serverStream",
            type = SERVER_STREAMING,
            suspending = false,
            responseType = LIST.parameterizedBy(STRING),
            imports = """
            import kotlin.collections.List
            import kotlinx.coroutines.flow.Flow
            """.trimIndent(),
            definitionBlock = """
            @ServerStream("serverStream")
            fun stream(request: Int): Flow<List<String>>
            """.trimIndent()
        )

    @Test
    fun `extracts valid client stream call definition`() =
        validateMethodExtraction<ClientStreamMethod>(
            declaredName = "stream",
            methodName = "clientStream",
            type = CLIENT_STREAMING,
            suspending = true,
            imports = "import kotlinx.coroutines.flow.Flow",
            definitionBlock = """
            @ClientStream("clientStream")
            suspend fun stream(request: Flow<Int>): String
            """.trimIndent()
        )

    @Test
    fun `extracts valid client stream call definition with generic request type`() =
        validateMethodExtraction<ClientStreamMethod>(
            declaredName = "stream",
            methodName = "clientStream",
            type = CLIENT_STREAMING,
            suspending = true,
            request = SimpleRequest("request", LIST.parameterizedBy(INT)),
            imports = """
            import kotlin.collections.List
            import kotlinx.coroutines.flow.Flow
            """.trimIndent(),
            definitionBlock = """
            @ClientStream("clientStream")
            suspend fun stream(request: Flow<List<Int>>): String
            """.trimIndent()
        )

    @Test
    fun `extracts valid client stream call definition without return type`() =
        validateMethodExtraction<ClientStreamMethod>(
            declaredName = "stream",
            methodName = "clientStream",
            type = CLIENT_STREAMING,
            suspending = true,
            responseType = UNIT,
            imports = "import kotlinx.coroutines.flow.Flow",
            definitionBlock = """
            @ClientStream("clientStream")
            suspend fun stream(request: Flow<Int>)
            """.trimIndent()
        )

    @Test
    fun `extracts valid bidi stream call definition`() = validateMethodExtraction<BidiStreamMethod>(
        declaredName = "stream",
        methodName = "bidiStream",
        type = BIDI_STREAMING,
        suspending = false,
        responseType = STRING,
        imports = "import kotlinx.coroutines.flow.Flow",
        definitionBlock = """
        @BidiStream("bidiStream")
        fun stream(request: Flow<Int>): Flow<String>
        """.trimIndent()
    )

    @Test
    fun `extracts valid bidi stream call definition with generic types`() = validateMethodExtraction<BidiStreamMethod>(
        declaredName = "stream",
        methodName = "bidiStream",
        type = BIDI_STREAMING,
        suspending = false,
        request = SimpleRequest("request", LIST.parameterizedBy(INT)),
        responseType = LIST.parameterizedBy(STRING),
        imports = """
        import kotlin.collections.List
        import kotlinx.coroutines.flow.Flow
        """.trimIndent(),
        definitionBlock = """
        @BidiStream("bidiStream")
        fun stream(request: Flow<List<Int>>): Flow<List<String>>
        """.trimIndent()
    )
}