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

import com.squareup.javapoet.JavaFile
import promise.database.Entity
import promise.database.compiler.utils.isElementAnnotatedAsRelation
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter

class RelationsDaoProcessor(private val processingEnv: ProcessingEnvironment) : AnnotatedClassProcessor() {
  override fun process(environment: RoundEnvironment?): List<JavaFile.Builder?>? {
    val javaFiles = ArrayList<JavaFile.Builder?>()
    environment?.getElementsAnnotatedWith(Entity::class.java)
        ?.forEach { element ->
          javaFiles.addAll(processElementDaos(element))
        }
    return javaFiles
  }

  private fun processElementDaos(element: Element): List<JavaFile.Builder?> {
    val javaFiles = ArrayList<JavaFile.Builder?>()
    val fields = ElementFilter.fieldsIn(element.enclosedElements).filter {
      it.isElementAnnotatedAsRelation()
    }
    if (fields.isNotEmpty())
      javaFiles.addAll(RelationsDaoGenerator(processingEnv, element as TypeElement, fields).generate())
    return javaFiles
  }
}