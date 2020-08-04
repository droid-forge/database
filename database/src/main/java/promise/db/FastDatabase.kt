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
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.collection.ArrayMap
import promise.commons.makeInstance
import promise.commons.model.List
import promise.commons.util.ClassUtil

abstract class FastDatabase internal constructor(
    name: String?,
    factory: SQLiteDatabase.CursorFactory?,
    version: Int,
    errorHandler: DatabaseErrorHandler) : FastDatabaseOpenHelper(name, factory, version, errorHandler), Crud<SQLiteDatabase> {

  abstract fun add(database: SQLiteDatabase, vararg tables: TableCrud<*, in SQLiteDatabase>): Boolean

  abstract fun <T : TableCrud<*, in SQLiteDatabase>> obtain(tableClass: Class<out TableCrud<*, in SQLiteDatabase>>): T

  /**
   *
   */
  abstract fun querySql(sql: String): Cursor

  /**
   *
   */
  abstract fun query(queryBuilder: QueryBuilder): Cursor

  /**
   *
   */
  abstract fun writableDatabase(): SQLiteDatabase

  companion object {
    private val dbCache: ArrayMap<String, FastDatabase> = ArrayMap()

    private val lock = Any()

    /**
     *
     */
    const val DEFAULT_NAME = "fast"

    @JvmOverloads
    @JvmStatic
    fun createDatabase(dbClass: Class<*>,
                       name: String,
                       migration: Migration? = null,
                       databaseCreationCallback: DatabaseCreationCallback? = null): FastDatabase {
      fun makeDatabase(dbClass: Class<*>): FastDatabase {
        if (ClassUtil.hasAnnotation(dbClass, Database::class.java)) {
          val database = dbClass.getAnnotation(Database::class.java)!!
          if (dbCache.containsKey(name)) return dbCache[name] as FastDatabase
          val databaseObject = makeInstance(FastDatabaseImpl::class,
              arrayOf(name, database.version)) as FastDatabaseImpl
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
    fun createInMemoryDatabase(
        dbClass: Class<*>,
        databaseCreationCallback: DatabaseCreationCallback? = null): FastDatabase {
      fun makeDatabase(dbClass: Class<*>): FastDatabase {
        if (ClassUtil.hasAnnotation(dbClass, Database::class.java)) {
          val database = dbClass.getAnnotation(Database::class.java)!!
          val databaseObject = makeInstance(FastDatabaseImpl::class,
              arrayOf(null, database.version)) as FastDatabaseImpl
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

    @JvmOverloads
    @JvmStatic
    fun createReactiveDatabase(dbClass: Class<*>,
                               name: String,
                               migration: Migration? = null,
                               databaseCreationCallback: DatabaseCreationCallback? = null): FastDatabase {
      fun makeDatabase(dbClass: Class<*>): FastDatabase {
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
                                       databaseCreationCallback: DatabaseCreationCallback? = null): FastDatabase {
      fun makeDatabase(dbClass: Class<*>): FastDatabase {
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
  }
}