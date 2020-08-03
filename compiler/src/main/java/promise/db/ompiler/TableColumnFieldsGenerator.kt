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
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import promise.db.Ignore
import promise.db.PrimaryKey
import promise.db.PrimaryKeyAutoIncrement
import java.util.*
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.tools.Diagnostic
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * generates column fields for the entity
 */
class TableColumnFieldsGenerator(
    private val processingEnvironment: ProcessingEnvironment,
    private val setElements: List<Element>) : CodeGenerator<Map<Pair<Element, String>, FieldSpec>> {

  /**
   *
   */
  var genColValues: ArrayList<Pair<Pair<String, TypeName>, String>> = ArrayList()

  override fun generate(): Map<Pair<Element, String>, FieldSpec> {
    val map = HashMap<Pair<Element, String>, FieldSpec>()
    setElements.filter {
      it.kind.isField
    }.forEachIndexed { i, element ->
      if (element.getAnnotation(PrimaryKeyAutoIncrement::class.java) != null ||
          element.getAnnotation(Ignore::class.java) != null) return@forEachIndexed
      val nameOfColumn = element.getNameOfColumn()
      if (nameOfColumn == "id") return@forEachIndexed
      val colVariableName = "${element.simpleName}Column"
      var variableClassType = element.toTypeName()
      if (variableClassType.isPrimitive) variableClassType = variableClassType.box()
      if (!element.isPersistable() && !element.isElementAnnotatedAsRelation()) {
        if (element.checkIfHasTypeConverter(processingEnvironment)) {
          val parameterizedColumnTypeName = ParameterizedTypeName.get(
              ClassName.get("promise.db", "Column"),
              TypeName.get(String::class.java))
          val columnInitializer = getColumnInitializer(element, ClassName.get(String::class.java))
          val spec = FieldSpec.builder(parameterizedColumnTypeName, colVariableName)
              .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
              .initializer(CodeBlock.of("""
              new Column<String>("$nameOfColumn", $columnInitializer, ${i + 1})
            """.trimIndent())
              )
              .build()
          map[Pair(element, colVariableName)] = spec
          val pair: Pair<Pair<String, TypeName>, String> = Pair(Pair(element.simpleName.toString(), variableClassType), colVariableName)
          genColValues.add(pair)
        }
      } else if(element.isPersistable()) {
        val spec = processField(element, nameOfColumn, i)
        map[Pair(element, colVariableName)] = spec
        val pair: Pair<Pair<String, TypeName>, String> = Pair(Pair(element.simpleName.toString(), variableClassType), colVariableName)
        genColValues.add(pair)
      }
    }

    return map
  }

  /**
   * generates field for the column type of this particular field
   */
  private fun processField(element: Element, nameOfColumn: String, i: Int): FieldSpec {
    val colVariableName = "${element.simpleName}Column"
    var variableClassType = element.toTypeName()
    if (variableClassType.isPrimitive) variableClassType = variableClassType.box()
    val parameterizedColumnTypeName = ParameterizedTypeName.get(
        ClassName.get("promise.db", "Column"),
        variableClassType)
    val columnInitializer = getColumnInitializer(element, variableClassType)
    return FieldSpec.builder(parameterizedColumnTypeName, colVariableName)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .initializer(CodeBlock.of("""
              new Column<${variableClassType}>("$nameOfColumn", $columnInitializer, ${i + 1})
            """.trimIndent())
        )
        .build()
  }

  /**
   * returns column type initializer for this column type
   */
  private fun getColumnInitializer(element: Element, classTypeName: TypeName): String {
    var str = "Column.Type"
    if (classTypeName.isSameAs(Integer::class.java) ||
        classTypeName.isSameAs(Integer::class.java) ||
        classTypeName.isSameAs(Float::class.java) ||
        classTypeName.isSameAs(Double::class.java) ||
        classTypeName.isSameAs(Boolean::class.java)) {
      str += ".INTEGER"
      if (element.getAnnotation(PrimaryKey::class.java) != null) {
        str += ".PRIMARY_KEY()"
      } else if (element.getAnnotation(promise.db.Number::class.java) != null) {
        val annotation = element.getAnnotation(promise.db.Number::class.java)
        if (annotation.default != 0) {
          str += ".DEFAULT(${annotation.default})"
        } else {
          if (annotation.nullable) {
            str += ".NULLABLE()"
          } else if (!annotation.nullable) {
            str += ".NON_NULL()"
          }
        }
      } else {
        str += ".NULLABLE()"
      }
    }
    else if (classTypeName.isSameAs(String::class.java)) {
      if (element.getAnnotation(promise.db.VarChar::class.java) != null) {
        str += ".VARCHAR"
        val annotation = element.getAnnotation(promise.db.VarChar::class.java)
        str += if (annotation.unique && annotation.length != 0) {
          ".UNIQUE(${annotation.length})"
        } else if (annotation.nullable && annotation.length != 0) {
          ".NULLABLE(${annotation.length})"
        } else if (!annotation.nullable && annotation.length != 0) {
          ".NOT_NULL(${annotation.length})"
        } else {
          throw IllegalStateException("element ${element.simpleName} is annotated as varchar without length")
        }
      }
      else {
        if (element.getAnnotation(promise.db.Text::class.java) != null) {
          str += ".TEXT"
          val annotation = element.getAnnotation(promise.db.Text::class.java)
          str += if (annotation.nullable) {
            ".NULLABLE()"
          } else if (!annotation.nullable) {
            ".NOT_NULL()"
          } else {
            throw IllegalStateException("element ${element.simpleName} is annotated as varchar without length")
          }
        }
        else {
          str += ".TEXT.NULLABLE()"
        }
      }
    }
    else {
      if (element.getAnnotation(promise.db.VarChar::class.java) != null) {
        str += ".VARCHAR"
        val annotation = element.getAnnotation(promise.db.VarChar::class.java)
        str += if (annotation.unique && annotation.length != 0) {
          ".UNIQUE(${annotation.length})"
        } else if (annotation.nullable && annotation.length != 0) {
          ".NULLABLE(${annotation.length})"
        } else if (!annotation.nullable && annotation.length != 0) {
          ".NOT_NULL(${annotation.length})"
        } else {
          throw IllegalStateException("element ${element.simpleName} is annotated as varchar without length")
        }
      }
      else {
        if (element.getAnnotation(promise.db.Text::class.java) != null) {
          str += ".TEXT"
          val annotation = element.getAnnotation(promise.db.Text::class.java)
          str += if (annotation.nullable) {
            ".NULLABLE()"
          } else if (!annotation.nullable) {
            ".NOT_NULL()"
          } else {
            throw IllegalStateException("element ${element.simpleName} is annotated as varchar without length")
          }
        }
        else {
          str += ".TEXT.NULLABLE()"
        }
      }
    }
    return str
  }




}