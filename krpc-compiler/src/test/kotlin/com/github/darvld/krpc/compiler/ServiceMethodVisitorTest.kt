package com.github.darvld.krpc.compiler

import com.github.darvld.krpc.compiler.model.UnaryMethod
import com.github.darvld.krpc.compiler.testing.shouldBe
import com.github.darvld.krpc.compiler.testing.shouldContain
import com.github.darvld.krpc.compiler.testing.assertExitCode
import com.github.darvld.krpc.compiler.testing.compile
import com.google.devtools.ksp.getFunctionDeclarationsByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.asClassName
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspIncremental
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.grpc.MethodDescriptor
import org.jetbrains.kotlin.utils.addToStdlib.assertedCast
import org.junit.Ignore
import org.junit.Test

class ServiceMethodVisitorTest {
    private fun provider(process: (Resolver) -> Unit) = SymbolProcessorProvider {
        object : SymbolProcessor {
            override fun process(resolver: Resolver): List<KSAnnotated> {
                process(resolver)
                return emptyList()
            }
        }
    }
    
    @Test
    fun `fails for method without annotations`() {
        val methodVisitor = ServiceMethodVisitor()
        val source = SourceFile.kotlin(
            name = "TestService.kt",
            contents = """
            package com.test.generated
            import com.github.darvld.krpc.UnaryCall

            suspend fun undefined(request: Int): String
            """
        )
        
        val provider = provider { resolver ->
            resolver.getFunctionDeclarationsByName("com.test.generated.undefined", true)
                .single()
                .accept(methodVisitor, Unit)
        }
        
        compile(provider, source).run {
            exitCode shouldBe COMPILATION_ERROR
            messages shouldContain "Service methods must provide the corresponding type annotation"
        }
    }
    
    @Ignore
    @Test
    fun `extracts valid unary call definition`() {
        val methodVisitor = ServiceMethodVisitor()
        
        val definition = SourceFile.kotlin(
            name = "TestService.kt",
            contents = """
            package com.test.generated
            import com.github.darvld.krpc.UnaryCall

            @UnaryCall("unaryCall")
            suspend fun unary(request: Int): String
            """
        )
        
        val processorProvider = provider { resolver ->
            resolver.getFunctionDeclarationsByName("com.test.generated.unary", true)
                .single()
                .accept(methodVisitor, Unit)
                .let { it.assertedCast<UnaryMethod> { "Extracted definition ($it) has unexpected type (should be UnaryMethod)." } }
                .run {
                    declaredName shouldBe "unary"
                    methodName shouldBe "unaryCall"
                    methodType shouldBe MethodDescriptor.MethodType.UNARY
                    requestName shouldBe "request"
                    requestType shouldBe Int::class.asClassName()
                    returnType shouldBe String::class.asClassName()
                    isSuspending shouldBe true
                }
        }
        
        val result = KotlinCompilation().apply {
            sources = listOf(definition)
            symbolProcessorProviders = listOf(processorProvider)
            
            kspIncremental = false
            inheritClassPath = true
            verbose = false
        }.compile()
        
        result.assertExitCode(OK)
    }
}