package com.github.darvld.krpc.compiler.testing

import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspIncremental
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.intellij.lang.annotations.Language
import java.io.File
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

@Suppress("nothing_to_inline")
inline fun <T> T?.assertNotNull(message: String? = null): T = assertNotNull(this, message)



/**Compiles the [source] files provided and returns the result after applying the symbol processor provider by [processorProvider].*/
fun compile(processorProvider: SymbolProcessorProvider, vararg source: SourceFile): KotlinCompilation.Result {
    return KotlinCompilation().apply {
        assertTrue(source.isNotEmpty(), "No sources were provided for compilation")
        sources = source.toList()
        
        symbolProcessorProviders = listOf(processorProvider)
        kspIncremental = false
        inheritClassPath = true
        verbose = false
    }.compile()
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