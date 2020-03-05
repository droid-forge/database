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
import android.content.ContentValues
import android.database.Cursor
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import io.reactivex.Maybe
import io.reactivex.Single
import promise.commons.data.log.LogUtil
import promise.commons.model.Identifiable
import promise.commons.model.List
import promise.commons.util.DoubleConverter
import promise.db.query.QueryBuilder
import promise.model.IdentifiableList

/**
 *
 */

class ReactiveFastDatabase private constructor(
    name: String,
    factory: SQLiteDatabase.CursorFactory?,
    version: Int,
    errorHandler: DatabaseErrorHandler) : FastDatabase(name, factory, version, errorHandler), ReactiveCrud<SQLiteDatabase> {
  private val TAG: String = LogUtil.makeTag(ReactiveFastDatabase::class.java)

  /**
   * @param name
   * @param version
   * @param cursorListener
   * @param listener
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  internal constructor(name: String, version: Int, cursorListener: DatabaseCursorFactory.Listener?, listener: Corrupt?) : this(
      name,
      DatabaseCursorFactory(cursorListener),
      version,
      DatabaseErrorHandler {
        assert(listener != null)
        listener!!.onCorrupt()
      })

  /**
   * @param version
   */
  internal constructor(version: Int) : this(DEFAULT_NAME, version)

  constructor(name: String, version: Int) : this(name, version, null, null)

  constructor(): this("", 0, null, null)

  /**
   * @param tableCrud
   * @param database
   * @param <T>
   * @return
  </T> */
  private fun <T : Identifiable<Int>> dropAsync(tableCrud: TableCrud<T, in SQLiteDatabase>, database: SQLiteDatabase): Single<Boolean> {
    return Single.fromCallable { tableCrud.onDrop(database) }
  }

  /**
   * @param builder
   * @return
   */
  fun queryAsync(builder: QueryBuilder): Single<Cursor> {
    return Single.fromCallable { query(builder) }
  }

  /**
   * @param tableCrud
   * @param <T>
   * @return
  </T> */
  @Throws(TableError::class)
  override fun <T : Identifiable<Int>> readAsync(tableCrud: TableCrud<T, in SQLiteDatabase>): ReactiveTable.Extras<T> {
    return object : QueryExtras<T>(tableCrud, readableDatabase) {
      override fun serialize(t: T): ContentValues = tableCrud.serialize(t)
      override fun deserialize(cursor: Cursor): T = tableCrud.deserialize(cursor)
    }
  }


  /**
   * @param tableCrud
   * @param <T>
   * @return
  </T> */
  override fun <T : Identifiable<Int>> readAllAsync(tableCrud: TableCrud<T, in SQLiteDatabase>): Maybe<IdentifiableList<out T>> {
    return Maybe.fromCallable { findAll(tableCrud) }
  }

  /**
   * @param tableCrud
   * @param column
   * @param <T>
   * @return
  </T> */
  override fun <T : Identifiable<Int>> readAllAsync(tableCrud: TableCrud<T, in SQLiteDatabase>, vararg column: Column<*>): Maybe<IdentifiableList<out T>> =
      Maybe.fromCallable { tableCrud.onFindAll(readableDatabase, *column) }

  /**
   * @param t
   * @param tableCrud
   * @param column
   * @param <T>
   * @return
  </T> */
  override fun <T : Identifiable<Int>> updateAsync(t: T, tableCrud: TableCrud<T, in SQLiteDatabase>, column: Column<*>): Maybe<Boolean> =
      Maybe.fromCallable { tableCrud.onUpdate(t, writableDatabase, column) }

  /**
   * @param t
   * @param tableCrud
   * @param <T>
   * @return
  </T> */
  override fun <T : Identifiable<Int>> updateAsync(t: T, tableCrud: TableCrud<T, in SQLiteDatabase>): Maybe<Boolean> {
    return Maybe.fromCallable { tableCrud.onUpdate(t, writableDatabase) }
  }

  /**
   * @param tableCrud
   * @param column
   * @param <T>
   * @return
  </T> */
  override fun <T : Identifiable<Int>> deleteAsync(tableCrud: TableCrud<T, in SQLiteDatabase>, column: Column<*>): Maybe<Boolean> =
      Maybe.fromCallable { tableCrud.onDelete(writableDatabase, column) }

  /**
   * @param tableCrud
   * @param t
   * @param <T>
   * @return
  </T> */
  override fun <T : Identifiable<Int>> deleteAsync(tableCrud: TableCrud<T, in SQLiteDatabase>, t: T): Maybe<Boolean> {
    return Maybe.fromCallable { tableCrud.onDelete(t, writableDatabase) }
  }

  /**
   * @param tableCrud
   * @return
   */
  override fun deleteAsync(tableCrud: TableCrud<*, in SQLiteDatabase>): Maybe<Boolean> {
    return Maybe.fromCallable { tableCrud.onDelete(writableDatabase) }
  }

  /**
   * @param tableCrud
   * @param column
   * @param list
   * @param <C>
   * @return
  </C> */
  override fun <C> deleteAsync(tableCrud: TableCrud<*, in SQLiteDatabase>, column: Column<C>, list: List<out C>): Maybe<Boolean> {
    return Maybe.fromCallable { tableCrud.onDelete(writableDatabase, column, list) }
  }

  /**
   * @param t
   * @param tableCrud
   * @param <T>
   * @return
  </T> */
  override fun <T : Identifiable<Int>> saveAsync(t: T, tableCrud: TableCrud<T, in SQLiteDatabase>): Single<Long> {
    return Single.fromCallable { tableCrud.onSave(t, writableDatabase) }
  }

  /**
   * @param list
   * @param tableCrud
   * @param <T>
   * @return
  </T> */
  override fun <T : Identifiable<Int>> saveAsync(list: IdentifiableList<out T>, tableCrud: TableCrud<T, in SQLiteDatabase>): Single<Boolean> {
    return Single.fromCallable { tableCrud.onSave(list, writableDatabase) }
  }

  /**
   * @return
   */
  override fun deleteAllAsync(): Maybe<Boolean> =
      Maybe.zip(tables().map { tableCrud: TableCrud<*, in SQLiteDatabase> -> this.deleteAsync(tableCrud) }
      ) { objects: Array<Any> ->
        List.fromArray<Any>(*objects).allMatch { aBoolean: Any ->
          aBoolean is Boolean &&
              aBoolean
        }
      }

  /**
   * @param tableCrud
   * @param <T>
   * @return
  </T> */
  override fun <T : Identifiable<Int>> getLastIdAsync(tableCrud: TableCrud<T, in SQLiteDatabase>): Maybe<Int> {
    return Maybe.fromCallable { tableCrud.onGetLastId(readableDatabase) }
  }

  /**
   * @param <T>
  </T> */
  private abstract inner class QueryExtras<T : Identifiable<Int>> internal constructor(private val tableCrud: TableCrud<T, in SQLiteDatabase>,
                                                                                       private val database: SQLiteDatabase) : ReactiveTable.Extras<T>, DoubleConverter<T, Cursor, ContentValues> {
    /**
     * @return
     */
    override fun first(): Maybe<T> = Maybe.fromCallable { tableCrud.onFind(database).first() }

    /**
     * @return
     */
    override fun last(): Maybe<T> = Maybe.fromCallable { tableCrud.onFind(database).last() }

    /**
     * @return
     */
    override fun all(): Maybe<IdentifiableList<out T>> =
        Maybe.fromCallable { tableCrud.onFind(database).all() }

    /**
     * @param limit
     * @return
     */
    override fun limit(limit: Int): Maybe<IdentifiableList<out T>> =
        Maybe.fromCallable { tableCrud.onFind(database).limit(limit) }

    /**
     * @param skip
     * @param limit
     * @return
     */
    override fun paginate(skip: Int, limit: Int): Maybe<IdentifiableList<out T>> =
        Maybe.fromCallable { tableCrud.onFind(database).paginate(skip, limit) }

    /**
     * @param skip
     * @param limit
     * @return
     */
    override fun paginateDescending(skip: Int, limit: Int): Maybe<IdentifiableList<out T>> =
        Maybe.fromCallable { tableCrud.onFind(database).paginateDescending(skip, limit) }

    /**
     * @param column
     * @param a
     * @param b
     * @return
     */
    override fun <N : Number> between(column: Column<N>, a: N, b: N): Maybe<IdentifiableList<out T>> =
        Maybe.fromCallable { tableCrud.onFind(database).between(column, a, b) }

    /**
     * @param column
     * @return
     */
    override fun where(vararg column: Column<*>): Maybe<IdentifiableList<out T>> =
        Maybe.fromCallable { tableCrud.onFind(database).where(*column) }

    /**
     * @param column
     * @param bounds
     * @return
     */
    @SafeVarargs
    override fun <N : Number> notIn(column: Column<N>, vararg bounds: N): Maybe<IdentifiableList<out T>> =
        Maybe.fromCallable { tableCrud.onFind(database).notIn(column, *bounds) }

    /**
     * @param column
     * @return
     */
    override fun like(vararg column: Column<String>): Maybe<IdentifiableList<out T>> =
        Maybe.fromCallable { tableCrud.onFind(database).like(*column) }

    /**
     * @param column
     * @return
     */
    override fun orderBy(column: Column<*>): Maybe<IdentifiableList<out T>> =
        Maybe.fromCallable { tableCrud.onFind(database).orderBy(column) }

    /**
     * @param column
     * @return
     */
    override fun groupBy(column: Column<*>): Maybe<IdentifiableList<out T>> =
        Maybe.fromCallable { tableCrud.onFind(database).groupBy(column) }

    /**
     * @param column
     * @param column1
     * @return
     */
    override fun groupAndOrderBy(column: Column<*>, column1: Column<*>): Maybe<IdentifiableList<out T>> =
        Maybe.fromCallable { tableCrud.onFind(database).groupAndOrderBy(column, column1) }
  }

  /**
   * @param name
   * @param factory
   * @param version
   * @param errorHandler
   */
  init {
    LogUtil.d(TAG, "fast db init")
  }
}