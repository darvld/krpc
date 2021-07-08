package com.github.darvld.krpc.compiler

import com.github.darvld.krpc.Service
import com.github.darvld.krpc.compiler.model.ServiceDefinition
import com.github.darvld.krpc.compiler.testing.shouldBe
import com.github.darvld.krpc.compiler.testing.shouldContain
import com.github.darvld.krpc.compiler.testing.whenCompiling
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.asClassName
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import com.tschuchort.compiletesting.SourceFile
import org.intellij.lang.annotations.Language
import org.junit.Test
import kotlin.test.fail

class ServiceVisitorTest {
    private val serviceVisitor = ServiceVisitor()
    
    private fun singleServiceProcessorProvider(
        testBlock: KSAnnotated.() -> Unit
    ): SymbolProcessorProvider = SymbolProcessorProvider {
        object : SymbolProcessor {
            override fun process(resolver: Resolver): List<KSAnnotated> {
                resolver.getSymbolsWithAnnotation(Service::class.qualifiedName!!)
                    .single()
                    .testBlock()
                
                return emptyList()
            }
        }
    }
    
    private fun assertExtractionFailsWith(
        errorMessage: String,
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
        
        val provider = singleServiceProcessorProvider { accept(serviceVisitor, Unit) }
        
        whenCompiling(using = provider, source) {
            exitCode shouldBe COMPILATION_ERROR
            messages shouldContain errorMessage
        }
    }
    
    private fun validateServiceExtraction(
        @Language("kotlin") imports: String = "",
        @Language("kotlin") definitionBlock: String,
        validate: ServiceDefinition.() -> Unit,
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
        
        val provider = singleServiceProcessorProvider { accept(serviceVisitor, Unit).validate() }
        
        whenCompiling(using = provider, source) {
            if (exitCode != OK) fail(messages)
        }
    }
    
    @Test
    fun `fails for non-interface declaration`() = assertExtractionFailsWith(
        errorMessage = "Service definitions must be interfaces.",
        definitionBlock = """
        @Service
        abstract class TestService
        """.trimIndent()
    )
    
    @Test
    fun `extracts valid service definition`() = validateServiceExtraction(
        definitionBlock = """
        @Service
        interface TestService {
            @UnaryCall
            suspend fun unary(request: Int): String
        }
        """.trimIndent()
    ) {
        declaredName shouldBe "TestService"
        packageName shouldBe "com.test.generated"
        
        serviceName shouldBe declaredName
        clientName shouldBe "TestClient"
        providerName shouldBe "TestServiceProvider"
        
        methods.single().run {
            declaredName shouldBe "unary"
            methodName shouldBe "unary"
            requestName shouldBe "request"
            requestType shouldBe Int::class.asClassName()
            responseType shouldBe String::class.asClassName()
        }
    }
    
    @Test
    fun `extracts valid service definition with custom names`() = validateServiceExtraction(
        definitionBlock = """
        @Service(overrideName="GpsService", providerName="GpsServer", clientName="GpsDevice")
        interface TestService {
            @UnaryCall("unaryCall")
            suspend fun unary(request: Int): String
        }
        """.trimIndent()
    ) {
        declaredName shouldBe "TestService"
        packageName shouldBe "com.test.generated"
        
        serviceName shouldBe "GpsService"
        clientName shouldBe "GpsDevice"
        providerName shouldBe "GpsServer"
        
        methods.single().run {
            declaredName shouldBe "unary"
            methodName shouldBe "unaryCall"
            requestName shouldBe "request"
            requestType shouldBe Int::class.asClassName()
            responseType shouldBe String::class.asClassName()
        }
    }
}