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

/**A simple server interceptor used to modify the current call context.
 *
 * Common uses include verifying the [CallMetadata] for authentication/authorization purposes, or
 * providing contextual information about the caller to the service implementation.*/
expect abstract class ServerMetadataInterceptor {
    /**Intercepts a call's [context], allowing the implementation to provide new values using the information
     * extracted from the call's [metadata].
     *
     * The returned context should be either the one provided in the arguments, or one derived from it using
     * [CallContext.withValue].*/
    abstract fun intercept(context: CallContext, metadata: CallMetadata): CallContext
}