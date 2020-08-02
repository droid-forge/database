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

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic


class DatabaseAnnotationGenerator(
    private val processingEnv: ProcessingEnvironment,
    private val element: Element) : CodeGenerator<AnnotationSpec> {
  /*
  @Database(
    tables = [
      PersonFastTable::class
    ],
    version = 2
)
   */
  override fun generate(): AnnotationSpec {
    if (element is TypeElement) {
      var stmt = " {\n"
      var entities: Array<TypeElement>? = null
      try {
        entities = element.getTableEntities(processingEnv)
      } catch (e: Throwable) {
        processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "getTableEntities: ${Utils.getStackTraceString(e)}")
      }
      val version = element.getDatabaseVersion()
      try {
        entities?.forEachIndexed { index, entityClass ->
          val className = entityClass.getClassName()
          //fileBuilder.addImport(pack, className)
          stmt += "$className.class"
          if (index != entities.size - 1) {
            stmt += ", \n"
          }
        }
        stmt += "\n}"
      } catch (e: Throwable) {
        processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "DatabaseAnnotationGenerator: ${Utils.getStackTraceString(e)}")
      }
      return AnnotationSpec.builder(ClassName.get("promise.db", "Database"))
          .addMember("tables", CodeBlock.of(stmt))
          .addMember("version", "$version")
          .build()
    }
    throw IllegalStateException("Element must be a type element")
  }
}