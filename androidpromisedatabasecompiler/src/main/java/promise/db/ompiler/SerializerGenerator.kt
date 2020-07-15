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
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName

class SerializerGenerator(
    fileSpec: FileSpec.Builder,
    private val typeDataTypePack: String,
    private val typeDataType: String,
    private val columns: List<Pair<Pair<String, TypeName>, String>>) : CodeBlockGenerator<FunSpec> {
  init {
    fileSpec.addImport("android.content", "ContentValues")
  }

  override fun generate(): FunSpec {
    var stmt =
        "  return  ContentValues().apply {\n"
    columns.forEach {
      stmt += generatePutStatement(it.first.first, it.second, it.first.second)
    }

    stmt += "}"

    return FunSpec.builder("serialize")
        .addParameter(ParameterSpec("t", ClassName(typeDataTypePack, typeDataType)))
        .addModifiers(KModifier.OVERRIDE)
        .returns(ClassName("android.content", "ContentValues"))
        .addCode(CodeBlock.of(stmt))
        .build()
  }

  private fun generatePutStatement(typeVariable: String, columnName: String, varTypeName: TypeName): String {
    if (varTypeName.isSameAs(Boolean::class.java)) {
      return "put(${columnName}.name, if (t.${typeVariable}) 1 else 0) \n"
    }
    return "put(${columnName}.name, t.${typeVariable}) \n"
  }
}