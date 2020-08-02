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
import com.squareup.javapoet.MethodSpec
import org.jetbrains.annotations.NotNull
import promise.db.Migrate
import promise.db.MigrationOptions
import promise.db.Migrations
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier

class DatabaseMigrationFieldGenerator(
    private val elements: Map<Element, String>) : CodeGenerator<MethodSpec?> {
  init {
    //fileSpec.addImport("android.content", "ContentValues")
  }

  /**
   * @Override
  public void onUpgrade(@NotNull SQLiteDatabase x, int v1, int v2) throws TableError {
  super.onUpgrade(x, v1, v2);
  if (v1 == 3 && v2 == 4) {
  dropColumns(x, ageColumn);
  }
  if (v1 == 4 && v2 == 5) {
  addColumns(x, marksColumn);
  }
  if (v1 == 7 && v2 == 8) {
  dropColumns(x, marksColumn);
  }
  }
   */
  private fun buildIndexMigration(
      codeBlock: CodeBlock.Builder,
      column: String,
      migration: Migrate) {
    if (migration.action == MigrationOptions.CREATE_INDEX) {
      codeBlock.add("if (v1 == ${migration.fromVersion} && v2 == ${migration.toVersion}) {\n")
      codeBlock.indent()
      codeBlock.addStatement("addIndex(x, \"${column}\")")
      codeBlock.unindent()
      codeBlock.add("}\n")
    }
  }
  private fun buildMigration(
      codeBlock: CodeBlock.Builder,
      column: String,
      migration: Migrate) {
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

  override fun generate(): MethodSpec? {
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
        if (migration.action == MigrationOptions.CREATE_INDEX)
          buildIndexMigration(codeBlock, it.key.getNameOfColumn(), migration)
        else buildMigration(codeBlock, it.value, migration)
        return@forEach
      }
      val migrations = it.key.getAnnotation(Migrations::class.java)
      migrations?.values?.forEach { m ->
        buildMigration(codeBlock, it.value, m)
      }
    }

    return MethodSpec.methodBuilder("onUpgrade")
        .addParameter(
            ClassName.get("android.database.sqlite", "SQLiteDatabase")
                .annotated(AnnotationSpec.builder(NotNull::class.java).build()),
            "x")
        .addParameter(Integer::class.javaPrimitiveType, "v1")
        .addParameter(Integer::class.javaPrimitiveType, "v2")
        .addAnnotation(Override::class.java)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addException(ClassName.get("promise.db", "TableError"))
        .addCode(codeBlock.build())
        .build()
  }

}