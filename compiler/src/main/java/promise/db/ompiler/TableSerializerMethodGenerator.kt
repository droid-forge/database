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

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName
import javax.lang.model.element.Modifier


class TableSerializerMethodGenerator(
    private val typeDataTypePack: String,
    private val typeDataType: String,
    private val columns: List<Pair<Pair<String, TypeName>, String>>) : CodeGenerator<MethodSpec> {
  init {
   // fileSpec.addImport("android.content", "ContentValues")
  }

  override fun generate(): MethodSpec {

    var stmt =
        "ContentValues values = new ContentValues();\n"
    columns.forEach {
      stmt += generatePutStatement(it.first.first, it.second, it.first.second)
    }

    stmt += """
      return values;
  
    """.trimIndent()

    return MethodSpec.methodBuilder("serialize")
        .addModifiers(Modifier.PUBLIC)
        .addParameter(ClassName.get(typeDataTypePack, typeDataType),"t")
        .addAnnotation(Override::class.java)
        .returns(ClassName.get("android.content", "ContentValues"))
        .addCode(CodeBlock.of(stmt))
        .build()
  }

  /**
   * values.put(nameColumn.getName(), t.getName());
   * values.put(isAdultColumn.getName(), person.isAdult() ? 1 : 0);
   */
  private fun generatePutStatement(typeVariable: String, columnName: String, varTypeName: TypeName): String {
    if (varTypeName.isSameAs(Boolean::class.java)) {
      return "values.put(${columnName}.getName(), t.is${typeVariable.capitalizeFirst()}() ? 1 : 0); \n"
    }
    return "values.put(${columnName}.getName(), t.get${typeVariable.capitalizeFirst()}()); \n"
  }


}