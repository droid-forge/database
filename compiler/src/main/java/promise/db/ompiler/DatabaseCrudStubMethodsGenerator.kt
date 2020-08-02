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

import com.squareup.javapoet.TypeSpec
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

class DatabaseCrudStubMethodsGenerator(
    private val typeSpec: TypeSpec.Builder,
    private val element: Element,
    private val processingEnv: ProcessingEnvironment) : CodeGenerator<String> {

  override fun generate(): String {
    val pack = processingEnv.elementUtils.getPackageOf(element).toString()

    val entities = (element as TypeElement).getTableEntities(processingEnv)

    return "created"
  }

}