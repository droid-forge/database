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
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.WildcardTypeName
import org.jetbrains.annotations.NotNull
import javax.lang.model.element.Modifier

class TableRegisteredColumnsMethodGenerator(
    private val columns: List<String>) : CodeGenerator<MethodSpec> {
  override fun generate(): MethodSpec {
    var stmt = "return List.fromArray("
    columns.forEachIndexed { index, s ->
      stmt += s
      if (index != columns.size - 1) stmt += ", "
    }
    stmt += ");"
    return MethodSpec.methodBuilder("getColumns")
        .returns(
            ParameterizedTypeName.get(ClassName.get("promise.commons.model", "List"),
                WildcardTypeName.subtypeOf(
                    ParameterizedTypeName.get(ClassName.get("promise.db", "Column"),
                        WildcardTypeName.subtypeOf(TypeName.OBJECT)
                    )
                )
            )
        )
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addAnnotation(Override::class.java)
        .addAnnotation(NotNull::class.java)
        .addCode(CodeBlock.of(stmt))
        .build()
  }
}