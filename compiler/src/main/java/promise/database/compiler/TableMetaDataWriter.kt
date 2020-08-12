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

import promise.database.Ignore
import promise.database.Migrate
import promise.database.MigrationOptions
import promise.database.Migrations
import promise.database.compiler.migration.Field
import promise.database.compiler.migration.TableMetaData
import promise.database.compiler.migration.TableMigration
import promise.database.compiler.migration.VersionChange
import promise.database.compiler.utils.getNameOfColumn
import promise.database.compiler.utils.getTableName
import javax.lang.model.element.Element
import javax.lang.model.util.ElementFilter

class TableMetaDataWriter(
    private val entityElement: Element,
    private val elements: Map<Element, String>) {

  companion object {
    val tableMetaData: ArrayList<TableMetaData> = ArrayList()
    init {
      if (PromiseDatabaseCompiler.databaseMetaDataWriter.currentDatabaseMetaData == null)
        PromiseDatabaseCompiler.databaseMetaDataWriter.loadCurrentMetaData()
    }
  }

  fun process(
      //elements: Map<Element, String>
  ): promise.database.compiler.utils.List<TableMigration> {
    val migrateFields = elements.filter {
      it.key.getAnnotation(Migrate::class.java) != null ||
          it.key.getAnnotation(Migrations::class.java) != null
    }

    val tableMigrations = promise.database.compiler.utils.List<TableMigration>()
    if (migrateFields.isEmpty()) {

    }
    else {
      migrateFields.forEach { entry ->
        if (entry.key.getAnnotation(Migrate::class.java) != null) {
          val migration = entry.key.getAnnotation(Migrate::class.java)
          tableMigrations.add(TableMigration().apply {
            versionChange = VersionChange().apply {
              fromVersion = migration.fromVersion
              toVersion = migration.toVersion
            }
            field = if (migration.action == MigrationOptions.CREATE_INDEX) entry.key.getNameOfColumn() else entry.value
            action = migration.action
          })
        } else if (entry.key.getAnnotation(Migrations::class.java) != null) {
          val migrations = entry.key.getAnnotation(Migrations::class.java)
          migrations.values.forEach {
            val migration = it
            tableMigrations.add(TableMigration().apply {
              versionChange = VersionChange().apply {
                fromVersion = migration.fromVersion
                toVersion = migration.toVersion
              }
              field = if (migration.action == MigrationOptions.CREATE_INDEX) entry.key.getNameOfColumn() else entry.value
              action = migration.action
            })
          }
        }
      }
    }
    val metaData = TableMetaData()
    val oldMetaData = promise.database.compiler.utils.List<TableMetaData>(PromiseDatabaseCompiler.databaseMetaDataWriter.currentDatabaseMetaData!!.tableMetaData)
        .find {
          it.tableName == entityElement.getTableName()
        }
    if (oldMetaData != null) {
      val diffMigrations= promise.database.compiler.utils.List(oldMetaData.migrations).joinOn(
          promise.database.compiler.utils.List(tableMigrations)) { t, u -> t != u }
      tableMigrations.addAll(diffMigrations)
      metaData.migrations = tableMigrations
    }
    else metaData.migrations = tableMigrations
    val fields: ArrayList<Field> = ArrayList()
    ElementFilter.fieldsIn(entityElement.enclosedElements).filter {
      it.getAnnotation(Ignore::class.java) == null
    }
        .forEach {
          fields.add(Field().apply {
            fieldName = it.simpleName.toString()
            columnName = it.getNameOfColumn()
          })
        }

    metaData.fields = fields
    metaData.tableName = entityElement.getTableName()
    tableMetaData.add(metaData)
    return tableMigrations
  }

}