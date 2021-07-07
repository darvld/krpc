import com.github.darvld.krpc.compiler.UnitClassName
import com.github.darvld.krpc.compiler.generators.generateServiceProviderBase
import org.intellij.lang.annotations.Language
import org.junit.Test

class ProviderTest : CompilerTest() {
    @Test
    fun `unary method`() {
        val definition = serviceDefinition(methods = listOf(unaryMethod()))
        val generated = temporaryFolder.newFile()
        generated.outputStream().use { stream -> generateServiceProviderBase(stream, definition) }
        
        generated.assertContentEquals(Model.UNARY_METHOD.trimIndent())
    }
    
    @Test
    fun `unary method no request nor response`() {
        val method = unaryMethod(requestName = "unit", requestType = UnitClassName, returnType = UnitClassName)
        val definition = serviceDefinition(methods = listOf(method))
        
        val generated = temporaryFolder.newFile()
        generated.outputStream().use { stream -> generateServiceProviderBase(stream, definition) }
        
        generated.assertContentEquals(Model.UNARY_EVENT.trimIndent())
    }
}

private object Model {
    @Language("kotlin")
    const val UNARY_METHOD = """
    package com.test.generated

    import com.github.darvld.krpc.SerializationProvider
    import io.grpc.ServerServiceDefinition
    import io.grpc.kotlin.AbstractCoroutineServerImpl
    import io.grpc.kotlin.ServerCalls
    import javax.`annotation`.processing.Generated
    import kotlin.coroutines.CoroutineContext
    import kotlin.coroutines.EmptyCoroutineContext
    
    @Generated("com.github.darvld.krpc")
    public abstract class TestServiceProvider(
      serializationProvider: SerializationProvider,
      context: CoroutineContext = EmptyCoroutineContext
    ) : AbstractCoroutineServerImpl(context), TestService {
      private val definitions: TestServiceDescriptor = TestServiceDescriptor(serializationProvider)
    
      @Generated("com.github.darvld.krpc")
      public final override fun bindService(): ServerServiceDefinition =
          ServerServiceDefinition.builder("TestService").run {
            addMethod(
              ServerCalls.unaryServerMethodDefinition(
                context,
                definitions.unary,
                ::unary
              )
            )

            build()    
          }
    }

    """
    
    @Language("kotlin")
    const val UNARY_EVENT = """
    package com.test.generated

    import com.github.darvld.krpc.SerializationProvider
    import io.grpc.ServerServiceDefinition
    import io.grpc.kotlin.AbstractCoroutineServerImpl
    import io.grpc.kotlin.ServerCalls
    import javax.`annotation`.processing.Generated
    import kotlin.coroutines.CoroutineContext
    import kotlin.coroutines.EmptyCoroutineContext
    
    @Generated("com.github.darvld.krpc")
    public abstract class TestServiceProvider(
      serializationProvider: SerializationProvider,
      context: CoroutineContext = EmptyCoroutineContext
    ) : AbstractCoroutineServerImpl(context), TestService {
      private val definitions: TestServiceDescriptor = TestServiceDescriptor(serializationProvider)
    
      @Generated("com.github.darvld.krpc")
      public final override fun bindService(): ServerServiceDefinition =
          ServerServiceDefinition.builder("TestService").run {
            addMethod(
              ServerCalls.unaryServerMethodDefinition(
                context,
                definitions.unary,
                implementation = { unary() }
              )
            )

            build()    
          }
    }

    """
}
