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

/**Interface providing a basic contract to convert between values of a certain type [T]
 *  and their serialized representation using a platform-specific [EncodedDataStream].*/
interface Transcoder<T> {
    /**Decodes a value of type [T] given its serialized form as [EncodedDataStream].*/
    fun decode(from: EncodedDataStream): T

    /**Encodes the [value] and returns a serialized form as [EncodedDataStream].*/
    fun encode(value: T): EncodedDataStream
}