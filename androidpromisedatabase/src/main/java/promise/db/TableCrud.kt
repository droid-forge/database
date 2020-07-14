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

import android.content.ContentValues
import android.database.Cursor
import promise.commons.model.Identifiable
import promise.commons.model.List
import promise.commons.util.DoubleConverter
import promise.model.IdentifiableList

/**
 * the abstract implementation of all possible operations to be done on an table
 * see [android.database.sqlite.SQLiteDatabase] for a sample
 * of type of [X]
 * Each extending class must implement [DoubleConverter.serialize] method to
 * convert the [Identifiable] instance to a content value see [ContentValues]
 * and [DoubleConverter.deserialize]
 * method to deserialize a [Cursor] back to the instance
 *
 * @param <T> the type to be stored in this particular table must be a type of [Identifiable]
 * @param <X> the type of database that this table resides in
</X></T> */
interface TableCrud<T : Identifiable<Int>, X> : DoubleConverter<T, Cursor, ContentValues> {
  /**
   * creates the table in the database
   *
   * @param x the database instance
   * @return true if the table is created
   * @throws TableError if theirs an error creating the table
   */
  @Throws(TableError::class)
  fun onCreate(x: X): Boolean

  /**
   * upgraded the table from one version to the next
   *
   * @param x  database instance
   * @param v1 last version of [X]
   * @param v2 next version of [X]
   * @throws TableError if theirs problem running the migration
   */
  @Throws(TableError::class)
  fun onUpgrade(x: X, v1: Int, v2: Int)

  /**
   * provides concise readAsync from the table
   * see [Extras]
   *
   * @param x the database instance
   * @return an instance of extras with more concise methods for querying the data set
   */
  fun onFind(x: X): Extras<T>

  /**
   * gets all the records in the table
   *
   * @param x     the database instance
   * @param close if the connection is to be closed
   * @return a list if the records
   */
  fun onFindAll(x: X, close: Boolean): IdentifiableList<out T>

  /**
   * gets all the records matching the conditions specified in the columns
   *
   * @param x      database instance
   * @param column specifies where or order by conditions
   * @return a list if records
   */
  fun onFindAll(x: X, vararg column: Column<*>): IdentifiableList<out T>

  /**
   * updates an item in the table specified by the condition in the column
   * see [Column.getOperand] for sample conditional operations
   *
   * @param t      record to updateAsync
   * @param x      database instance
   * @param column field specifying the updateAsync condition
   * @return true id record is updated
   * @throws TableError if there is missing updating information or error in sql script
   */
  @Throws(TableError::class)
  fun onUpdate(t: T, x: X, column: Column<*>): Boolean

  /**
   * updates an item with an id with a value more that zero
   * see [FastTable.id]
   *
   * @param t record to updateAsync
   * @param x database instance
   * @return true if record is updated otherwise false
   */
  fun onUpdate(t: T, x: X): Boolean

  /**
   * deleteAsync a record from the table specified by the condition in the column
   * see [Column.getOperand] for sample conditional operations
   *
   * @param x      database instance
   * @param column field to infer where condition
   * @return true if record deleted otherwise false
   */
  fun onDelete(x: X, column: Column<*>): Boolean

  /**
   * deletes an item with an id with a value more that zero
   * see [FastTable.id]
   *
   * @param t record to updateAsync
   * @param x database instance
   * @return true if record is deleted otherwise false
   */
  fun onDelete(t: T, x: X): Boolean

  /**
   * truncates the table
   *
   * @param x database instance
   * @return true if truncated otherwise false
   */
  fun onDelete(x: X): Boolean

  /**
   * deletes a number of rows where the column matches each item in list
   *
   * @param x      database instance
   * @param column field to match each item in list
   * @param list   matches to deleteAsync from
   * @param <C>    type of match must not be derived
   * @return true if deleted otherwise false
  </C> */
  fun <C> onDelete(x: X, column: Column<C>, list: List<out C>): Boolean

  /**
   * saves an item in the table
   * if it has an id more than zero, it updates it instead
   *
   * @param t item to saveAsync
   * @param x database instance
   * @return reference of affected row
   */
  fun onSave(t: T, x: X): Long

  /**
   * saveAsync multiple items to the table
   *
   * @param list items to saveAsync
   * @param x    database instance
   * @return true if all items are saved otherwise false
   */
  fun onSave(list: IdentifiableList<out T>, x: X): Boolean

  /**
   * @param x
   * @return
   * @throws TableError
   */
  @Throws(TableError::class)
  fun onDrop(x: X): Boolean

  fun onGetLastId(x: X): Int
  /**
   * @return tableName of this table
   */
  val name: String

  /**
   * @param <T>
  </T> */
  interface Extras<T : Identifiable<Int>> {
    /**
     * @return
     */
    fun first(): T?

    /**
     * @return
     */
    fun last(): T?

    /**
     * @return
     */
    fun all(): IdentifiableList<out T>

    /**
     * @param limit
     * @return
     */
    fun limit(limit: Int): IdentifiableList<out T>

    /**
     * @param skip
     * @param limit
     * @return
     */
    fun paginate(skip: Int, limit: Int): IdentifiableList<out T>

    /**
     * @param skip
     * @param limit
     * @return
     */
    fun paginateDescending(skip: Int, limit: Int): IdentifiableList<out T>

    /**
     * @param column
     * @param a
     * @param b
     * @return
     */
    fun <N : Number> between(column: Column<N>, a: N, b: N): IdentifiableList<out T>

    /**
     * @param column
     * @return
     */
    fun where(vararg column: Column<*>): IdentifiableList<out T>

    /**
     * @param column
     * @param bounds
     * @return
     */
    fun <N : Number> notIn(column: Column<N>, vararg bounds: N): IdentifiableList<out T>

    /**
     * @param column
     * @return
     */
    fun like(vararg column: Column<*>): IdentifiableList<out T>

    /**
     * @param column
     * @return
     */
    fun orderBy(column: Column<*>): IdentifiableList<out T>

    /**
     * @param column
     * @return
     */
    fun groupBy(column: Column<*>): IdentifiableList<out T>

    /**
     * @param column
     * @param column1
     * @return
     */
    fun groupAndOrderBy(column: Column<*>, column1: Column<*>): IdentifiableList<out T>
  }
}