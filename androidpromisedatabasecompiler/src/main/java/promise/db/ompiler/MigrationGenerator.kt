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
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.asClassName
import promise.db.MigrationAction
import promise.db.MigrationActions
import javax.lang.model.element.Element

class MigrationGenerator(
    fileSpec: FileSpec.Builder,
    private val elements: Map<Element, String>) : CodeBlockGenerator<FunSpec?> {
  init {
    fileSpec.addImport("android.content", "ContentValues")
  }

  override fun generate(): FunSpec? {
    val migrateFields = elements.filter {
      it.key.getAnnotation(MigrationAction::class.java) != null
    }
    if (migrateFields.isEmpty()) return null
    val codeBlock = CodeBlock.builder()
        .addStatement("super.onUpgrade(x, v1, v2)")

    migrateFields.forEach {
      val annotation = it.key.getAnnotation(MigrationAction::class.java)
      if (annotation.action == MigrationActions.CREATE) {
        codeBlock.add("if (v1 == ${annotation.from} && v2 == ${annotation.to}) {\n")
        codeBlock.addStatement("addColumns(x, ${it.value})")
        codeBlock.add("}\n")
      }
     else if (annotation.action == MigrationActions.DROP) {
        codeBlock.add("if (v1 == ${annotation.from} && v2 == ${annotation.to}) {\n")
        codeBlock.addStatement("dropColumns(x, ${it.value})")
        codeBlock.add("}\n")
      }
    }

    return FunSpec.builder("onUpgrade")
        .addParameter(ParameterSpec("x", ClassName("android.database.sqlite", "SQLiteDatabase")))
        .addParameter(ParameterSpec("v1", Int::class.asClassName()))
        .addParameter(ParameterSpec("v2", Int::class.asClassName()))
        .addModifiers(KModifier.OVERRIDE)
        .addCode(codeBlock.build())
        .build()
  }

  private fun generatePutStatement(typeVariable: String, columnName: String): String {
    return "put(${columnName}.name, t.${typeVariable}) \n"
  }
}