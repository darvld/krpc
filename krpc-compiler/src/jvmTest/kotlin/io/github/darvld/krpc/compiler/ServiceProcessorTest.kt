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

import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import com.tschuchort.compiletesting.SourceFile
import io.github.darvld.krpc.compiler.generators.CodeGenerationTest.Companion.serviceDefinition
import io.github.darvld.krpc.compiler.generators.ServiceComponentGenerator
import io.github.darvld.krpc.compiler.testing.whenCompiling
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