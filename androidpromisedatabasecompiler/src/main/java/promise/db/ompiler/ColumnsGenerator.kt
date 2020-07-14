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
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import promise.db.PrimaryKeyAutoIncrement
import java.util.*
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.tools.Diagnostic
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ColumnsGenerator(fileSpec: FileSpec.Builder,
                       private val processingEnvironment: ProcessingEnvironment
                       ,
                       private val setElements: List<Element>) : CodeBlockGenerator<Map<Pair<Element, String>, PropertySpec>> {

  var genColValues: ArrayList<Pair<Pair<String, TypeName>, String>> = ArrayList()

  init {
    fileSpec.addImport("promise.db", "Column")
  }

  override fun generate(): Map<Pair<Element, String>, PropertySpec> {
    val map = HashMap<Pair<Element, String>, PropertySpec>()
    filterPrimitiveElements(setElements.filter {
      it.kind.isField
    }).forEachIndexed { i, element ->
      if (element.getAnnotation(PrimaryKeyAutoIncrement::class.java) != null) return@forEachIndexed
      val colVariableName = "${element.simpleName}Column"
      var variableClassType: TypeName? = null
      variableClassType = element.asType().asTypeName()

      val nameOfColumn = getNameOfColumn(element)
      if (nameOfColumn == "id") return@forEachIndexed

      val parameterizedColumnTypeName = ClassName("promise.db", "Column")
          .parameterizedBy(variableClassType)
      val columnInitializer = getColumnInitializer(element)

      val spec = PropertySpec.builder(colVariableName, parameterizedColumnTypeName)
          .initializer(CodeBlock.of("""
              Column<%T>("$nameOfColumn", $columnInitializer, %L)
            """.trimIndent(), variableClassType, i + 1)
          )
          .build()
      map[Pair(element, colVariableName)] = spec
      val pair: Pair<Pair<String, TypeName>, String> = Pair(Pair(element.simpleName.toString(), variableClassType), colVariableName)
      genColValues.add(pair)
    }

    return map
  }

  private fun getNameOfColumn(element: Element): String {
    //processingEnvironment.messager.printMessage(Diagnostic.Kind.ERROR, "elemtype: "+element.asType().asTypeName().toString()+"\n")
    //processingEnvironment.messager.printMessage(Diagnostic.Kind.ERROR, "class type: "+Int::class.java.name+"\n")
    var name: String? = null
    if ((element.asType().asTypeName().isSameAs(Int::class.boxedJava()) ||
            element.asType().asTypeName().isSameAs(Float::class.boxedJava()) ||
            element.asType().asTypeName().isSameAs(Double::class.boxedJava()) ||
            element.asType().asTypeName().isSameAs(Boolean::class.boxedJava())) &&
        element.getAnnotation(promise.db.Number::class.java) != null) {
      name = element.getAnnotation(promise.db.Number::class.java).name
    } else if (element.asType().asTypeName().isSameAs(String::class.boxedJava()) && element.getAnnotation(promise.db.Varchar::class.java) != null) {
      name = element.getAnnotation(promise.db.Varchar::class.java).name
    }
    if (name != null && name.isNotEmpty()) return name
    return element.simpleName.toString()
  }

  private fun getColumnInitializer(element: Element): String {
    var str = "Column.Type"
    if (element.asType().asTypeName().isSameAs(Int::class.boxedJava()) ||
        element.asType().asTypeName().isSameAs(Float::class.boxedJava()) ||
        element.asType().asTypeName().isSameAs(Double::class.boxedJava()) ||
        element.asType().asTypeName().isSameAs(Boolean::class.boxedJava())) {
      str += ".INTEGER"
      if (element.getAnnotation(promise.db.Number::class.java) != null) {
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
    else if (element.asType().asTypeName().isSameAs(String::class.boxedJava())) {
      if (element.getAnnotation(promise.db.Varchar::class.java) != null) {
        str += ".VARCHAR"
        val annotation = element.getAnnotation(promise.db.Varchar::class.java)
        str += if (annotation.nullable && annotation.length != 0) {
          ".NULLABLE(${annotation.length})"
        } else if (!annotation.nullable && annotation.length != 0) {
          ".NOT_NULL(${annotation.length})"
        } else {
          throw IllegalStateException("element ${element.simpleName} is annotated as varchar without length")
        }
      } else {
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
        } else {
          str += ".TEXT.NULLABLE()"
        }
      }
    }
    return str
  }

  private fun filterPrimitiveElements(elements: List<Element>): List<Element> =  elements.filter {
    try {
      it.asType().asTypeName().isSameAs(Int::class.boxedJava()) ||
          it.asType().asTypeName().isSameAs(String::class.boxedJava())  ||
          it.asType().asTypeName().isSameAs(Float::class.boxedJava()) ||
          it.asType().asTypeName().isSameAs(Double::class.boxedJava()) ||
          it.asType().asTypeName().isSameAs(Boolean::class.boxedJava())
    } catch (e: Throwable) {
      processingEnvironment.messager.printMessage(Diagnostic.Kind.ERROR,
          "FilterPrimitiveElement ${it.kind.name}: ${Arrays.toString(e.stackTrace)}")
      false
    }
  }
}