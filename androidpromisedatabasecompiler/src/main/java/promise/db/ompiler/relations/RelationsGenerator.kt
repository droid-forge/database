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

package promise.db.ompiler.relations

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import promise.db.ManyToOne
import promise.db.OneToOne
import promise.db.ompiler.CodeBlockGenerator
import java.util.*
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.tools.Diagnostic
import kotlin.collections.ArrayList

class RelationsGenerator(
    fileSpec: FileSpec.Builder,
    private val processingEnvironment: ProcessingEnvironment
    ,
    private val setElements: List<Element>
) : CodeBlockGenerator<List<FunSpec>?> {

  override fun generate(): List<FunSpec>? {
    val funSpecs = ArrayList<FunSpec>()
    filterNotPrimitiveElements(setElements.filter {
      it.kind.isField
    }).also {
      if (it.isEmpty()) return null
    }.forEach {

    }
    return funSpecs
  }

  private fun getNameOfColumn(element: Element): String {
    return element.simpleName.toString()
  }

  private fun filterNotPrimitiveElements(elements: List<Element>): List<Element> = elements.filter {
    try {
      it.getAnnotation(OneToOne::class.java) != null ||
          it.getAnnotation(ManyToOne::class.java) != null
    } catch (e: Throwable) {
      processingEnvironment.messager.printMessage(Diagnostic.Kind.ERROR,
          "FilterPrimitiveElement ${it.kind.name}: ${Arrays.toString(e.stackTrace)}")
      true
    }
  }
}