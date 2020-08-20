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
package promise.db

import android.database.Cursor
import androidx.collection.ArrayMap
import androidx.sqlite.db.SupportSQLiteDatabase
import promise.commons.createInstance
import promise.commons.data.log.LogUtil
import promise.commons.model.Identifiable
import promise.commons.model.List
import promise.commons.util.ClassUtil
import promise.commons.util.Conditions
import promise.database.Table
import promise.model.IdentifiableList
import promise.utils.Visitor
import java.util.*

@Suppress("UNCHECKED_CAST")
open class FastDatabaseImpl constructor(
    name: String?,
    version: Int)
  : FastDatabase(name, version) {

  private var fallBackToDestructiveMigration: Boolean = false

  override fun fallBackToDestructiveMigration() {
    this.fallBackToDestructiveMigration = true
  }

  private val cacheMap: ArrayMap<String, Any> = ArrayMap()

  private var migration: Migration? = null

  private var databaseCreationCallback: DatabaseCreationCallback? = null

  final override fun onCreate(db: SupportSQLiteDatabase) {
    if (databaseCreationCallback != null) {
      databaseCreationCallback!!.beforeCreate(db)
      create(db)
      databaseCreationCallback!!.afterCreate(db)
    } else create(db)
  }

  final override fun onUpgrade(database: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
    try {
      if (newVersion - oldVersion == 1)
        onUpgradeDatabase(database, oldVersion, newVersion)
      else {
        var i = oldVersion
        while (i < newVersion) {
          onUpgradeDatabase(database, i, i + 1)
          i++
        }
      }
      upgradeTables(database, oldVersion, newVersion)
    } catch (e: Throwable) {
      if (fallBackToDestructiveMigration) {
        for (table in Conditions.checkNotNull(tables())) try {
          drop(table, database)
        } catch (tableError: TableError) {
          LogUtil.e(TAG, tableError)
        }
        onCreate(database)
      } else LogUtil.e(TAG, e)
    }
  }

  private fun onUpgradeDatabase(database: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
    if (migration != null) migration!!.onMigrate(this, database, oldVersion, newVersion)
  }

  override fun name(): String = this.databaseName

  private var tables: List<out FastTable<*>>? = null

  protected fun tables(): List<TableCrud<*, in SupportSQLiteDatabase>> =
      Conditions.checkNotNull(tables, "tables not found for this database")
          as List<TableCrud<*, in SupportSQLiteDatabase>>

  override fun <T : TableCrud<*, in SupportSQLiteDatabase>> obtain(tableClass: Class<out TableCrud<*, in SupportSQLiteDatabase>>): T {
    fun <T : TableCrud<*, in SupportSQLiteDatabase>> makeTable(tableClass: Class<out TableCrud<*, in SupportSQLiteDatabase>>): T {
      if (ClassUtil.hasAnnotation(tableClass, Table::class.java)) {
        val table = tableClass.getAnnotation(Table::class.java)!!
        if (cacheMap.containsKey(table.tableName)) return cacheMap[table.tableName] as T
        val tableObject: FastTable<*> = createInstance<FastTable<*>>(clazz = tableClass.kotlin, args = arrayOf(this))
        tableObject.setNameOfTable(table.tableName)
        tableObject.setArgs(ArrayMap<String, Any>().apply {
          put(INDEXES, table.indices)
          put(FOREIGN_kEYS, table.foreignKeys)
          put(COMPOUND_INDEXES, table.compoundIndexes)
        })
        cacheMap[table.tableName] = tableObject
        return tableObject as T
      }
      throw IllegalArgumentException("The class must be annotated with @Table")
    }
    return makeTable(tableClass)
  }

  internal fun <T : FastTable<*>> setTables(tablesClasses: List<out Class<out FastTable<*>>>) {
    this.tables = tablesClasses.map {
      return@map obtain<T>(it)
    }
  }

  internal fun setMigration(migration: Migration?) {
    this.migration = migration
  }

  fun setDatabaseCreationCallback(creationCallback: DatabaseCreationCallback?): FastDatabaseImpl {
    this.databaseCreationCallback = creationCallback
    return this
  }

  private fun create(database: SupportSQLiteDatabase) {
    var created = true
    for (table in Conditions.checkNotNull(tables())) created = try {
      created && create(table, database)
    } catch (dbError: DBError) {
      LogUtil.e(TAG, dbError)
      return
    }
  }

  private fun upgradeTables(database: SupportSQLiteDatabase, v1: Int, v2: Int) {
    for (table in Conditions.checkNotNull(tables())) try {
      if (v2 - v1 == 1)
        checkTableExist(table).onUpgrade(database, v1, v2)
      else {
        var i = v1
        while (i < v2) {
          checkTableExist(table).onUpgrade(database, i, i + 1)
          i++
        }
      }
    } catch (tableError: TableError) {
      LogUtil.e(TAG, tableError)
    }
  }

  @SafeVarargs
  override fun add(database: SupportSQLiteDatabase, vararg tables: TableCrud<*, in SupportSQLiteDatabase>): Boolean {
    var created = true
    for (table in tables) {
      created = try {
        created && create(table, database)
      } catch (dbError: DBError) {
        LogUtil.e(TAG, dbError)
        return false
      }
    }
    return created
  }

  @Throws(DBError::class)
  private fun create(tableCrud: TableCrud<*, in SupportSQLiteDatabase>, database: SupportSQLiteDatabase): Boolean {
    try {
      tableCrud.onCreate(database)
    } catch (e: TableError) {
      throw DBError(e)
    }
    return true
  }

  @Throws(DBError::class)
  private fun drop(tableCrud: TableCrud<*, in SupportSQLiteDatabase>, database: SupportSQLiteDatabase): Boolean {
    try {
      checkTableExist(tableCrud).onDrop(database)
    } catch (e: TableError) {
      throw DBError(e)
    }
    return true
  }

  override fun writableDatabase(): SupportSQLiteDatabase = writableDatabase

  override fun querySql(sql: String): Cursor {
    LogUtil.d(TAG, "query: $sql")
    return readableDatabase.query(sql, null)
  }

  override fun query(queryBuilder: QueryBuilder): Cursor {
    val sql = queryBuilder.build()
    val params = queryBuilder.buildParameters()
    LogUtil.d(TAG, "query: $sql", " params: " + Arrays.toString(params))
    return readableDatabase.query(sql, params)
  }

  override fun <T : Identifiable<Int>> find(tableCrud: TableCrud<T, in SupportSQLiteDatabase>): TableCrud.Extras<T> =
      checkTableExist(tableCrud).accept(FetchExtrasVisitor(readableDatabase)) as TableCrud.Extras<T>
  //return checkTableExist(tableCrud).onFind(readableDatabase)

  override fun <T : Identifiable<Int>> findAll(tableCrud: TableCrud<T, in SupportSQLiteDatabase>): IdentifiableList<out T> =
      checkTableExist(tableCrud).accept(FetchAllVisitor(readableDatabase, null)) as IdentifiableList<out T>

  override fun <T : Identifiable<Int>> update(t: T, tableCrud: TableCrud<T, in SupportSQLiteDatabase>): Boolean =
      checkTableExist(tableCrud).accept(UpdateVisitor(writableDatabase, t, null)) as Boolean

  override fun <T : Identifiable<Int>> update(t: T, tableCrud: TableCrud<T, in SupportSQLiteDatabase>, column: Column<*>): Boolean =
      try {
        checkTableExist(tableCrud).accept(UpdateVisitor(writableDatabase, t, column)) as Boolean
      } catch (tableError: TableError) {
        LogUtil.e(TAG, "update error", tableError)
        false
      }

  override fun <T : Identifiable<Int>> findAll(tableCrud: TableCrud<T, in SupportSQLiteDatabase>,
                                               vararg columns: Column<*>): IdentifiableList<out T> =
      checkTableExist(tableCrud).accept(FetchAllVisitor(readableDatabase, columns)) as IdentifiableList<out T>

  override fun <T : Identifiable<Int>> delete(tableCrud: TableCrud<T, in SupportSQLiteDatabase>, t: T): Boolean =
      checkTableExist(tableCrud).accept(DeleteVisitor(writableDatabase, t)) as Boolean

  override fun delete(tableCrud: TableCrud<*, in SupportSQLiteDatabase>, column: Column<*>): Boolean =
      checkTableExist(tableCrud).acceptErasure(DeleteErasureVisitor(writableDatabase, column)) as Boolean

  override fun delete(tableCrud: TableCrud<*, in SupportSQLiteDatabase>): Boolean =
      checkTableExist(tableCrud).acceptErasure(DeleteErasureVisitor(writableDatabase)) as Boolean

  @SafeVarargs
  override fun delete(vararg tableCruds: TableCrud<*, in SupportSQLiteDatabase>): Boolean {
    var delete = true
    for (table in tableCruds) delete = delete && delete(table)
    return delete
  }

  override fun <T> delete(tableCrud: TableCrud<*, in SupportSQLiteDatabase>, column: Column<T>, list: List<out T>): Boolean =
      checkTableExist(tableCrud).acceptErasure(DeleteListErasureVisitor(writableDatabase, column, list)) as Boolean

  override fun <T : Identifiable<Int>> save(t: T, tableCrud: TableCrud<T, in SupportSQLiteDatabase>): Long =
      checkTableExist(tableCrud).accept(SaveVisitor(UpdateVisitor(writableDatabase, t, null), writableDatabase, t)) as Long

  override fun <T : Identifiable<Int>> save(list: IdentifiableList<out T>, tableCrud: TableCrud<T, in SupportSQLiteDatabase>): Boolean =
      checkTableExist(tableCrud).accept(SaveListVisitor(writableDatabase, list)) as Boolean

  override fun deleteAll(): Boolean = synchronized(FastDatabaseImpl::class.java) {
    var deleted = true
    try {
      writableDatabase.execSQL("PRAGMA foreign_keys = FALSE")
      transact {
        for (table in Conditions.checkNotNull(tables())) deleted = deleted && delete(checkTableExist(table))
      }
      writableDatabase.execSQL("PRAGMA foreign_keys = TRUE")
    } catch (e: Exception) {
    }
    return deleted
  }

  override fun getLastId(tableCrud: TableCrud<*, in SupportSQLiteDatabase>): Int =
      checkTableExist(tableCrud).acceptErasure(FetchLastIdVisitor(readableDatabase)) as Int

  override fun transact(block: FastDatabase.() -> Unit) = synchronized(this) {
    val db = writableDatabase
    try {
      db.beginTransaction()
      block.invoke(this)
      db.setTransactionSuccessful()
    } finally {
      db.endTransaction()
    }
  }

  override fun <R : Any> accept(visitor: Visitor<FastDatabase, R>): R = visitor.visit(this)

  private fun <T : Identifiable<Int>> checkTableExist(tableCrud: TableCrud<T, in SupportSQLiteDatabase>): TableCrud<T, in SupportSQLiteDatabase> =
      Conditions.checkNotNull(tableCrud)

  companion object {
    private val TAG: String = LogUtil.makeTag(FastDatabase::class.java)
  }

  init {
    LogUtil.d(TAG, "fast db init")
  }
}