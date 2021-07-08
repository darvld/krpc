import com.github.darvld.krpc.compiler.FlowClassName
import com.github.darvld.krpc.compiler.buildFile
import com.github.darvld.krpc.compiler.model.BidiStreamMethod
import com.github.darvld.krpc.compiler.model.ServiceDefinition
import com.github.darvld.krpc.compiler.model.ServiceMethodDefinition
import com.github.darvld.krpc.compiler.model.UnaryMethod
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
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
    ): ServiceDefinition = ServiceDefinition(
        declaredName,
        packageName,
        serviceName,
        clientName,
        providerName,
        methods
    )

    protected fun unaryMethod(
        declaredName: String = "unary",
        methodName: String = "${declaredName}Test",
        requestName: String = "request",
        requestType: ClassName = Int::class.asClassName(),
        returnType: ClassName = String::class.asClassName()
    ): UnaryMethod = UnaryMethod(
        declaredName,
        methodName,
        requestName,
        requestType,
        returnType
    )

    protected fun bidiStreamMethod(
        declaredName: String = "bidiStream",
        methodName: String = "${declaredName}Test",
        requestName: String = "request",
        requestType: ParameterizedTypeName = FlowClassName.parameterizedBy(Int::class.asClassName()),
        returnType: ParameterizedTypeName = FlowClassName.parameterizedBy(String::class.asClassName())
    ) = BidiStreamMethod(
        declaredName,
        methodName,
        requestName,
        requestType,
        returnType
    )

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