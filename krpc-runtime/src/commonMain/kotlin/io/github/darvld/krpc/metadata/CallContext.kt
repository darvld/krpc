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

/**A context used to propagate information within rpc calls. The context is usually modifier by a
 * server interceptor using [CallContext.withValue], and consumed by the service providers using [CallContextKey.get].*/
expect class CallContext {
    /**Returns a new context with the given [value] associated to [key].*/
    fun <V> withValue(key: CallContextKey<V>, value: V): CallContext
}

/**A key used to store and retrieve values from a [CallContext].*/
expect class CallContextKey<T> {
    /**Retrieves the value associated with this key in the current context.*/
    fun get(): T

    /**Retrieves the value associated with this key in the given [context].*/
    fun get(context: CallContext): T
}

/**Creates a new [CallContextKey] with the given debug [name]. Use it to construct new context keys from common code.*/
expect fun <T> contextKey(name: String): CallContextKey<T>