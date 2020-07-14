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
import javax.annotation.processing.ProcessingEnvironment

class DeserializerGenerator(
    private val fileSpec: FileSpec.Builder,
    private val processingEnvironment: ProcessingEnvironment,
    private val typeDataTypePack: String,
    private val typeDataType: String,
    private val columns: List<Pair<Pair<String, TypeName>, String>>) : CodeBlockGenerator<FunSpec> {

  init {
    //fileSpec.addImport(ClassName("android.database", "Cursor"))
  }

  override fun generate(): FunSpec {
    var stmt =
        "    return $typeDataType().apply { \n"

    columns.forEach {
      stmt += generateSetStatement(it.first.first, it.first.second, it.second)
    }

    stmt += "}"

    return FunSpec.builder("deserialize")
        .addParameter(ParameterSpec("e", ClassName("android.database", "Cursor")))
        .addModifiers(KModifier.OVERRIDE)
        .returns(ClassName(typeDataTypePack, typeDataType))
        .addCode(CodeBlock.of(stmt))
        .build()
  }

  private fun generateSetStatement(varName: String, varType: TypeName, colName: String): String {
    if (varType.isSameAs(Boolean::class.boxedJava())) {
      return "$varName = e.${getCursorReturn(varType)}(${colName}.getIndex(e)) == 1\n"
    }
    return "$varName = e.${getCursorReturn(varType)}(${colName}.getIndex(e))\n"
  }

  private fun getCursorReturn(varType: TypeName): String {
    //processingEnvironment.messager.printMessage(Diagnostic.Kind.ERROR, "type ${varType.toString()}")
    return when {
      varType.isSameAs(Int::class.boxedJava()) -> "getInt"
      varType.isSameAs(Boolean::class.boxedJava()) -> "getInt"
      varType.isSameAs(Double::class.boxedJava()) -> "getDouble"
      varType.isSameAs(Float::class.boxedJava()) -> "getFloat"
      else -> "getString"
    }
  }

}