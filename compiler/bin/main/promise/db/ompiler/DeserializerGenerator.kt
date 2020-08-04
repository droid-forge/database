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

class DeserializerGenerator(
    private val typeDataTypePack: String,
    private val typeDataType: String,
    private val columns: List<Pair<Pair<String, TypeName>, String>>) : CodeBlockGenerator<MethodSpec> {

  init {
    //fileSpec.addImport(ClassName("android.database", "Cursor"))
  }

  override fun generate(): MethodSpec {
    var stmt = """
      try {
        $typeDataType v1 = new $typeDataType();
        
    """.trimIndent()
    columns.forEach {
      stmt += generateSetStatement(it.first.first, it.first.second, it.second)
    }
    stmt += """
      return v1;      
    } catch(android.database.CursorIndexOutOfBoundsException ex) {
      promise.commons.data.log.LogUtil.e(TAG, "deserialize", ex);
      return new $typeDataType();
    }
    """.trimIndent()

    //fileSpec.addImport("android.database", "CursorIndexOutOfBoundsException")

    return MethodSpec.methodBuilder("deserialize")
        .addParameter(ClassName.get("android.database", "Cursor"), "e")
        .addAnnotation(Override::class.java)
        .addModifiers(Modifier.PUBLIC)
        .returns(ClassName.get(typeDataTypePack, typeDataType))
        .addCode(CodeBlock.of(stmt))
        .build()
  }

  /**
   * person.setAge(cursor.getInt(ageColumn.getIndex(cursor)));
   */
  private fun generateSetStatement(varName: String, varType: TypeName, colName: String): String {
    if (varType.isSameAs(Boolean::class.java)) {
      return " v1.set${capitalizeFirst(varName)}(e.${getCursorReturn(varType)}(${colName}.getIndex(e)) == 1);\n"
    }
    return " v1.set${capitalizeFirst(varName)}(e.${getCursorReturn(varType)}(${colName}.getIndex(e)));\n"
  }

  private fun capitalizeFirst(varname: String): String {
    return varname.replace(varname.first(), varname.first().toUpperCase())
  }

  private fun getCursorReturn(varType: TypeName): String {
    //processingEnvironment.messager.printMessage(Diagnostic.Kind.ERROR, "type ${varType.toString()}")
    return when {
      varType.isSameAs(Integer::class.java) -> "getInt"
      varType.isSameAs(Boolean::class.java) -> "getInt"
      varType.isSameAs(Double::class.java) -> "getDouble"
      varType.isSameAs(Float::class.java) -> "getFloat"
      else -> "getString"
    }
  }

}