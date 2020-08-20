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
import promise.commons.makeInstance
import promise.commons.model.List
import promise.commons.util.ClassUtil
import promise.utils.Visitor

abstract class FastDatabase internal constructor(
    name: String?,
    version: Int) : FastDatabaseOpenHelper(name, version),
    Crud<SupportSQLiteDatabase> {
  /**
   * adds tables to the existing database
   */
  abstract fun add(database: SupportSQLiteDatabase, vararg tables: TableCrud<*, in SupportSQLiteDatabase>): Boolean

  /**
   * returns a table instance provided the class of the table
   * the class provided must be annotated with @Table
   */
  abstract fun <T : TableCrud<*, in SupportSQLiteDatabase>> obtain(tableClass: Class<out TableCrud<*, in SupportSQLiteDatabase>>): T

  /**
   * queries the given sql and returns a cursor
   */
  abstract fun querySql(sql: String): Cursor

  /**
   * pass a visitor to merge multiple operations
   */
  abstract fun <R : Any> accept(visitor: Visitor<FastDatabase, R>): R

  /**
   * queries the given query builder and returns a cursor
   */
  abstract fun query(queryBuilder: QueryBuilder): Cursor

  /**
   * returns a writable version of the database
   */
  abstract fun writableDatabase(): SupportSQLiteDatabase

  /**
   * executes query transaction
   */
  abstract fun transact(block: FastDatabase.() -> Unit)

  /**
   * clear records in the passed tables
   */
  abstract fun delete(vararg tableCruds: TableCrud<*, in SupportSQLiteDatabase>): Boolean

  /**
   * returns the name of the database
   */
  abstract fun name(): String

  /**
   * in case migrations have not been done successfully, calling this ensures the database drops and creates all the tables afresh
   */
  abstract fun fallBackToDestructiveMigration()

  companion object {
    private val dbCache: ArrayMap<String, FastDatabase> = ArrayMap()
    private val lock = Any()
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