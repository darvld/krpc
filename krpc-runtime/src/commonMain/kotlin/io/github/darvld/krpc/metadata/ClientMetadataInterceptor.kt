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

package io.github.darvld.krpc.metadata

/**A simple client interceptor used to provide metadata headers for a call.
 *
 * The most common use case for this class is to provide authentication/authorization tokens for service calls.*/
expect abstract class ClientMetadataInterceptor() {
    /**Intercepts a call's [metadata].
     *
     * The returned value can be a new instance, however in most cases
     * an interceptor will simple use [CallMetadata.put] to provide new headers and return the same [metadata]
     * instance being intercepted.*/
    abstract fun intercept(metadata: CallMetadata): CallMetadata
}