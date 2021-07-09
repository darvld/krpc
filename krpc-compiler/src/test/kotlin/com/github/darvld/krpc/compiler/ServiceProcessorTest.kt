package com.github.darvld.krpc.compiler

import com.github.darvld.krpc.compiler.generators.CodeGenerationTest.Companion.serviceDefinition
import com.github.darvld.krpc.compiler.generators.ServiceComponentGenerator
import com.github.darvld.krpc.compiler.testing.whenCompiling
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import com.tschuchort.compiletesting.SourceFile
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class ServiceProcessorTest {
    
    @Test
    fun `processes valid definition`() {
        val expected = serviceDefinition()
        
        val source = SourceFile.kotlin(
            name = "TestService.kt",
            contents = """
            package com.test.generated
            import com.github.darvld.krpc.*

            @Service
            interface TestService
            """
        )
        
        val mockGenerator = ServiceComponentGenerator { _, definition ->
            assertEquals(expected, definition)
        }
        
        val provider = SymbolProcessorProvider {
            ServiceProcessor(it, generators = listOf(mockGenerator))
        }
        
        whenCompiling(provider, source) {
            if (exitCode != OK) fail(messages)
        }
    }
}