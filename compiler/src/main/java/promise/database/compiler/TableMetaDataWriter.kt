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

import promise.database.HasMany
import promise.database.Ignore
import promise.database.Migrate
import promise.database.MigrationOptions
import promise.database.Migrations
import promise.database.compiler.migration.Field
import promise.database.compiler.migration.TableMetaData
import promise.database.compiler.migration.TableMigration
import promise.database.compiler.migration.VersionChange
import promise.database.compiler.utils.function.FilterFunction2
import promise.database.compiler.utils.getNameOfColumn
import promise.database.compiler.utils.getTableName
import promise.database.compiler.utils.isColumnNullable
import javax.lang.model.element.Element
import javax.lang.model.util.ElementFilter

class TableMetaDataWriter(
    private val entityElement: Element,
    private val elements: Map<Element, String>) {

  companion object {
    val tableMetaData: ArrayList<TableMetaData> = ArrayList()

    fun finalMaxDbVersion(): Int {
      if (tableMetaData.isNotEmpty()) {
        val versionChanges: ArrayList<VersionChange?> = ArrayList()
        tableMetaData.map { metaData -> metaData.migrations.map { it.versionChange } }
            .forEach {
              versionChanges.addAll(it)
            }
        return versionChanges.mapNotNull { it!!.toVersion }.max() ?: 1
      }
      return 1
    }

    init {
      if (PromiseDatabaseCompiler.databaseMetaDataWriter.currentDatabaseMetaData == null)
        PromiseDatabaseCompiler.databaseMetaDataWriter.loadCurrentMetaData()
    }
  }

  fun process(
      //elements: Map<Element, String>
  ): promise.database.compiler.utils.List<TableMigration> {
    val tableMigrations = promise.database.compiler.utils.List<TableMigration>()

    val migrateFields = elements.filter {
      it.key.getAnnotation(Migrate::class.java) != null ||
          it.key.getAnnotation(Migrations::class.java) != null
    }

    if (migrateFields.isNotEmpty()) migrateFields.forEach { entry ->
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

    val metaData = TableMetaData()
    val oldMetaData = promise.database.compiler.utils.List<TableMetaData>(
        PromiseDatabaseCompiler.databaseMetaDataWriter.currentDatabaseMetaData!!.tableMetaData)
        .find {
          it.tableName == entityElement.getTableName()
        }

    if (oldMetaData != null) {
      tableMigrations.addAll(generateRequiredTableMigrations(
          PromiseDatabaseCompiler.databaseMetaDataWriter.currentDatabaseMetaData!!.dbVersion,
          promise.database.compiler.utils.List(oldMetaData.fields),
          elements.filter {
            it.key.getAnnotation(Migrate::class.java) == null &&
                it.key.getAnnotation(Migrations::class.java) == null
          }))
      tableMigrations.addAll(promise.database.compiler.utils.List(oldMetaData.migrations))
      metaData.migrations = tableMigrations.uniques()
    } else metaData.migrations = tableMigrations

    val fields: ArrayList<Field> = ArrayList()
    /**
     * ignore fields with Has many relation and ones with Ignore annotation
     */
    ElementFilter.fieldsIn(entityElement.enclosedElements).filter {
      it.getAnnotation(Ignore::class.java) == null &&
          it.getAnnotation(HasMany::class.java) == null
    }
        .forEach {
          fields.add(Field().apply {
            fieldName = it.simpleName.toString()
            columnName = it.getNameOfColumn()
            nullable = it.isColumnNullable()
          })
        }

    metaData.fields = fields
    metaData.tableName = entityElement.getTableName()
    tableMetaData.add(metaData)
    return tableMigrations
  }

  private fun generateRequiredTableMigrations(
      oldDatabaseVersion: Int,
      oldFields: promise.database.compiler.utils.List<Field>,
      newFields: Map<Element, String>
  ): List<TableMigration> {
    if (oldFields.isEmpty() && newFields.isEmpty()) return emptyList()
    val newFields2 = promise.database.compiler.utils.List(newFields.toList())
    val migrations: ArrayList<TableMigration> = ArrayList()
    if (oldFields.size > newFields.size) oldFields.reduce(newFields2,
        object : FilterFunction2<String, Pair<Element, String>, Field> {
          override fun filterBy(u: Field): String = u.fieldName
          override fun getKey(t: Pair<Element, String>): String = t.first.simpleName.toString()
        }, false)
        .forEach {
          migrations.add(TableMigration().apply {
            versionChange = VersionChange().apply {
              fromVersion = oldDatabaseVersion
              toVersion = oldDatabaseVersion + 1
            }
            field = "\"${it.columnName}\""
            action = MigrationOptions.DROP
          })
        }
    if (newFields.size > oldFields.size) newFields2.reduce(oldFields,
        object : FilterFunction2<String, Field, Pair<Element, String>> {
          override fun filterBy(u: Pair<Element, String>): String = u.first.simpleName.toString()
          override fun getKey(t: Field): String = t.fieldName
        }, false)
        .forEach {
          migrations.add(TableMigration().apply {
            versionChange = VersionChange().apply {
              fromVersion = oldDatabaseVersion
              toVersion = oldDatabaseVersion + 1
            }
            field = it.second
            action = MigrationOptions.CREATE
          })
        }
    if (newFields2.size == oldFields.size) {
      val sameFieldsMaintained = newFields2.joinOn(oldFields
      ) { t, u -> t.first.simpleName.toString() == u.fieldName }

    }
    return migrations
  }

}