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

package promise.database.compiler

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName
import promise.database.compiler.utils.ConverterTypes
import promise.database.compiler.utils.JavaUtils
import promise.database.compiler.utils.camelCase
import promise.database.compiler.utils.capitalizeFirst
import promise.database.compiler.utils.checkIfHasTypeConverter
import promise.database.compiler.utils.getConverterCompatibleMethod
import promise.database.compiler.utils.isElementAnnotatedAsRelation
import promise.database.compiler.utils.isSameAs
import promise.database.compiler.utils.toTypeName
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier

class TableDeserializerMethodGenerator(
    private val typeDataTypePack: String,
    private val typeDataType: String,
    private val columns: List<Pair<Pair<String, Element>, String>>) : CodeGenerator<MethodSpec> {

  override fun generate(): MethodSpec {
    val codeBlock = CodeBlock.builder()
    val varName = typeDataType.camelCase()
    codeBlock.beginControlFlow("try")
    codeBlock.addStatement("$typeDataType $varName = new $typeDataType()")
    columns.forEach {
      generateSetStatement(
          codeBlock,
          varName,
          it.first.first, it.first.second, it.second)
    }
    codeBlock.addStatement("return $varName")
    codeBlock.endControlFlow()
    JavaUtils.generateCatchSQliteExceptionBlockForDeserializer(codeBlock, typeDataType)
    codeBlock.endControlFlow()

    return MethodSpec.methodBuilder("deserialize")
        .addParameter(ClassName.get("android.database", "Cursor"), "e")
        .addAnnotation(Override::class.java)
        .addModifiers(Modifier.PUBLIC)
        .addJavadoc("Deserializer converts from cursor to the entity")
        .returns(ClassName.get(typeDataTypePack, typeDataType))
        .addCode(codeBlock.build())
        .build()
  }

  private fun generateSetStatement(
      codeBlock: CodeBlock.Builder,
      objectName: String,
      varName: String,
      varType: Element,
      colName: String) {
    if (varType.toTypeName().isSameAs(Boolean::class.java)) {
      codeBlock.addStatement("$objectName.set${varName.capitalizeFirst()}(e.${getCursorReturn(varType.toTypeName())}(${colName}.getIndex(e)) == 1)")
      return
    }
    if (varType.checkIfHasTypeConverter()) {
      val executableFn = varType.getConverterCompatibleMethod(ConverterTypes.DESERIALIZER)
      if (executableFn != null)
        codeBlock.addStatement("$objectName.set${varName.capitalizeFirst()}(typeConverter.${executableFn.simpleName}(e.${getCursorReturn(TypeName.get(String::class.java))}(${colName}.getIndex(e))))")
    } else if (varType.isElementAnnotatedAsRelation()) {
      codeBlock.add(JavaUtils.generateDeserializerRelationSetStatement(objectName, varType, colName))
    } else {
      codeBlock.addStatement("$objectName.set${varName.capitalizeFirst()}(e.${getCursorReturn(varType.toTypeName())}(${colName}.getIndex(e)))")
    }
  }

  private fun getCursorReturn(varType: TypeName): String =
      when {
        varType.isSameAs(Integer::class.java) -> "getInt"
        varType.isSameAs(Long::class.java) -> "getLong"
        varType.isSameAs(Boolean::class.java) -> "getInt"
        varType.isSameAs(Double::class.java) -> "getDouble"
        varType.isSameAs(Float::class.java) -> "getFloat"
        else -> "getString"
      }
}