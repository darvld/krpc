import com.github.darvld.krpc.compiler.buildFile
import com.github.darvld.krpc.compiler.model.ServiceDefinition
import com.github.darvld.krpc.compiler.model.ServiceMethodDefinition
import com.squareup.kotlinpoet.TypeSpec
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

abstract class CompilerTest {
    
    @Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()
    
    protected fun serviceDefinition(
        declaredName: String = "TestService",
        packageName: String = "com.test.generated",
        serviceName: String = declaredName,
        clientName: String = "TestClient",
        providerName: String = "TestServiceProvider",
        methods: List<ServiceMethodDefinition> = emptyList()
    ): ServiceDefinition {
        return ServiceDefinition(
            declaredName,
            packageName,
            serviceName,
            clientName,
            providerName,
            methods
        )
    }
    
    protected inline fun TemporaryFolder.newObject(
        name: String,
        block: TypeSpec.Builder.() -> Unit
    ): File {
        val file = newFile("$name.kt")
        file.outputStream().use { stream ->
            buildFile("com.test.generated", name, stream) {
                TypeSpec.objectBuilder(name).apply(block).build().also(::addType)
            }
        }
        return file
    }
}