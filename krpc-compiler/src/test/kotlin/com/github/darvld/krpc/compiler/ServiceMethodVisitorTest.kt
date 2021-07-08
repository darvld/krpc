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
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
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
        requestName: String = "request",
        requestType: TypeName = Int::class.asTypeName(),
        returnType: TypeName = String::class.asTypeName(),
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
                it.requestName shouldBe requestName
                it.requestType shouldBe requestType
                it.returnType shouldBe returnType
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
        errorMessage = "ClientStream rpc methods must declare a Flow of a serializable type as the only parameter.",
        imports = "import kotlinx.coroutines.flow.Flow",
        definitionBlock = """
        @ClientStream
        suspend fun invalid(request: Int): String
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
        errorMessage = "BidiStream rpc methods must declare a Flow of a serializable type as the only parameter.",
        imports = "import kotlinx.coroutines.flow.Flow",
        definitionBlock = """
        @BidiStream
        fun invalid(request: Int): Flow<String>
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
    fun `extracts valid unary call definition without arguments`() =
        validateMethodExtraction<UnaryMethod>(
            declaredName = "unary",
            methodName = "unaryCall",
            type = UNARY,
            requestName = "unit",
            requestType = UnitClassName,
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
            returnType = UnitClassName,
            suspending = true,
            definitionBlock = """
            @UnaryCall("unaryCall")
            suspend fun unary(request: Int)
            """.trimIndent()
        )
    
    @Test
    fun `extracts valid server stream call definition`() =
        validateMethodExtraction<ServerStreamMethod>(
            declaredName = "stream",
            methodName = "serverStream",
            type = SERVER_STREAMING,
            suspending = false,
            returnType = FlowClassName.parameterizedBy(String::class.asTypeName()),
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
            requestName = "unit",
            requestType = UnitClassName,
            suspending = false,
            returnType = FlowClassName.parameterizedBy(String::class.asTypeName()),
            imports = "import kotlinx.coroutines.flow.Flow",
            definitionBlock = """
            @ServerStream("serverStream")
            fun stream(): Flow<String>
            """.trimIndent()
        )
    
    @Test
    fun `extracts valid client stream call definition`() =
        validateMethodExtraction<ClientStreamMethod>(
            declaredName = "stream",
            methodName = "clientStream",
            type = CLIENT_STREAMING,
            suspending = true,
            requestType = FlowClassName.parameterizedBy(Int::class.asTypeName()),
            imports = "import kotlinx.coroutines.flow.Flow",
            definitionBlock = """
            @ClientStream("clientStream")
            suspend fun stream(request: Flow<Int>): String
            """.trimIndent()
        )
    
    @Test
    fun `extracts valid client stream call definition without return type`() =
        validateMethodExtraction<ClientStreamMethod>(
            declaredName = "stream",
            methodName = "clientStream",
            type = CLIENT_STREAMING,
            suspending = true,
            requestType = FlowClassName.parameterizedBy(Int::class.asClassName()),
            returnType = UnitClassName,
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
        requestType = FlowClassName.parameterizedBy(Int::class.asTypeName()),
        returnType = FlowClassName.parameterizedBy(String::class.asTypeName()),
        imports = "import kotlinx.coroutines.flow.Flow",
        definitionBlock = """
        @BidiStream("bidiStream")
        fun stream(request: Flow<Int>): Flow<String>
        """.trimIndent()
    )
}