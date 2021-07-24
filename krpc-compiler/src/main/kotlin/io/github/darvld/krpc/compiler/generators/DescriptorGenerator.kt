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

package io.github.darvld.krpc.compiler.generators

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.KModifier.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.darvld.krpc.AbstractServiceDescriptor
import io.github.darvld.krpc.MethodType
import io.github.darvld.krpc.compiler.METHOD_DESCRIPTOR
import io.github.darvld.krpc.compiler.SERIALIZATION_PROVIDER
import io.github.darvld.krpc.compiler.SERIALIZATION_PROVIDER_PARAM
import io.github.darvld.krpc.compiler.SERVICE_NAME_PROPERTY
import io.github.darvld.krpc.compiler.dsl.*
import io.github.darvld.krpc.compiler.model.CompositeRequest
import io.github.darvld.krpc.compiler.model.RequestInfo.Companion.requestTypeFor
import io.github.darvld.krpc.compiler.model.ServiceDefinition
import io.github.darvld.krpc.compiler.model.ServiceMethodDefinition
import kotlinx.serialization.Serializable
import java.io.OutputStream

/**Generates a class containing transcoders and method descriptors used for runtime configuration
 * of gRPC services.*/
object DescriptorGenerator : ServiceComponentGenerator {

    private fun MethodType.asMember(): MemberName {
        return MemberName(ClassName("io.github.darvld.krpc", "MethodType"), name)
    }

    override fun getFilename(service: ServiceDefinition): String = service.descriptorName

    override fun generateComponent(output: OutputStream, service: ServiceDefinition) {
        writeFile(service.packageName, service.descriptorName, output) {
            addClass {
                markAsGenerated()
                superclass(AbstractServiceDescriptor::class)

                addKdoc(
                    """
                    Descriptor generated by the kRPC compiler for the [${service.className}] interface.
                         
                    This class provides method descriptors for other generated service components, you can
                    use it to build your own service components instead of using the ones generated by the
                    compiler.
                    """.trimIndent(),
                )

                // Constructor with serialization provider as parameter (used to initialize method descriptors)
                addConstructor(primary = true) {
                    addParameter(SERIALIZATION_PROVIDER_PARAM, SERIALIZATION_PROVIDER)
                }

                // Override serviceName abstract property
                addProperty(SERVICE_NAME_PROPERTY, STRING, OVERRIDE) {
                    initializer("%S", service.serviceName)
                }

                // Generate method descriptors
                for (method in service.methods) {
                    val requestType = service.requestTypeFor(method)

                    if (method.request is CompositeRequest) addType(buildRequestWrapper(method))

                    addTranscoder(requestType)
                    addTranscoder(method.responseType)

                    val descriptorDoc = """
                    Generated gRPC [MethodDescriptor] for the [${service.declaredName}.${method.declaredName}] method.
                    
                    This descriptor is used by generated service components and should not be used in general code.
                    """.trimIndent()

                    addProperty(buildMethodDescriptor(method, requestType, descriptorDoc))
                }
            }
        }
    }

    /**Creates a wrapper data class around this method's arguments.
     *
     * This provides support for multiple arguments in methods without client-streaming.*/
    fun buildRequestWrapper(method: ServiceMethodDefinition): TypeSpec {
        require(method.request is CompositeRequest)

        return buildClass(method.request.wrapperName) {
            markAsGenerated()
            addAnnotation(Serializable::class)
            addModifiers(INTERNAL, DATA)

            addKdoc(
                """
                Internal wrapper class generated by the kRPC compiler.
                 
                This wrapper is used internally to support multiple arguments in service methods,
                it should not be used in general code.
                """.trimIndent()
            )

            addConstructor(primary = true) {
                for ((name, type) in method.request.parameters) {
                    addParameter(name, type)
                    addProperty(PropertySpec.builder(name, type).initializer(name).build())
                }
            }
        }
    }

    /**Creates a method descriptor for use in client and server implementations.
     *
     * Usually the descriptor property will be created only once (in the service descriptor),
     * and then accessed by other components.*/
    fun buildMethodDescriptor(
        method: ServiceMethodDefinition,
        requestType: TypeName,
        kdoc: String? = null,
    ): PropertySpec {
        return buildProperty(
            method.declaredName,
            METHOD_DESCRIPTOR.parameterizedBy(requestType, method.responseType),
            INTERNAL
        ) {
            markAsGenerated()
            kdoc?.let(::addKdoc)

            initializer(
                """
                methodDescriptor(
                  name="${method.methodName}",
                  type=%M,
                  requestTranscoder=${requestType.transcoderName},
                  responseTranscoder=${method.responseType.transcoderName}
                )
                """.trimIndent(),
                method.methodType.asMember(),
            )
        }
    }
}
