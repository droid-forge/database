/*
 * Copyright 2017, Peter Vincent
 * Licensed under the Apache License, Version 2.0, Android Promise.
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package promise.db.ompiler

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.WildcardTypeName

class RegisterColumnsGenerator(fileSpec: FileSpec.Builder, private val columns: List<String>) : CodeBlockGenerator<PropertySpec> {
  init {
    fileSpec.addImport("promise.commons.model", "List")
  }

  override fun generate(): PropertySpec {
    var stmt = "return List.fromArray("
    columns.forEachIndexed { index, s ->
      stmt += s
      if (index != columns.size - 1) {
        stmt += ", "
      }
    }
    stmt += ")"
    return PropertySpec.builder("columns", ClassName("promise.commons.model", "List")
        .parameterizedBy(WildcardTypeName.producerOf(ClassName("promise.db", "Column")
            .parameterizedBy(STAR))))
        .addModifiers(KModifier.OVERRIDE)
        .getter(FunSpec.getterBuilder()
            .addCode(CodeBlock.of(stmt))
            .build())
        .build()

  }
}