import com.github.darvld.krpc.compiler.ServiceProcessorProvider
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

internal val KotlinCompilation.Result.workingDir: File
    get() = checkNotNull(outputDirectory.parentFile)

internal val KotlinCompilation.Result.kspGeneratedSources: List<File>
    get() = workingDir.resolve("ksp/sources/")
        .walk()
        .mapNotNull { if (it.isFile) it else null }
        .toList()

internal val testSourcesRoot: File = File("src/test/resources/testCases")

internal fun resolveTestResource(path: String): File {
    return testSourcesRoot.resolve(path)
}

/**Compiles the [source] files provided and returns the result.
 *
 * This method automatically adds the [ServiceProcessorProvider] to the compiler.*/
fun compile(workingDirectory: File, vararg source: SourceFile): KotlinCompilation.Result {
    return KotlinCompilation().apply {
        assertTrue(source.isNotEmpty(), "No sources were provided for compilation")
        sources = source.toList()
        workingDir = workingDirectory
        
        symbolProcessorProviders = listOf(ServiceProcessorProvider())
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
fun KotlinCompilation.Result.assertExitCode(code: KotlinCompilation.ExitCode) {
    assertEquals(code, exitCode, "Exit code $exitCode does not match the expected: $code")
}