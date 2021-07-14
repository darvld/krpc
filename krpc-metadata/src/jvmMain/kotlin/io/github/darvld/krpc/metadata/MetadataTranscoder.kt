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

import io.github.darvld.krpc.Transcoder
import io.grpc.Metadata
import java.io.InputStream

@JvmInline
value class MetadataTranscoder<T>(private val transcoder: Transcoder<T>) : Metadata.BinaryStreamMarshaller<T> {
    override fun parseStream(stream: InputStream): T = transcoder.decode(stream)
    override fun toStream(value: T): InputStream = transcoder.encode(value)
}