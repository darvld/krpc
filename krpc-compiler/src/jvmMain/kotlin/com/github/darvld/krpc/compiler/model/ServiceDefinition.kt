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

package com.github.darvld.krpc.compiler.model

import com.squareup.kotlinpoet.ClassName

/**Model class used by [ServiceProcessor][com.github.darvld.krpc.compiler.ServiceProcessor] to store information
 * about the service being processed.
 *
 * @see ServiceMethodDefinition*/
data class ServiceDefinition(
    /**The name of the interface used to define the service.*/
    val declaredName: String,
    /**The package name of the service definition, used for the generated sources.*/
    val packageName: String,
    /**The "official" GRPC name for this service.*/
    val serviceName: String,
    /**The name of the pseudo-constructor function to be generated as a client implementation.*/
    val clientName: String,
    /**The name of the generated abstract service provider.*/
    val providerName: String,
    /**A list of methods defined for the service.*/
    val methods: List<ServiceMethodDefinition>
) {
    /**The name of this method's generated descriptor class.*/
    inline val descriptorName: String get() = "${serviceName}Descriptor"

    /**The [ClassName] for this service's declaring interface.*/
    val className by lazy { ClassName(packageName, declaredName) }

    /**The [ClassName] for the generated descriptor.*/
    val descriptorClassName by lazy { ClassName(packageName, descriptorName) }

    /**The [ClassName] for the generated client implementation.*/
    val clientClassName by lazy { ClassName(packageName, clientName) }

    /**The [ClassName] for the generated service provider.*/
    val providerClassName by lazy { ClassName(packageName, providerName) }
}