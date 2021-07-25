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

package io.github.darvld.krpc

/**A descriptor for rpc methods.
 *
 * This class is platform-specific, since it needs to plug into specific
 * runtime APIs that differ greatly between platforms.
 *
 * For example, while on JVM the grpc-java runtime requires
 * descriptors with marshallers to be provided, on JS the serialization is provided through callbacks
 * whenever a new call is created.*/
expect class MethodDescriptor<T, R>