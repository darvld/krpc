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

package com.github.darvld.krpc.compiler.testing

import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspIncremental
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.intellij.lang.annotations.Language
import java.io.File
import java.io.OutputStream
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

val KotlinCompilation.Result.workingDir: File
    get() = checkNotNull(outputDirectory.parentFile)

val KotlinCompilation.Result.kspGeneratedSources: List<File>
    get() = workingDir.resolve("ksp/sources/")
        .walk()
        .mapNotNull { if (it.isFile) it else null }
        .toList()


/**Compiles the [source] files provided and returns the result after applying the symbol processor provider by [processorProvider].*/
fun compile(processorProvider: SymbolProcessorProvider, vararg source: SourceFile): KotlinCompilation.Result {
    return KotlinCompilation().apply {
        assertTrue(source.isNotEmpty(), "No sources were provided for compilation")
        sources = source.toList()

        messageOutputStream = OutputStream.nullOutputStream()
        verbose = false

        symbolProcessorProviders = listOf(processorProvider)
        kspIncremental = false

        inheritClassPath = true
    }.compile()
}

inline fun <R> whenCompiling(
    using: SymbolProcessorProvider,
    vararg source: SourceFile,
    block: KotlinCompilation.Result.() -> R
): R {
    return compile(using, *source).run(block)
}

/**Finds a file by its [name] (without extension) or fails the test with [message].*/
fun List<File>.findByName(name: String, message: String? = null): File {
    val match = find { it.nameWithoutExtension == name }
    match.assertExists(mustBeFile = true, message)
    return match
}

/**Assert that this file exists, and optionally checks whether it *is* a file or not.*/
@OptIn(ExperimentalContracts::class)
fun File?.assertExists(mustBeFile: Boolean = true, message: String? = null) {
    contract {
        returns() implies (this@assertExists != null)
    }

    assertNotNull(this, "$message (file is null)")
    assertTrue(exists() && isFile == mustBeFile, "$message (file does not exist)")
}

/**Asserts that the content of this file match the [expected] string.*/
fun File.assertContentEquals(@Language("kotlin") expected: String, message: String? = null) {
    assertEquals(expected, readText(), message)
}

/**Asserts that this compilation generated at least one source file using KSP.*/
fun KotlinCompilation.Result.assertGeneratedKspSources() {
    assert(kspGeneratedSources.isNotEmpty()) { "No KSP sources were generated" }
}

/**Asserts that the compilation's exit code matches the given [code].*/
fun KotlinCompilation.Result.assertExitCode(code: KotlinCompilation.ExitCode): KotlinCompilation.Result {
    assertEquals(code, exitCode, "Exit code $exitCode does not match the expected: $code")
    return this
}