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

import android.annotation.TargetApi
import android.database.Cursor
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import androidx.collection.ArrayMap
import promise.commons.createInstance
import promise.commons.data.log.LogUtil
import promise.commons.model.Identifiable
import promise.commons.model.List
import promise.commons.util.ClassUtil
import promise.commons.util.Conditions
import promise.model.IdentifiableList
import java.util.*

open class FastDatabaseImpl internal constructor(
    name: String?,
    factory: SQLiteDatabase.CursorFactory?,
    version: Int,
    errorHandler: DatabaseErrorHandler)
  : FastDatabase(name, factory, version, errorHandler) {

  private val cacheMap: ArrayMap<String, Any> = ArrayMap()

  private var migration: Migration? = null

  private var databaseCreationCallback: DatabaseCreationCallback? = null

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  private constructor(name: String?,
                      version: Int,
                      cursorListener: DatabaseCursorFactory.Listener?,
                      listener: Corrupt?) : this(
      name,
      DatabaseCursorFactory(cursorListener),
      version,
      DatabaseErrorHandler {
        assert(listener != null)
        listener!!.onCorrupt()
      })

  constructor(name: String?, version: Int) : this(name, version, null, null)

  final override fun onCreate(db: SQLiteDatabase) {
    if (databaseCreationCallback != null) {
      databaseCreationCallback!!.beforeCreate(db)
      create(db)
      databaseCreationCallback!!.afterCreate(db)
    } else create(db)
  }

  final override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    LogUtil.d(TAG, "onUpgrade", oldVersion, newVersion)
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
  }

  private fun onUpgradeDatabase(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    if (migration != null) migration!!.onMigrate(this, database, oldVersion, newVersion)
  }

  override fun name(): String = this.databaseName

  private var tables: List<out FastTable<*>>? = null

  protected fun tables(): List<TableCrud<*, in SQLiteDatabase>> =
      Conditions.checkNotNull(tables, "tables not found for this database")
          as List<TableCrud<*, in SQLiteDatabase>>

  override fun <T : TableCrud<*, in SQLiteDatabase>> obtain(tableClass: Class<out TableCrud<*, in SQLiteDatabase>>): T {
    fun <T : TableCrud<*, in SQLiteDatabase>> makeTable(tableClass: Class<out TableCrud<*, in SQLiteDatabase>>): T {
      if (ClassUtil.hasAnnotation(tableClass, Table::class.java)) {
        val table = tableClass.getAnnotation(Table::class.java)!!
        if (cacheMap.containsKey(table.tableName)) return cacheMap[table.tableName] as T
        val tableObject: FastTable<*> = createInstance<FastTable<*>>(tableClass.kotlin, arrayOf(this))
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

  private fun create(database: SQLiteDatabase) {
    var created = true
    for (table in Conditions.checkNotNull(tables())) created = try {
      created && create(table, database)
    } catch (dbError: DBError) {
      LogUtil.e(TAG, dbError)
      return
    }
  }

  private fun upgradeTables(database: SQLiteDatabase, v1: Int, v2: Int) {
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
  override fun add(database: SQLiteDatabase, vararg tables: TableCrud<*, in SQLiteDatabase>): Boolean {
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

  private fun drop(database: SQLiteDatabase): Boolean {
    /*for (Map.Entry<IndexCreated, Table<, SQLiteDatabase>> entry :
        indexCreatedTableHashMap.entrySet()) {
      try {
        dropped = dropped && drop(checkTableExist(entry.getValue()), database);
      } catch (DBError dbError) {
        dbError.printStackTrace();
        return false;
      }
    }*/return true
  }

  @Throws(DBError::class)
  private fun create(tableCrud: TableCrud<*, in SQLiteDatabase>, database: SQLiteDatabase): Boolean {
    try {
      tableCrud.onCreate(database)
    } catch (e: TableError) {
      throw DBError(e)
    }
    return true
  }

  @Throws(DBError::class)
  private fun drop(tableCrud: TableCrud<*, in SQLiteDatabase>, database: SQLiteDatabase): Boolean {
    try {
      checkTableExist(tableCrud).onDrop(database)
    } catch (e: TableError) {
      throw DBError(e)
    }
    return true
  }

  override fun writableDatabase(): SQLiteDatabase = writableDatabase

  override fun querySql(sql: String): Cursor {
    LogUtil.d(TAG, "query: $sql")
    return readableDatabase.rawQuery(sql, null)
  }

  override fun query(queryBuilder: QueryBuilder): Cursor {
    val sql = queryBuilder.build()
    val params = queryBuilder.buildParameters()
    LogUtil.d(TAG, "query: $sql", " params: " + Arrays.toString(params))
    return readableDatabase.rawQuery(sql, params)
  }

  override fun <T : Identifiable<Int>> find(tableCrud: TableCrud<T, in SQLiteDatabase>): TableCrud.Extras<T> =
      checkTableExist(tableCrud).onFind(readableDatabase)

  override fun <T : Identifiable<Int>> findAll(tableCrud: TableCrud<T, in SQLiteDatabase>): IdentifiableList<out T> =
      checkTableExist(tableCrud).onFindAll(readableDatabase, true)

  override fun <T : Identifiable<Int>> update(t: T, tableCrud: TableCrud<T, in SQLiteDatabase>): Boolean =
      checkTableExist(tableCrud).onUpdate(t, writableDatabase)

  override fun <T : Identifiable<Int>> update(t: T, tableCrud: TableCrud<T, in SQLiteDatabase>, column: Column<*>): Boolean =
      try {
        checkTableExist(tableCrud).onUpdate(t, writableDatabase, column)
      } catch (tableError: TableError) {
        LogUtil.e(TAG, "update error", tableError)
        false
      }

  override fun <T : Identifiable<Int>> findAll(tableCrud: TableCrud<T, in SQLiteDatabase>,
                                               vararg columns: Column<*>): IdentifiableList<out T> =
      checkTableExist(tableCrud).onFindAll(readableDatabase, *columns)

  override fun <T : Identifiable<Int>> delete(tableCrud: TableCrud<T, in SQLiteDatabase>, t: T): Boolean =
      checkTableExist(tableCrud).onDelete(t, writableDatabase)

  override fun delete(tableCrud: TableCrud<*, in SQLiteDatabase>, column: Column<*>): Boolean =
      checkTableExist(tableCrud).onDelete(writableDatabase, column)

  override fun delete(tableCrud: TableCrud<*, in SQLiteDatabase>): Boolean =
      checkTableExist(tableCrud).onDelete(writableDatabase)

  @SafeVarargs
 override fun delete(vararg tableCruds: TableCrud<*, in SQLiteDatabase>): Boolean {
    var delete = true
    for (table in tableCruds) delete = delete && delete(table)
    return delete
  }

  override fun <T> delete(tableCrud: TableCrud<*, in SQLiteDatabase>, column: Column<T>, list: List<out T>): Boolean =
      checkTableExist(tableCrud).onDelete(writableDatabase, column, list)

  override fun <T : Identifiable<Int>> save(t: T, tableCrud: TableCrud<T, in SQLiteDatabase>): Long =
      checkTableExist(tableCrud).onSave(t, writableDatabase)

  override fun <T : Identifiable<Int>> save(list: IdentifiableList<out T>, tableCrud: TableCrud<T, in SQLiteDatabase>): Boolean =
      checkTableExist(tableCrud).onSave(list, writableDatabase)


  override fun deleteAll(): Boolean = synchronized(FastDatabaseImpl::class.java) {
    var deleted = true
    try {
      writableDatabase.execSQL("PRAGMA foreign_keys = FALSE");
      transact {
        for (table in Conditions.checkNotNull(tables())) deleted = deleted && delete(checkTableExist(table))
      }
      writableDatabase.execSQL("PRAGMA foreign_keys = TRUE");
    } catch (e: Exception) {
    }
    return deleted
  }

  override fun getLastId(tableCrud: TableCrud<*, in SQLiteDatabase>): Int =
      checkTableExist(tableCrud).onGetLastId(readableDatabase)

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

  private fun <T : Identifiable<Int>> checkTableExist(tableCrud: TableCrud<T, in SQLiteDatabase>): TableCrud<T, in SQLiteDatabase> =
      Conditions.checkNotNull(tableCrud)

  companion object {
    private val TAG: String = LogUtil.makeTag(FastDatabase::class.java)
  }

  init {
    LogUtil.d(TAG, "fast db init")
  }
}