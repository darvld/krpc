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

package io.github.darvld.krpc.compiler

import com.google.devtools.ksp.symbol.KSNode

/**Signals a fatal error during the symbol processing task, optionally indicating the symbol where the error was encountered.*/
class ProcessingError(val symbol: KSNode?, override val message: String) : Error(message)

/**Interrupts the processing by throwing a new [ProcessingError] with the specified [message] and [symbol].*/
fun reportError(symbol: KSNode?, message: String): Nothing {
    throw ProcessingError(symbol, message)
}