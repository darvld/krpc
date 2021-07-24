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

package io.github.darvld.krpc.compiler.dsl

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.OutputStream


/**Build a file using [FileSpec.Builder] and write it to [output].*/
inline fun writeFile(
    withPackage: String,
    fileName: String,
    output: OutputStream,
    block: FileSpec.Builder.() -> Unit
) {
    FileSpec.builder(withPackage, fileName).apply(block).build().let { spec ->
        output.writer().use(spec::writeTo)
    }
}

/**Build a class using [TypeSpec.classBuilder] and automatically add it to the current file.*/
inline fun FileSpec.Builder.addClass(
    packageName: String = this.packageName,
    className: String = this.name,
    block: TypeSpec.Builder.() -> Unit
): TypeSpec {
    return TypeSpec.classBuilder(ClassName(packageName, className)).apply(block).build().also(::addType)
}
