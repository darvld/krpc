import com.github.darvld.krpc.compiler.UnitClassName
import com.github.darvld.krpc.compiler.generators.generateServiceProviderBase
import org.junit.Test

class ProviderTest : CompilerTest() {
    
    @Test
    fun `unary method`() {
        val definition = serviceDefinition(methods = listOf(unaryMethod()))
        val generated = temporaryFolder.newFile()
        generated.outputStream().use { stream -> generateServiceProviderBase(stream, definition) }

        generated.assertContentEquals(
            providerWithMethods(
                """addMethod(
                  ServerCalls.unaryServerMethodDefinition(
                    context,
                    definitions.unary,
                    ::unary
                  )
                )"""
            )
        )
    }

    @Test
    fun `unary method no request nor response`() {
        val method = unaryMethod(requestName = "unit", requestType = UnitClassName, returnType = UnitClassName)
        val definition = serviceDefinition(methods = listOf(method))

        val generated = temporaryFolder.newFile()
        generated.outputStream().use { stream -> generateServiceProviderBase(stream, definition) }

        generated.assertContentEquals(
            providerWithMethods(
                """addMethod(
                  ServerCalls.unaryServerMethodDefinition(
                    context,
                    definitions.unary,
                    implementation = { unary() }
                  )
                )"""
            )
        )
    }

    @Test
    fun `bidi stream`() {
        val method = bidiStreamMethod()
        val definition = serviceDefinition(methods = listOf(method))

        val generated = temporaryFolder.newFile()
        generated.outputStream().use { stream -> generateServiceProviderBase(stream, definition) }

        generated.assertContentEquals(
            providerWithMethods(
                """addMethod(
                  ServerCalls.bidiStreamingServerMethodDefinition(
                    context,
                    definitions.bidiStream,
                    ::bidiStream
                  )
                )"""
            )
        )
    }

    private fun providerWithMethods(block: String): String {
        return """
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
                $block
    
                build()    
              }
        }
    
        """.trimIndent()
    }
}