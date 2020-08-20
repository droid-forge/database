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
import android.database.Cursor
import android.database.SQLException
import androidx.sqlite.db.SupportSQLiteDatabase
import io.reactivex.Maybe
import io.reactivex.Single
import promise.commons.data.log.LogUtil
import promise.commons.model.Identifiable
import promise.commons.model.List
import promise.commons.model.List.fromArray
import promise.commons.util.Conditions
import promise.database.Table
import promise.model.ITimeStamped
import promise.model.IdentifiableList
import java.util.*

internal const val INDEXES = "indexes"
internal const val COMPOUND_INDEXES = "compound_indexes"
internal const val FOREIGN_kEYS = "foreign_keys"

/**
 * This class models database queries
 *
 * @param <T> [Identifiable] instance to be persisted by the model
</T> */
@Suppress("UNCHECKED_CAST")
abstract class FastTable<T : Identifiable<Int>>
/**
 * @param database
 */(

    val database: FastDatabase) : TableCrud<T, SupportSQLiteDatabase>, DMLFunctions<T> {

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
    @JvmStatic
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

  override fun single(cursor: Cursor): T {
    cursor.moveToFirst()
    return deserialize(cursor)
  }

  override fun collection(cursor: Cursor): IdentifiableList<out T> {
    val collection = IdentifiableList<T>()
    while (cursor.moveToNext() && !cursor.isClosed) {
      collection.add(getWithId(cursor))
    }
    cursor.close()
    return collection
  }

  open fun createEntityInstance(): T {
    throw RuntimeException("STUB ONLY SUPPORTED FOR ACTIVE RECORD ENTITIES")
  }

  fun create(block: T.() -> Unit): T {
    val instance = createEntityInstance()
    block.invoke(instance)
    instance.setId(save(instance).toInt())
    (instance as ActiveRecord<T>).table = this
    return instance
  }

  /**
   * @return
   */
  /**
   * The specific tag for logging in this table
   */
  override val TAG: String = LogUtil.makeTag(FastTable::class.java) + name

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

  private lateinit var args: Map<String, Any>

  internal fun setArgs(args: Map<String, Any>) {
    this.args = args
  }

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

  private fun generateCompoundIndexQuery(compoundIndex: Table.CompoundIndex): String {
    var columnNames = ""
    var indexSql = "("
    val indexes = compoundIndex.indexes
    indexes.forEachIndexed { i: Int, index: Table.Index ->
      columnNames = if (i == indexes.size - 1) columnNames + index.columnName else "$columnNames${index.columnName}_"
      indexSql = if (i == indexes.size - 1) indexSql + index.columnName else "$indexSql${index.columnName}, "
    }
    indexSql = "$indexSql);"
    return if (compoundIndex.unique) {
      "CREATE UNIQUE INDEX IF NOT EXISTS idx_$columnNames ON $nameOfTable $indexSql"
    } else "CREATE INDEX IF NOT EXISTS idx_$columnNames ON $nameOfTable $indexSql"
  }

  private fun generateIndexQueryForTableIndex(index: Table.Index): String {
    val indexSql = "(${index.columnName});"
    return "CREATE INDEX IF NOT EXISTS idx_${index.columnName} ON $nameOfTable $indexSql"
  }

  private fun generateIndexQuery(index: String): String {
    val indexSql = "(${index});"
    return "CREATE INDEX IF NOT EXISTS idx_${index} ON $nameOfTable $indexSql"
  }

  fun addForeignKet(x: SupportSQLiteDatabase,
                    column: Column<*>,
                    referenceTable: String,
                    referenceColumn: Column<*>) {
    val sql = "ALTER TABLE $name ADD FOREIGN KEY (${column.name}) REFERENCES $referenceTable(${referenceColumn.name});"
    x.execSQL(sql)
  }

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
   * @param x writable sql database
   * @return true if table is created
   * @throws TableError if theirs a query error
   */
  @Throws(TableError::class)
  override fun onCreate(x: SupportSQLiteDatabase): Boolean {
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
      x.execSQL(sql)
      val arrayOfCompoundIndices = args[COMPOUND_INDEXES] as Array<Table.CompoundIndex>
      if (!arrayOfCompoundIndices.isNullOrEmpty()) addCompoundIndices(x, arrayOfCompoundIndices)
      val arrayOfIndices = args[INDEXES] as Array<Table.Index>
      if (!arrayOfIndices.isNullOrEmpty()) addIndices(x, arrayOfIndices)
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
  override fun onUpgrade(x: SupportSQLiteDatabase, v1: Int, v2: Int) {
    val builder: QueryBuilder = queryBuilder()
    @SuppressLint("Recycle") val c = x.query(builder.build(), builder.buildParameters())
    val set: Set<String> = HashSet(fromArray(*c.columnNames))
    if (!set.contains(createdAt.name)) addColumns(x, createdAt)
    if (!set.contains(updatedAt.name)) addColumns(x, updatedAt)
  }

  /**
   * adds column to the database
   *
   * @param database writable sql database
   * @param columns  fields to be added must be nullable entry types
   * @throws TableError if theirs an sql error
   */
  @Throws(TableError::class)
  fun addColumns(database: SupportSQLiteDatabase, vararg columns: Column<*>) {
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

  @Throws(TableError::class)
  private fun addCompoundIndices(database: SupportSQLiteDatabase, indexes: Array<Table.CompoundIndex>) {
    indexes.forEach {
      val indexSql = generateCompoundIndexQuery(it)
      LogUtil.d(TAG, indexSql)
      database.execSQL(indexSql)
    }
  }

  private fun addIndices(database: SupportSQLiteDatabase, indexes: Array<Table.Index>) {
    indexes.forEach {
      val indexSql = generateIndexQueryForTableIndex(it)
      LogUtil.d(TAG, indexSql)
      database.execSQL(indexSql)
    }
  }

  fun addIndex(database: SupportSQLiteDatabase, index: String) {
    val indexSql: String = generateIndexQuery(index)
    LogUtil.d(TAG, indexSql)
    database.execSQL(indexSql)
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
  fun dropColumns(database: SupportSQLiteDatabase, vararg columns: String): Boolean {
    for (column in columns) {
      val alterSql = "$ALTER_COMMAND `$name` DROP COLUMN $column;"
      try {
        database.execSQL(alterSql)
      } catch (e: SQLException) {
        throw TableError(e)
      }
    }
    return true
  }

  @Throws(TableError::class)
  fun renameColumns(database: SupportSQLiteDatabase, vararg columns: Pair<String, String>): Boolean {
    for (column in columns) {
      val alterSql = "$ALTER_COMMAND `$name` SET COLUMN ${column.first} ${column.second};"
      try {
        database.execSQL(alterSql)
      } catch (e: SQLException) {
        throw TableError(e)
      }
    }
    return true
  }

  override fun querySql(sql: String) = database.querySql(sql)

  /**
   * @param t
   * @return
   */
  override fun save(t: T): Long = database.save(t, this)

  /**
   * @param t
   * @return
   */
  override fun saveAsync(t: T): Single<Long> = reactiveDatabase.saveAsync(t, this)

  /**
   * @param list
   * @return
   */
  override fun save(list: IdentifiableList<out T>): Boolean = database.save(list, this)

  /**
   * @param list
   * @return
   */
  override fun saveAsync(list: IdentifiableList<out T>): Single<Boolean> =
      reactiveDatabase.saveAsync(list, this)

  /**
   * @param t
   * @return
   */
  override fun update(t: T): Boolean = database.update(t, this)

  /**
   * @param t
   * @return
   */
  override fun updateAsync(t: T): Maybe<Boolean> = reactiveDatabase.updateAsync(t, this)

  /**
   * @param t
   * @param column
   * @return
   */
  override fun update(t: T, column: Column<*>): Boolean = database.update(t, this, column)

  /**
   * @param t
   * @param column
   * @return
   */
  override fun updateAsync(t: T, column: Column<*>): Maybe<Boolean> =
      reactiveDatabase.updateAsync(t, this, column)

  /**
   *
   */
  override fun queryBuilder(): QueryBuilder = QueryBuilder().from(this)

  /**
   *
   */
  override fun query(queryBuilder: QueryBuilder): Cursor = database.query(queryBuilder)

  /**
   *
   */
  override fun queryAsync(queryBuilder: QueryBuilder): Single<Cursor> =
      reactiveDatabase.queryAsync(queryBuilder)

  /**
   * @return
   */
  override fun find(): TableCrud.Extras<T> = database.find(this)

  /**
   * @return
   */
  override fun findById(idLong: Long): T? = database.find(this).where(id.with(idLong.toInt())).first()

  /**
   * @return
   */
  override fun findOne(vararg columns: Column<*>): T? = database.find(this).where(*columns).first()

  /**
   * @return
   * @throws TableError
   */
  @Throws(TableError::class)
  override fun findAsync(): ReactiveTable.Extras<T> = reactiveDatabase.readAsync(this)

  /**
   * @return
   */
  override fun findAll(): IdentifiableList<out T> = database.findAll(this)

  /**
   * @return
   */
  override fun findAllAsync(): Maybe<IdentifiableList<out T>> = reactiveDatabase.readAllAsync(this)

  /**
   * @param column
   * @return
   */
  override fun findAll(vararg column: Column<*>): IdentifiableList<out T> = database.findAll(this, *column)

  /**
   * @param column
   * @return
   */
  override fun findAllAsync(vararg column: Column<*>): Maybe<IdentifiableList<out T>> =
      reactiveDatabase.readAllAsync(this, *column)

  /**
   * @param column
   * @return
   */
  override fun delete(column: Column<*>): Boolean = database.delete(this, column)

  /**
   * @param column
   * @return
   */
  override fun deleteAsync(column: Column<*>): Maybe<Boolean> = reactiveDatabase.deleteAsync(this, column)

  /**
   * @param t
   * @return
   */
  override fun delete(t: T): Boolean = database.delete(this, t)

  /**
   * @param t
   * @return
   */
  override fun deleteAsync(t: T): Maybe<Boolean> = reactiveDatabase.deleteAsync(this, t)

  /**
   * @param column
   * @param list
   * @param <N>
   * @return
  </N> */
  override fun <N> delete(column: Column<N>, list: List<out N>): Boolean =
      database.delete(this, column, list)

  /**
   * @param column
   * @param list
   * @param <C>
   * @return
  </C> */
  override fun <C> deleteAsync(column: Column<C>, list: List<out C>): Maybe<Boolean> =
      reactiveDatabase.deleteAsync(this, column, list)

  /**
   * @return
   */
  override fun clear(): Boolean = database.delete(this)

  /**
   * @return
   */
  override fun clearAsync(): Maybe<Boolean> = reactiveDatabase.deleteAsync(this)

  /**
   * @return
   */
  override val lastId: Int
    get() = database.getLastId(this)

  /**
   * @return
   */
  override val lastIdAsync: Maybe<Int>
    get() = reactiveDatabase.getLastIdAsync(this)

  /**
   *
   */
  override fun transact(block: FastTable<T>.() -> Unit) = synchronized(this) {
    val db = database.writableDatabase
    try {
      db.beginTransaction()
      block.invoke(this)
      db.setTransactionSuccessful()
    } finally {
      db.endTransaction()
    }
  }

  /**
   * drop this table from the database
   *
   * @param x writable sql database
   * @return true if the table is dropped
   * @throws TableError if theirs an sql error
   */
  @Throws(TableError::class)
  override fun onDrop(x: SupportSQLiteDatabase): Boolean {
    val sql = "$DROP_PREFIX$name;"
    try {
      x.execSQL(sql)
    } catch (e: SQLException) {
      throw TableError(e)
    }
    return true
  }

  /**
   * backup all the items in the table during dangerous upgrdes
   *
   * @param database readable database instance
   */
  fun backup(database: SupportSQLiteDatabase) {
    backup = IdentifiableList()
    backup!!.addAll(accept(FetchAllVisitor(database, null)) as IdentifiableList<T>)
  }

  /**
   * saveAsync back the backed items to the database
   *
   * @param database writable database
   */
  fun restore(database: SupportSQLiteDatabase) {
    if (backup != null && !backup!!.isEmpty()) {
      accept(SaveListVisitor<T>(database, backup!!))
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
    t.setId(cursor.getInt(id.index))
    if (t is ITimeStamped) {
      val iTimeAware = t as ITimeStamped
      iTimeAware.setCreatedAt(cursor.getInt(createdAt.getIndex(cursor)).toLong())
      iTimeAware.setUpdatedAt(cursor.getInt(updatedAt.getIndex(cursor)).toLong())
      t = iTimeAware as T
    }
    if (t is ActiveRecord<*>) (t as ActiveRecord<T>).table = this
    return t
  }

}