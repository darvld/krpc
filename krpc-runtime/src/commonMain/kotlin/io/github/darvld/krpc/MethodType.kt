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

/**Describes the type of an rpc method: [UNARY], [CLIENT_STREAMING], [SERVER_STREAMING] or [BIDI_STREAMING].
 *
 * On some platforms, such as JVM, this enum may contain some more entries.*/
expect enum class MethodType {
    /**The client sends a single request and receives a single response. Unary methods suspend until the response
     * is received.*/
    UNARY,

    /**The client sends a stream of requests and receives a single response. Client streaming methods suspend
     * until the response is received.*/
    CLIENT_STREAMING,

    /**The client sends a single request and receives a stream of responses. Server streaming methods don't
     * suspend, instead the back-pressure control is provided through the returned `Flow`.*/
    SERVER_STREAMING,

    /**The client sends a stream of requests and receives a stream of responses. Bidirectional streaming methods don't
     * suspend, instead the back-pressure control is provided through the returned `Flow`.*/
    BIDI_STREAMING,
}