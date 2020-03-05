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

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.text.TextUtils
import io.reactivex.Maybe
import io.reactivex.Single
import promise.commons.data.log.LogUtil
import promise.commons.model.Identifiable
import promise.commons.model.List
import promise.commons.util.Conditions
import promise.commons.util.DoubleConverter
import promise.db.query.QueryBuilder
import promise.db.query.criteria.Criteria
import promise.db.query.projection.Projection
import promise.model.ITimeAware
import promise.model.IdentifiableList
import java.util.*

/**
 * This class models database queries
 *
 * @param <T> [Identifiable] instance to be persisted by the model
</T> */
abstract class FastTable<T : Identifiable<Int>>
/**
 * @param database
 */(
    /**
     *
     */
    val database: FastDatabase) : TableCrud<T, SQLiteDatabase> {
  companion object {
    /**
     * The create prefix in a prefix for queries that create a table structure
     */
    private const val CREATE_PREFIX = "CREATE TABLE IF NOT EXISTS "
    /**
     * The drop prefix is a prefix for queries that destroy a table in the database
     */
    private const val DROP_PREFIX = "TRUNCATE TABLE IF EXISTS "
    /**
     * the tableName of the primary column
     */
    private const val ID_COLUMN_NAME = "id"
    /**
     * The tableName of the timestamp columns
     */
    private const val CREATED_AT_COLUMN_NAME = "CREATED_AT"
    private const val UPDATED_AT_COLUMN_NAME = "UPDATED_AT"
    /**
     * Each table must have a primary column as well al timestamp columns
     * see [Column] for more info
     */
    var id: Column<Int>
    var createdAt: Column<Int>
    var updatedAt: Column<Int>

    /*
   * the primary column of the database if named id and the same tableName in all tables
   */
    init {
      id = Column(ID_COLUMN_NAME, Column.Type.INTEGER.PRIMARY_KEY_AUTOINCREMENT())
      createdAt = Column(CREATED_AT_COLUMN_NAME, Column.Type.INTEGER.NULLABLE())
      updatedAt = Column(UPDATED_AT_COLUMN_NAME, Column.Type.INTEGER.NULLABLE())
    }

    /**
     * The alter prefix is used to alter the structure of a column when making upgrades
     */
    private const val ALTER_COMMAND = "ALTER TABLE"
  }

  /**
   * @return
   */
  /**
   * The specific tag for logging in this table
   */
  private val TAG: String = LogUtil.makeTag(FastTable::class.java) + name
  /**
   * Temporary data holder for holding data during dangerous table structure changes
   */
  private var backup: IdentifiableList<T>? = null

  /**
   * @return
   */
  private val reactiveDatabase: ReactiveFastDatabase
    get() {
      if (database is ReactiveFastDatabase) return database
      throw IllegalStateException("The database is not an instance of ReactiveFastDatabase")
    }

  /**
   * gets all the columns for this model from the child class for creation purposes
   * see [.onCreate]
   *
   * @return list of columns
   */
  abstract val columns: List<out Column<*>>
  /**
   *
   */
  private var nameOfTable: String = ""

  /**
   *
   */
  internal fun setNameOfTable(name: String) {
    this.nameOfTable = name
  }

  /**
   *
   */
  final override val name: String
    get() = nameOfTable

  /**
   * optional to get the number of all the columns in this table
   * note by the added 3 is for id, and timestamp columns
   *
   * @return the number of the columns in this table
   */
  val numberOfColumns: Int
    get() = columns.size + 3

  /**
   * this handler created a table in the given database instance passed
   * uses [Column.getDescription] to get the column info
   * adds the id and timestamp in the table
   *
   * @param database writable sql database
   * @return true if table is created
   * @throws TableError if theirs a query error
   */
  @Throws(TableError::class)
  override fun onCreate(database: SQLiteDatabase): Boolean {
    var sql = CREATE_PREFIX
    /*
     * add the opening braces after select prefix, see {@link FastTable#CREATE_PREFIX}
     */sql = "$sql$name("
    val columns = Conditions.checkNotNull(columns)
    /*
     * sorts the column in ascending order, see {@link Column#ascending()} comparator
     */Collections.sort(columns, Column.ascending)
    val columns1 = List<Column<*>>()
    /*
     * add the three additional columns to the creation script
     */columns1.add(id)
    columns1.addAll(columns)
    columns1.add(createdAt)
    columns1.add(updatedAt)
    for (i in columns1.indices) {
      val column = columns1[i]
      sql = if (i == columns1.size - 1) sql + column.toString() else "$sql$column, "
    }
    sql = "$sql);"
    try {
      LogUtil.d(TAG, sql)
      database.execSQL(sql)
    } catch (e: SQLException) {
      throw TableError(e)
    }
    return true
  }

  /*
   * upgrades the table from one version to the next
   * if the table doesn't have the timestamps add them
   * @param database writable sql database
   * @param v1 previous version
   * @param v2 next version
   * @return
   * @throws TableError
   */
  @Throws(TableError::class)
  override fun onUpgrade(database: SQLiteDatabase, v1: Int, v2: Int) {
    val builder: QueryBuilder = QueryBuilder().from(this)
    @SuppressLint("Recycle") val c = database.rawQuery(builder.build(), builder.buildParameters())
    val set: Set<String> = HashSet(List.fromArray(*c.columnNames))
    if (!set.contains(createdAt.name)) addColumns(database, createdAt)
    if (!set.contains(updatedAt.name)) addColumns(database, updatedAt)
  }

  /**
   * adds column to the database
   *
   * @param database writable sql database
   * @param columns  fields to be added must be nullable entry types
   * @throws TableError if theirs an sql error
   */
  @Throws(TableError::class)
  fun addColumns(database: SQLiteDatabase, vararg columns: Column<*>) {
    for (column in columns) {
      val alterSql = "$ALTER_COMMAND `$name` ADD $column;"
      try {
        LogUtil.d(TAG, alterSql)
        database.execSQL(alterSql)
      } catch (e: SQLException) {
        throw TableError(e)
      }
    }
  }

  /**
   * drops column from the database
   *
   * @param database writable sql database
   * @param columns  fields to be dropped
   * @return true if fields are dropped successfully
   * @throws TableError if theirs an sql error
   */
  @Throws(TableError::class)
  fun dropColumns(database: SQLiteDatabase, vararg columns: Column<*>): Boolean {
    for (column in columns) {
      val alterSql = ALTER_COMMAND + " `" + name + "` " + "DROP COLUMN " + column.name + ";"
      try {
        database.execSQL(alterSql)
      } catch (e: SQLException) {
        throw TableError(e)
      }
    }
    return true
  }

  /**
   * @param t
   * @return
   */
  fun save(t: T): Long = database.save(t, this)

  /**
   * @param t
   * @return
   */
  fun saveAsync(t: T): Single<Long> = reactiveDatabase.saveAsync(t, this)

  /**
   * @param list
   * @return
   */
  fun save(list: IdentifiableList<out T>): Boolean = database.save(list, this)

  /**
   * @param list
   * @return
   */
  fun saveAsync(list: IdentifiableList<out T>): Single<Boolean> =
      reactiveDatabase.saveAsync(list, this)

  /**
   * @param t
   * @return
   */
  fun update(t: T): Boolean = database.update(t, this)

  /**
   * @param t
   * @return
   */
  fun updateAsync(t: T): Maybe<Boolean> = reactiveDatabase.updateAsync(t, this)

  /**
   * @param t
   * @param column
   * @return
   */
  fun update(t: T, column: Column<*>): Boolean = database.update(t, this, column)

  /**
   * @param t
   * @param column
   * @return
   */
  fun updateAsync(t: T, column: Column<*>): Maybe<Boolean> =
      reactiveDatabase.updateAsync(t, this, column)

  fun queryBuilder(): QueryBuilder = QueryBuilder().from(this)

  fun query(queryBuilder: QueryBuilder): Cursor = database.query(queryBuilder)

  fun queryAsync(queryBuilder: QueryBuilder): Single<Cursor> =
      reactiveDatabase.queryAsync(queryBuilder)

  /**
   * @return
   */
  fun find(): TableCrud.Extras<T> = database.find(this)

  /**
   * @return
   */
  fun findById(idLong: Long): T? = database.find(this).where(id.with(idLong.toInt())).first()

  /**
   * @return
   */
  fun findOne(vararg columns: Column<*>): T? = database.find(this).where(*columns).first()

  /**
   * @return
   * @throws TableError
   */
  @Throws(TableError::class)
  fun findAsync(): ReactiveTable.Extras<T> = reactiveDatabase.readAsync(this)

  /**
   * @return
   */
  fun findAll(): IdentifiableList<out T> = database.findAll(this)

  /**
   * @return
   */
  fun findAllAsync(): Maybe<IdentifiableList<out T>> = reactiveDatabase.readAllAsync(this)

  /**
   * @param column
   * @return
   */
  fun findAll(vararg column: Column<*>): IdentifiableList<out T> = database.findAll(this, *column)

  /**
   * @param column
   * @return
   */
  fun findAllAsync(vararg column: Column<*>): Maybe<IdentifiableList<out T>> =
      reactiveDatabase.readAllAsync(this, *column)

  /**
   * @param column
   * @return
   */
  fun delete(column: Column<*>): Boolean = database.delete(this, column)

  /**
   * @param column
   * @return
   */
  fun deleteAsync(column: Column<*>): Maybe<Boolean> = reactiveDatabase.deleteAsync(this, column)

  /**
   * @param t
   * @return
   */
  fun delete(t: T): Boolean = database.delete(this, t)

  /**
   * @param t
   * @return
   */
  fun deleteAsync(t: T): Maybe<Boolean> = reactiveDatabase.deleteAsync(this, t)

  /**
   * @param column
   * @param list
   * @param <N>
   * @return
  </N> */
  fun <N> delete(column: Column<N>, list: List<out N>): Boolean =
      database.delete(this, column, list)

  /**
   * @param column
   * @param list
   * @param <C>
   * @return
  </C> */
  fun <C> deleteAsync(column: Column<C>, list: List<out C>): Maybe<Boolean> =
      reactiveDatabase.deleteAsync(this, column, list)

  /**
   * @return
   */
  fun clear(): Boolean = database.delete(this)

  /**
   * @return
   */
  fun clearAsync(): Maybe<Boolean> = reactiveDatabase.deleteAsync(this)

  /**
   * @return
   */
  val lastId: Int
    get() = database.getLastId(this)

  /**
   * @return
   */
  val lastIdAsync: Maybe<Int>
    get() = reactiveDatabase.getLastIdAsync(this)

  /**
   * more verbose readAsync operation against the database
   *
   * @param database readable sql database
   * @return an extras instance for more concise reads
   */
  override fun onFind(database: SQLiteDatabase): TableCrud.Extras<T> =
      object : QueryExtras<T>(database) {
        override fun serialize(t: T): ContentValues = this@FastTable.serialize(t)
        override fun deserialize(cursor: Cursor): T = this@FastTable.deserialize(cursor)
      }

  /**
   * readAsync all the rows in the database
   *
   * @param database readable sql database
   * @param close    close the connection if this is true
   * @return a list of records
   */
  override fun onFindAll(database: SQLiteDatabase, close: Boolean): IdentifiableList<out T> {
    val builder: QueryBuilder = QueryBuilder().from(this@FastTable)
    val cursor: Cursor
    return try {
      cursor = database.rawQuery(builder.build(), builder.buildParameters())
      val ts = IdentifiableList<T>()
      while (cursor.moveToNext() && !cursor.isClosed) ts.add(getWithId(cursor))
      cursor.close()
      /*if (close) database.close();*/ts
    } catch (e: SQLiteException) {
      IdentifiableList<T>()
    }
  }

  /**
   * readAsync the rows following the criteria specified in the column provided
   * for each of the columns
   * if [Column.value] is not null, filter with the value
   * if [Column.order] is not null, order by that column too
   *
   * @param database readable sql database
   * @param columns  the fields to infer where and order by conditions
   * @return list of records satisfying the criteria
   */
  override fun onFindAll(database: SQLiteDatabase, vararg columns: Column<*>): IdentifiableList<out T> {
    val builder: QueryBuilder = QueryBuilder().from(this@FastTable).takeAll()
    for (column in columns) {
      if (column.value() != null) builder.whereAnd(Criteria.equals(column, column.value()))
      if (column.order() != null) {
        if (column.order() == Column.DESCENDING) {
          builder.orderByDescending(column)
        } else builder.orderByAscending(column)
      }
    }
    val cursor = database.rawQuery(builder.build(), builder.buildParameters())
    val ts = IdentifiableList<T>()
    while (cursor.moveToNext() && !cursor.isClosed) ts.add(getWithId(cursor))
    cursor.close()
    /*database.close();*/return ts
  }

  /**
   * updateAsync a row in the table
   *
   * @param t        instance to updateAsync
   * @param database writable sql database
   * @param column   field with where condition to updateAsync
   * @return true if instance is updated
   */
  @Throws(TableError::class)
  override fun onUpdate(t: T, database: SQLiteDatabase, column: Column<*>): Boolean {
    val whereArg: String = if (column.operand != null && column.value() != null)
    column.name + column.operand + column.value()
    else throw TableError("Cant update the record, missing updating information")
    val values = serialize(t)
    values.put(updatedAt.name, System.currentTimeMillis())
    return database.update(name, values, whereArg, null) >= 0
  }

  /**
   * updated an instance with an id value more than zero
   *
   * @param t        the instance to updateAsync
   * @param database writable sql database
   * @return true if updated
   */
  override fun onUpdate(t: T, database: SQLiteDatabase): Boolean = try {
    onUpdate(t, database, id.with(t.getId()))
  } catch (tableError: TableError) {
    false
  }

  /**
   * deletes multiple rows from the table where the column matches all the
   * given values in list
   *
   * @param database writable sql database
   * @param column   the matching column
   * @param list     values to match with
   * @param <C>      the type of matching, must not be derived data type
   * @return true if all rows are deleted
  </C> */
  override fun <C> onDelete(database: SQLiteDatabase, column: Column<C>, list: List<out C>): Boolean {
    val deleted: Boolean
    var where = ""
    var i = 0
    val listSize = list.size
    while (i < listSize) {
      val c = list[i]
      where = if (i == listSize - 1) {
        column.name + " " + column.operand + " " + c
      } else column.name + " " + column.operand + " " + c + " OR "
      i++
    }
    deleted = database.delete(name, where, null) >= 0
    return deleted
  }

  /**
   * deleteAsync a row in the table matching condition in the column
   *
   * @param database writable sql database
   * @param column   field to match
   * @return true if row is deleted
   */
  override fun onDelete(database: SQLiteDatabase, column: Column<*>): Boolean {
    val where = column.name + column.operand + column.value()
    return database.delete(name, where, null) >= 0
  }

  /**
   * deleteAsync an instance from the table
   *
   * @param t        instance to be removed must have an id more than zero
   * @param database writable sql database
   * @return true if item is deleted
   */
  override fun onDelete(t: T, database: SQLiteDatabase): Boolean =
      onDelete(database, id.with(t.getId()))

  /**
   * deleteAsync all rows in the table
   *
   * @param database writable sql database
   * @return true if all rows are deleted
   */
  override fun onDelete(database: SQLiteDatabase): Boolean =
      !TextUtils.isEmpty(name) && database.delete(name, null, null) >= 0

  /**
   * saveAsync an instance to the database
   * serialize it to content values
   *
   * @param t        instance to be saved
   * @param database writable sql database
   * @return id of the row affected
   */
  override fun onSave(t: T, database: SQLiteDatabase): Long {
    if (t.getId() != 0 && onUpdate(t, database)) return t.getId().toLong()
    val values = serialize(t)
    values.put(createdAt.name, System.currentTimeMillis())
    values.put(updatedAt.name, System.currentTimeMillis())
    return database.insert(name, null, values)
  }

  /**
   * saveAsync a list of items in the database
   *
   * @param list     items to be saved
   * @param database writable sql database
   * @return true if all the items are saved
   */
  override fun onSave(list: IdentifiableList<out T>, database: SQLiteDatabase): Boolean {
    var saved = true
    var i = 0
    val listSize = list.size
    while (i < listSize) {
      val t = list[i]
      saved = saved && onSave(t, database) > 0
      i++
    }
    /*if (close) database.close();*/return saved
  }

  /**
   * drop this table from the database
   *
   * @param database writable sql database
   * @return true if the table is dropped
   * @throws TableError if theirs an sql error
   */
  @Throws(TableError::class)
  override fun onDrop(database: SQLiteDatabase): Boolean {
    val sql = "$DROP_PREFIX$name;"
    try {
      database.execSQL(sql)
    } catch (e: SQLException) {
      throw TableError(e)
    }
    return true
  }

  /**
   * get the last id of the last row in the table
   * uses projection to count the id column as num
   *
   * @param database readable sql database
   * @return the id
   */
  override fun onGetLastId(database: SQLiteDatabase): Int {
    val builder: QueryBuilder = QueryBuilder().from(this).select(Projection.count(id).`as`("num"))
    @SuppressLint("Recycle") val cursor = database.rawQuery(builder.build(), builder.buildParameters())
    return cursor.getInt(id.index)
  }

  /**
   * backup all the items in the table during dangerous upgrdes
   *
   * @param database readable database instance
   */
  fun backup(database: SQLiteDatabase) {
    backup = IdentifiableList()
    backup!!.addAll(onFindAll(database, false))
  }

  /**
   * saveAsync back the backed items to the database
   *
   * @param database writable database
   */
  fun restore(database: SQLiteDatabase) {
    if (backup != null && !backup!!.isEmpty()) {
      onSave(backup!!, database)
      backup!!.clear()
    }
    backup = null
  }

  /**
   * gets a single from the cursor pre populated with id and timestamps
   *
   * @param cursor the serialized version of the instance
   * @return instance from the cursor
   */
  private fun getWithId(cursor: Cursor): T {
    var t = deserialize(cursor)
    t!!.setId(cursor.getInt(id.index))
    if (t is ITimeAware) {
      val iTimeAware = t as ITimeAware
      iTimeAware.setCreatedAt(cursor.getInt(createdAt.getIndex(cursor)).toLong())
      iTimeAware.setUpdatedAt(cursor.getInt(updatedAt.getIndex(cursor)).toLong())
      t = iTimeAware as T
    }
    return t
  }

  /**
   * This class contains special queries for reading from the table
   * see [TimeAware] for encapsulating id and timestamps
   * see [DoubleConverter] for serializing and de-serializing
   *
   * @param <Q> The type of the items in the table
  </Q> */
  private abstract inner class QueryExtras<Q : Identifiable<Int>> internal constructor(
      /**
       * the database instance to readAsync fromm
       */
      private val database: SQLiteDatabase) : TableCrud.Extras<Q>, DoubleConverter<Q, Cursor, ContentValues> {

    fun database(): SQLiteDatabase = database

    /**
     * get a record pre populated with id and timestamps from each readAsync
     *
     * @param cursor serialized version of Q
     * @return Q the de-serialized output of reading the cursor
     */
    fun getWithId(cursor: Cursor): Q {
      var t = deserialize(cursor)
      t!!.setId(cursor.getInt(id.index))
      if (t is ITimeAware) {
        val iTimeAware = t as ITimeAware
        iTimeAware.setCreatedAt(cursor.getInt(createdAt.getIndex(cursor)).toLong())
        iTimeAware.setUpdatedAt(cursor.getInt(updatedAt.getIndex(cursor)).toLong())
        t = iTimeAware as Q
      }
      return t
    }

    /**
     * get the first record in the table
     *
     * @return the first records or null if theirs none in the table
     */
    override fun first(): Q? {
      val cursor: Cursor
      return try {
        val builder: QueryBuilder = QueryBuilder().from(this@FastTable).take(1)
        cursor = database.rawQuery(builder.build(), builder.buildParameters())
        getWithId(cursor)
      } catch (e: SQLiteException) {
        LogUtil.e(TAG, e)
        null
      }
    }

    /**
     * get the last record in the table
     *
     * @return an item or null if theirs none stored in the table
     */
    override fun last(): Q? {
      val cursor: Cursor
      return try {
        val builder: QueryBuilder = QueryBuilder().from(this@FastTable).orderByDescending(id).take(1)
        cursor = database.rawQuery(builder.build(), builder.buildParameters())
        getWithId(cursor)
      } catch (e: SQLiteException) {
        LogUtil.e(TAG, e)
        null
      }
    }

    /**
     * get all the items in the table
     *
     * @return the items or an empty list if theirs none
     */
    override fun all(): IdentifiableList<out Q> {
      val cursor: Cursor
      return try {
        val builder: QueryBuilder = QueryBuilder().from(this@FastTable).takeAll()
        cursor = database.rawQuery(builder.build(), builder.buildParameters())
        val ts = IdentifiableList<Q>()
        while (cursor.moveToNext() && !cursor.isClosed) ts.add(getWithId(cursor))
        ts
      } catch (e: SQLiteException) {
        IdentifiableList<Q>()
      }
    }

    /**
     * readAsync the top items in the table
     *
     * @param limit the number of records to readAsync
     * @return a list of the items
     */
    override fun limit(limit: Int): IdentifiableList<out Q> {
      val cursor: Cursor
      return try {
        val builder: QueryBuilder = QueryBuilder().from(this@FastTable).take(limit)
        cursor = database.rawQuery(builder.build(), builder.buildParameters())
        val ts = IdentifiableList<Q>()
        while (cursor.moveToNext() && !cursor.isClosed) ts.add(getWithId(cursor))
        ts
      } catch (e: SQLiteException) {
        LogUtil.e(TAG, e)
        IdentifiableList<Q>()
      }
    }

    /**
     * reads the records between the skip and limit in the table
     *
     * @param skip  of set from the top to not readAsync
     * @param limit items to load after skip
     * @return a list of records
     */
    override fun paginate(skip: Int, limit: Int): IdentifiableList<out Q> {
      val cursor: Cursor
      return try {
        val builder: QueryBuilder = QueryBuilder().from(this@FastTable).take(limit).skip(skip)
        cursor = database.rawQuery(builder.build(), builder.buildParameters())
        val ts = IdentifiableList<Q>()
        while (cursor.moveToNext() && !cursor.isClosed) ts.add(getWithId(cursor))
        ts
      } catch (e: SQLiteException) {
        LogUtil.e(TAG, e)
        IdentifiableList<Q>()
      }
    }

    override fun paginateDescending(skip: Int, limit: Int): IdentifiableList<out Q> {
      val cursor: Cursor
      return try {
        val builder: QueryBuilder = QueryBuilder().from(this@FastTable).orderByDescending(id).take(limit).skip(skip)
        cursor = database.rawQuery(builder.build(), builder.buildParameters())
        val ts = IdentifiableList<Q>()
        while (cursor.moveToNext() && !cursor.isClosed) ts.add(getWithId(cursor))
        ts
      } catch (e: SQLiteException) {
        LogUtil.e(TAG, e)
        IdentifiableList<Q>()
      }
    }

    /**
     * gets all items that match in between the int left and right
     *
     * @param column column to match between
     * @param a      lower between bound
     * @param b      upper between bound
     * @return a list of items
     */
    override fun <N : Number> between(column: Column<N>, a: N, b: N): IdentifiableList<out Q> {
      val cursor: Cursor
      return try {
        val builder: QueryBuilder = QueryBuilder().from(this@FastTable).takeAll().whereAnd(Criteria.between(column, a, b))
        cursor = database.rawQuery(builder.build(), builder.buildParameters())
        val ts = IdentifiableList<Q>()
        while (cursor.moveToNext() && !cursor.isClosed) ts.add(getWithId(cursor))
        ts
      } catch (e: SQLiteException) {
        LogUtil.e(TAG, e)
        IdentifiableList<Q>()
      }
    }

    /**
     * gets all items matching the multiple columns
     *
     * @param column fields to match their values
     * @return a list of items
     */
    override fun where(vararg column: Column<*>): IdentifiableList<out Q> {
      val cursor: Cursor
      return try {
        val builder: QueryBuilder = QueryBuilder().from(this@FastTable).takeAll()
        for (column1 in column) if (column1.value() != null) builder.whereAnd(Criteria.equals(column1, column1.value()))
        cursor = database.rawQuery(builder.build(), builder.buildParameters())
        val ts = IdentifiableList<Q>()
        while (cursor.moveToNext() && !cursor.isClosed) ts.add(getWithId(cursor))
        ts
      } catch (e: SQLiteException) {
        LogUtil.e(TAG, e)
        IdentifiableList<Q>()
      }
    }

    /**
     * gets all the items matching not in any of the columns
     *
     * @param column field to match
     * @param bounds not in bounds
     * @return a list of items
     */
    @SafeVarargs
    override fun <N : Number> notIn(column: Column<N>, vararg bounds: N): IdentifiableList<out Q> {
      val cursor: Cursor
      val items = arrayOfNulls<Any>(bounds.size)
      System.arraycopy(bounds, 0, items, 0, bounds.size)
      val builder: QueryBuilder = QueryBuilder().from(this@FastTable).takeAll()
          .whereAnd(Criteria.notIn(column, items))
      return try {
        cursor = database.rawQuery(builder.build(), builder.buildParameters())
        val ts = IdentifiableList<Q>()
        while (cursor.moveToNext() && !cursor.isClosed) {
          val t = getWithId(cursor)
          ts.add(t)
        }
        cursor.close()
        /*database.close();*/ts
      } catch (e: SQLiteException) {
        LogUtil.e(TAG, e)
        IdentifiableList<Q>()
      }
    }

    /**
     * get all the rows where the column is like the columns values
     *
     * @param column the fields to compute like from
     * @return a list of columns
     */
    override fun like(vararg column: Column<*>): IdentifiableList<out Q> {
      val cursor: Cursor
      val builder: QueryBuilder = QueryBuilder().from(this@FastTable).takeAll()
      for (column1 in column) builder.whereAnd(Criteria.contains(column1, column1.value().toString()))
      return try {
        cursor = database.rawQuery(builder.build(), builder.buildParameters())
        val ts = IdentifiableList<Q>()
        while (cursor.moveToNext() && !cursor.isClosed) {
          val t = getWithId(cursor)
          ts.add(t)
        }
        cursor.close()
        /*database.close();*/ts
      } catch (e: SQLiteException) {
        LogUtil.e(TAG, e)
        IdentifiableList<Q>()
      }
    }

    /**
     * get all the rows in the oder specified by the column
     *
     * @param column field to order by
     * @return a list of ordered items
     */
    override fun orderBy(column: Column<*>): IdentifiableList<out Q> {
      val cursor: Cursor
      val builder: QueryBuilder = QueryBuilder().from(this@FastTable).takeAll()
      if (column.order() == Column.DESCENDING) {
        builder.orderByDescending(column)
      } else builder.orderByAscending(column)
      return try {
        cursor = database.rawQuery(builder.build(), builder.buildParameters())
        val ts = IdentifiableList<Q>()
        while (cursor.moveToNext() && !cursor.isClosed) {
          val t = getWithId(cursor)
          ts.add(t)
        }
        cursor.close()
        /*database.close();*/ts
      } catch (e: SQLiteException) {
        LogUtil.e(TAG, e)
        IdentifiableList<Q>()
      }
    }

    /**
     * gets all the items grouped by the column
     *
     * @param column field to group by
     * @return a list of grouped items
     */
    override fun groupBy(column: Column<*>): IdentifiableList<out Q> {
      val cursor: Cursor
      val builder: QueryBuilder = QueryBuilder().from(this@FastTable).takeAll().groupBy(column)
      return try {
        cursor = database.rawQuery(builder.build(), builder.buildParameters())
        val ts = IdentifiableList<Q>()
        while (cursor.moveToNext() && !cursor.isClosed) {
          val t = getWithId(cursor)
          ts.add(t)
        }
        cursor.close()
        /*database.close();*/ts
      } catch (e: SQLiteException) {
        LogUtil.e(TAG, e)
        IdentifiableList<Q>()
      }
    }

    /**
     * gets all the items grouped and ordered by the two columns
     *
     * @param column  group by field
     * @param column1 order by fields
     * @return a list of items
     */
    override fun groupAndOrderBy(column: Column<*>, column1: Column<*>): IdentifiableList<out Q> {
      val cursor: Cursor
      val builder: QueryBuilder = QueryBuilder().from(this@FastTable).takeAll().groupBy(column)
      if (column1.order() == Column.DESCENDING) {
        builder.orderByDescending(column1)
      } else builder.orderByAscending(column1)
      return try {
        cursor = database.rawQuery(builder.build(), builder.buildParameters())
        val ts = IdentifiableList<Q>()
        while (cursor.moveToNext() && !cursor.isClosed) {
          val t = getWithId(cursor)
          ts.add(t)
        }
        cursor.close()
        /*database.close();*/ts
      } catch (e: SQLiteException) {
        LogUtil.e(TAG, e)
        IdentifiableList<Q>()
      }
    }

  }

}