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
import io.github.darvld.krpc.*
import io.github.darvld.krpc.compiler.addClass
import io.github.darvld.krpc.compiler.buildFile
import io.github.darvld.krpc.compiler.markAsGenerated
import io.github.darvld.krpc.compiler.model.CompositeRequest
import io.github.darvld.krpc.compiler.model.RequestInfo.Companion.requestTypeFor
import io.github.darvld.krpc.compiler.model.ServiceDefinition
import io.github.darvld.krpc.compiler.model.ServiceMethodDefinition
import kotlinx.serialization.Serializable
import java.io.OutputStream

internal class DescriptorGenerator : ServiceComponentGenerator() {

    override fun getFilename(service: ServiceDefinition): String {
        return service.descriptorName
    }

    override fun generateComponent(output: OutputStream, service: ServiceDefinition) {
        buildFile(service.packageName, service.descriptorName, output) {
            addClass {
                markAsGenerated()

                superclass(AbstractServiceDescriptor::class)

                addKdoc(
                    """
                    Internal helper class generated by the kRPC compiler for the [%T] interface.
                         
                    This class provides method descriptors for other generated service components.
                    It should not be used in general code.
                    
                    @constructor Constructs a new [${service.descriptorName}] using a [SerializationProvider]
                    to create the marshallers for method requests/responses.
                    @param $SERIALIZATION_PROVIDER_PARAM A provider implementing a serialization format.
                    Used to generate transcoders for rpc methods.
                    """.trimIndent(),
                    service.className
                )

                // Constructor with serialization provider as parameter (used to initialize method descriptors)
                primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter(SERIALIZATION_PROVIDER_PARAM, SerializationProvider::class)
                        .build()
                )

                // Override serviceName abstract property
                addProperty(
                    PropertySpec.builder(SERVICE_NAME_PROPERTY, STRING)
                        .addModifiers(OVERRIDE)
                        .initializer("%S", service.serviceName)
                        .build()
                )

                // Generate method descriptors
                for (method in service.methods) {
                    val requestType = service.requestTypeFor(method)
                    val descriptor = buildMethodDescriptor(method, service, requestType)

                    if (method.request is CompositeRequest) addType(buildRequestWrapper(method))

                    addTranscoder(requestType)
                    addTranscoder(method.responseType)

                    addProperty(descriptor)
                }
            }
        }
    }

    fun buildRequestWrapper(method: ServiceMethodDefinition): TypeSpec {
        require(method.request is CompositeRequest)

        return TypeSpec.classBuilder(method.request.wrapperName).apply {
            addModifiers(INTERNAL, DATA)
            addAnnotation(Serializable::class)
            markAsGenerated()

            addKdoc(
                """
            Internal wrapper class generated by the kRPC compiler.
             
            This wrapper is used internally to support multiple arguments in service methods,
            it should not be used in general code.
            """.trimIndent()
            )

            val constructor = FunSpec.constructorBuilder()

            for ((name, type) in method.request.parameters) {
                constructor.addParameter(name, type)
                addProperty(PropertySpec.builder(name, type).initializer(name).build())
            }

            primaryConstructor(constructor.build())
        }.build()
    }

    fun buildMethodDescriptor(
        method: ServiceMethodDefinition,
        service: ServiceDefinition,
        requestType: TypeName,
    ): PropertySpec = with(method) {

        return PropertySpec.builder(
            method.declaredName,
            METHOD_DESCRIPTOR.parameterizedBy(requestType, responseType)
        ).run {
            markAsGenerated()
            addModifiers(INTERNAL)
            mutable(false)
            addKdoc(
                DESCRIPTOR_KDOC.trimIndent(),
                "${service.declaredName}.${method.declaredName}",
                "${service.packageName}.${service.declaredName}.${method.declaredName}"
            )
            initializer(
                """
                methodDescriptor(
                  name=%S,
                  type=%M,
                  requestTranscoder=%L,
                  responseTranscoder=%L
                )
                """.trimIndent(),
                methodName,
                methodType.asMember(),
                requestType.transcoderName,
                responseType.transcoderName
            )
            build()
        }
    }

    companion object {
        const val SERVICE_NAME_PROPERTY = "serviceName"
        const val SERIALIZATION_PROVIDER_PARAM = "serializationProvider"

        const val DESCRIPTOR_KDOC = """
        Generated gRPC [MethodDescriptor] for the [%L][%L] method.
        
        This descriptor is used by generated service components and should not be used in general code.
        """

        val TRANSCODER = Transcoder::class.asTypeName()
        val METHOD_DESCRIPTOR = ClassName("io.github.darvld.krpc", "MethodDescriptor")

        val TRANSCODER_EXTENSION_MEMBER = MemberName("io.github.darvld.krpc", "transcoder", isExtension = true)

        private fun MethodType.asMember(): MemberName {
            return MemberName(ClassName("io.github.darvld.krpc", "MethodType"), name)
        }
    }
}
