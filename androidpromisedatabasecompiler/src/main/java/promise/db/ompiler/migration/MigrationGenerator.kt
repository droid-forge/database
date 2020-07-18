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

package promise.db.ompiler.migration

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.asClassName
import promise.db.Migrate
import promise.db.MigrationOptions
import promise.db.Migrations
import promise.db.ompiler.CodeBlockGenerator
import javax.lang.model.element.Element

class MigrationGenerator(
    fileSpec: FileSpec.Builder,
    private val elements: Map<Element, String>) : CodeBlockGenerator<FunSpec?> {
  init {
    fileSpec.addImport("android.content", "ContentValues")
  }

  private fun buildMigration(codeBlock: CodeBlock.Builder, column: String, migration: Migrate) {
    if (migration.action == MigrationOptions.CREATE) {
      codeBlock.add("if (v1 == ${migration.fromVersion} && v2 == ${migration.toVersion}) {\n")
      codeBlock.indent()
      codeBlock.addStatement("addColumns(x, ${column})")
      codeBlock.unindent()
      codeBlock.add("}\n")
    } else if (migration.action == MigrationOptions.DROP) {
      codeBlock.add("if (v1 == ${migration.fromVersion} && v2 == ${migration.toVersion}) {\n")
      codeBlock.indent()
      codeBlock.addStatement("dropColumns(x, ${column})")
      codeBlock.unindent()
      codeBlock.add("}\n")
    }
  }

  override fun generate(): FunSpec? {
    val migrateFields = elements.filter {
      it.key.getAnnotation(Migrate::class.java) != null ||
          it.key.getAnnotation(Migrations::class.java) != null
    }
    if (migrateFields.isEmpty()) return null
    val codeBlock = CodeBlock.builder()
        .addStatement("super.onUpgrade(x, v1, v2)")

    migrateFields.forEach {
      val migration = it.key.getAnnotation(Migrate::class.java)
      if (migration != null) {
        buildMigration(codeBlock, it.value, migration)
        return@forEach
      }
      val migrations = it.key.getAnnotation(Migrations::class.java)
      migrations?.values?.forEach { m ->
        buildMigration(codeBlock, it.value, m)
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

}