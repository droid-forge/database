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
import promise.commons.data.log.LogUtil
import promise.commons.makeInstance
import promise.commons.model.Identifiable
import promise.commons.model.List
import promise.commons.util.ClassUtil
import promise.commons.util.Conditions
import promise.model.IdentifiableList
import java.util.*

/**
 *
 */

open class FastDatabase internal constructor(
    name: String?,
    factory: SQLiteDatabase.CursorFactory?,
    version: Int,
    errorHandler: DatabaseErrorHandler)
  : FastDatabaseOpenHelper(name, factory, version, errorHandler),
    Crud<SQLiteDatabase> {

  private val cacheMap: ArrayMap<String, Any> = ArrayMap()

  private var migration: Migration? = null

  private var databaseCreationCallback: DatabaseCreationCallback? = null

  /**
   * @param name
   * @param version
   * @param cursorListener
   * @param listener
   */
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

  /**
   * @param db
   */
  final override fun onCreate(db: SQLiteDatabase) {
    if (databaseCreationCallback != null) {
      databaseCreationCallback!!.beforeCreate(db)
      create(db)
      databaseCreationCallback!!.afterCreate(db)
    } else create(db)
  }

  /**
   * @param database
   * @param oldVersion
   * @param newVersion
   */
  final override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    LogUtil.d(TAG, "onUpgrade", oldVersion, newVersion)
    if (newVersion - oldVersion == 1)
      onUpgradeDatabase(database, oldVersion, newVersion) else {
      var i = oldVersion
      while (i < newVersion) {
        onUpgradeDatabase(database, i, i + 1)
        i++
      }
    }
    upgradeTables(database, oldVersion, newVersion)
  }

  /**
   * @param database
   * @param oldVersion
   * @param newVersion
   * @return
   */
  private fun onUpgradeDatabase(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    if (migration != null) migration!!.onMigrate(this, database, oldVersion, newVersion)
  }

  /**
   * @return
   */
  fun name(): String = this.databaseName

  private var tables: List<out FastTable<*>>? = null

  /**
   * @return
   */
  protected fun tables(): List<TableCrud<*, in SQLiteDatabase>> =
      Conditions.checkNotNull(tables, "tables not found for this database")
          as List<TableCrud<*, in SQLiteDatabase>>

  /**
   *
   */
  fun <T : FastTable<*>> obtain(tableClass: Class<out FastTable<*>>): T {
    fun <T : FastTable<*>> makeTable(tableClass: Class<out FastTable<*>>): T {
      if (ClassUtil.hasAnnotation(tableClass, Table::class.java)) {
        val table = tableClass.getAnnotation(Table::class.java)!!
        if (cacheMap.containsKey(table.tableName)) return cacheMap[table.tableName] as T
        val tableObject = makeInstance(tableClass.kotlin, arrayOf(this)) as T
        tableObject.setNameOfTable(table.tableName)
        tableObject.setArgs(ArrayMap<String, Any>().apply {
          put(INDEXES, table.indices)
          put(FOREIGN_kEYS, table.foreignKeys)
          put(COMPOUND_INDEXES, table.compoundIndexes)
        })
        cacheMap[table.tableName] = tableObject
        return tableObject
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

  fun setDatabaseCreationCallback(creationCallback: DatabaseCreationCallback?): FastDatabase {
    this.databaseCreationCallback = creationCallback
    return this
  }

  /**
   * @param database
   */
  private fun create(database: SQLiteDatabase) {
    var created = true
    for (table in Conditions.checkNotNull(tables())) created = try {
      created && create(table, database)
    } catch (dbError: DBError) {
      LogUtil.e(TAG, dbError)
      return
    }
  }

  /**
   * @param database
   * @param v1
   * @param v2
   */
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

  /**
   * @param database
   * @param tableCruds
   * @return
   */
  @SafeVarargs
  fun add(database: SQLiteDatabase, vararg tableCruds: TableCrud<*, in SQLiteDatabase>): Boolean {
    var created = true
    for (table in tableCruds) {
      created = try {
        created && create(table, database)
      } catch (dbError: DBError) {
        LogUtil.e(TAG, dbError)
        return false
      }
    }
    return created
  }

  /**
   * @param database
   * @return
   */
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

  /**
   * @param tableCrud
   * @param database
   * @return
   * @throws DBError
   */
  @Throws(DBError::class)
  private fun create(tableCrud: TableCrud<*, in SQLiteDatabase>, database: SQLiteDatabase): Boolean {
    try {
      tableCrud.onCreate(database)
    } catch (e: TableError) {
      throw DBError(e)
    }
    return true
  }

  /**
   * @param tableCrud
   * @param database
   * @return
   * @throws DBError
   */
  @Throws(DBError::class)
  private fun drop(tableCrud: TableCrud<*, in SQLiteDatabase>, database: SQLiteDatabase): Boolean {
    try {
      checkTableExist(tableCrud).onDrop(database)
    } catch (e: TableError) {
      throw DBError(e)
    }
    return true
  }

  override fun querySql(sql: String): Cursor {
    LogUtil.d(TAG, "query: $sql")
    return readableDatabase.rawQuery(sql, null)
  }

  /**
   * @param builder
   * @return
   */
  fun query(builder: QueryBuilder): Cursor {
    val sql = builder.build()
    val params = builder.buildParameters()
    LogUtil.d(TAG, "query: $sql", " params: " + Arrays.toString(params))
    return readableDatabase.rawQuery(sql, params)
  }

  /**
   * @param tableCrud
   * @param <T>
   * @return
  </T> */
  override fun <T : Identifiable<Int>> find(tableCrud: TableCrud<T, in SQLiteDatabase>): TableCrud.Extras<T> =
      checkTableExist(tableCrud).onFind(readableDatabase)

  /**
   * @param tableCrud
   * @param <T>
   * @return
  </T> */
  override fun <T : Identifiable<Int>> findAll(tableCrud: TableCrud<T, in SQLiteDatabase>): IdentifiableList<out T> =
      checkTableExist(tableCrud).onFindAll(readableDatabase, true)

  /**
   * @param t
   * @param tableCrud
   * @param <T>
   * @return
  </T> */
  override fun <T : Identifiable<Int>> update(t: T, tableCrud: TableCrud<T, in SQLiteDatabase>): Boolean =
      checkTableExist(tableCrud).onUpdate(t, writableDatabase)

  /**
   * @param t
   * @param tableCrud
   * @param column
   * @param <T>
   * @return
  </T> */
  override fun <T : Identifiable<Int>> update(t: T, tableCrud: TableCrud<T, in SQLiteDatabase>, column: Column<*>): Boolean =
      try {
        checkTableExist(tableCrud).onUpdate(t, writableDatabase, column)
      } catch (tableError: TableError) {
        LogUtil.e(TAG, "update error", tableError)
        false
      }

  /**
   * @param tableCrud
   * @param columns
   * @param <T>
   * @return
  </T> */
  override fun <T : Identifiable<Int>> findAll(tableCrud: TableCrud<T, in SQLiteDatabase>,
                                               vararg columns: Column<*>): IdentifiableList<out T> =
      checkTableExist(tableCrud).onFindAll(readableDatabase, *columns)

  /**
   * @param tableCrud
   * @param t
   * @param <T>
   * @return
  </T> */
  override fun <T : Identifiable<Int>> delete(tableCrud: TableCrud<T, in SQLiteDatabase>, t: T): Boolean =
      checkTableExist(tableCrud).onDelete(t, writableDatabase)

  /**
   * @param tableCrud
   * @param column
   * @return
   */
  override fun delete(tableCrud: TableCrud<*, in SQLiteDatabase>, column: Column<*>): Boolean =
      checkTableExist(tableCrud).onDelete(writableDatabase, column)

  /**
   * @param tableCrud
   * @return
   */
  override fun delete(tableCrud: TableCrud<*, in SQLiteDatabase>): Boolean =
      checkTableExist(tableCrud).onDelete(writableDatabase)

  /**
   * @param tableCruds
   * @return
   */
  @SafeVarargs
  fun delete(vararg tableCruds: TableCrud<*, in SQLiteDatabase>): Boolean {
    var delete = true
    for (table in tableCruds) delete = delete && delete(table)
    return delete
  }

  /**
   * @param tableCrud
   * @param column
   * @param list
   * @param <T>
   * @return
  </T> */
  override fun <T> delete(tableCrud: TableCrud<*, in SQLiteDatabase>, column: Column<T>, list: List<out T>): Boolean =
      checkTableExist(tableCrud).onDelete(writableDatabase, column, list)

  /**
   * @param t
   * @param tableCrud
   * @param <T>
   * @return
  </T> */
  override fun <T : Identifiable<Int>> save(t: T, tableCrud: TableCrud<T, in SQLiteDatabase>): Long =
      checkTableExist(tableCrud).onSave(t, writableDatabase)

  /**
   * @param list
   * @param tableCrud
   * @param <T>
   * @return
  </T> */
  override fun <T : Identifiable<Int>> save(list: IdentifiableList<out T>, tableCrud: TableCrud<T, in SQLiteDatabase>): Boolean =
      checkTableExist(tableCrud).onSave(list, writableDatabase)

  /**
   * @return
   */
  override fun deleteAll(): Boolean = synchronized(FastDatabase::class.java) {
    var deleted = true
    try {
      writableDatabase.execSQL("PRAGMA foreign_keys = FALSE");
      transact {
        for (table in Conditions.checkNotNull(tables())) deleted = deleted && delete(checkTableExist(table))
      }
      writableDatabase.execSQL("PRAGMA foreign_keys = TRUE");
    } catch (e: Exception) { }
    return deleted
  }

  /**
   * @param tableCrud
   * @return
   */
  override fun getLastId(tableCrud: TableCrud<*, in SQLiteDatabase>): Int =
      checkTableExist(tableCrud).onGetLastId(readableDatabase)

  inline fun transact(block: FastDatabase.() -> Unit) = synchronized(this) {
    val db = writableDatabase
    try {
      db.beginTransaction()
      block.invoke(this)
      db.setTransactionSuccessful()
    } finally {
      db.endTransaction()
    }
  }

  /**
   * @param tableCrud
   * @param <T>
   * @return
  </T> */
  private fun <T : Identifiable<Int>> checkTableExist(tableCrud: TableCrud<T, in SQLiteDatabase>): TableCrud<T, in SQLiteDatabase> =
      Conditions.checkNotNull(tableCrud)

  companion object {

    private val dbCache: ArrayMap<String, FastDatabase> = ArrayMap()

    private val lock = Any()
    /**
     *
     */
    const val DEFAULT_NAME = "fast"

    @JvmOverloads
    @JvmStatic
    fun createDatabase(dbClass: Class<*>, name: String, migration: Migration? = null): FastDatabase {
      fun makeDatabase(dbClass: Class<*>): FastDatabase {
        if (ClassUtil.hasAnnotation(dbClass, Database::class.java)) {
          val database = dbClass.getAnnotation(Database::class.java)!!
          if (dbCache.containsKey(name)) return dbCache[name] as FastDatabase
          val databaseObject = makeInstance(FastDatabase::class,
              arrayOf(name, database.version)) as FastDatabase
          val classList: List<Class<out FastTable<*>>> = List()
          database.tables.forEach {
            classList.add(it.java)
          }
          databaseObject.setTables<FastTable<*>>(classList)
          databaseObject.setMigration(migration)
          dbCache[name] = databaseObject
          return databaseObject
        }
        throw IllegalArgumentException("The class must be annotated with @Database")
      }
      return synchronized(lock) {
        makeDatabase(dbClass)
      }
    }

    @JvmStatic
    fun createInMemoryDatabase(dbClass: Class<*>): FastDatabase {
      fun makeDatabase(dbClass: Class<*>): FastDatabase {
        if (ClassUtil.hasAnnotation(dbClass, Database::class.java)) {
          val database = dbClass.getAnnotation(Database::class.java)!!
          val databaseObject = makeInstance(FastDatabase::class,
              arrayOf(null, database.version)) as FastDatabase
          val classList: List<Class<out FastTable<*>>> = List()
          database.tables.forEach {
            classList.add(it.java)
          }
          databaseObject.setTables<FastTable<*>>(classList)
          return databaseObject
        }
        throw IllegalArgumentException("The class must be annotated with @Database")
      }
      return synchronized(lock) {
        makeDatabase(dbClass)
      }
    }

    @JvmOverloads
    @JvmStatic
    fun createReactiveDatabase(dbClass: Class<*>,
                               name: String,
                               migration: Migration? = null,
                               databaseCreationCallback: DatabaseCreationCallback? = null): ReactiveFastDatabase {
      fun makeDatabase(dbClass: Class<*>): ReactiveFastDatabase {
        if (ClassUtil.hasAnnotation(dbClass, Database::class.java)) {
          val database = dbClass.getAnnotation(Database::class.java)!!
          if (dbCache.containsKey(name)) return dbCache[name] as ReactiveFastDatabase
          val databaseObject = makeInstance(ReactiveFastDatabase::class,
              arrayOf(name, database.version)) as ReactiveFastDatabase
          val classList: List<Class<out FastTable<*>>> = List()
          database.tables.forEach {
            classList.add(it.java)
          }
          databaseObject.setTables<FastTable<*>>(classList)
          databaseObject.setMigration(migration)
          databaseObject.setDatabaseCreationCallback(databaseCreationCallback)
          dbCache[name] = databaseObject
          return databaseObject
        }
        throw IllegalArgumentException("The class must be annotated with @Database")
      }
      return synchronized(lock) {
        makeDatabase(dbClass)
      }
    }

    @JvmOverloads
    @JvmStatic
    fun createInMemoryReactiveDatabase(dbClass: Class<*>,
                                       databaseCreationCallback: DatabaseCreationCallback? = null): ReactiveFastDatabase {
      fun makeDatabase(dbClass: Class<*>): ReactiveFastDatabase {
        if (ClassUtil.hasAnnotation(dbClass, Database::class.java)) {
          val database = dbClass.getAnnotation(Database::class.java)!!
          val databaseObject = makeInstance(ReactiveFastDatabase::class,
              arrayOf(null, database.version)) as ReactiveFastDatabase
          val classList: List<Class<out FastTable<*>>> = List()
          database.tables.forEach {
            classList.add(it.java)
          }
          databaseObject.setTables<FastTable<*>>(classList)
          databaseObject.setDatabaseCreationCallback(databaseCreationCallback)
          return databaseObject
        }
        throw IllegalArgumentException("The class must be annotated with @Database")
      }
      return synchronized(lock) {
        makeDatabase(dbClass)
      }
    }

    /*private static Map<IndexCreated, Table<, SQLiteDatabase>> indexCreatedTableHashMap;*/
    private val TAG: String = LogUtil.makeTag(FastDatabase::class.java)
  }

  init {
    LogUtil.d(TAG, "fast db init")
  }
}