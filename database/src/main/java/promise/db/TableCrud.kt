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
import promise.commons.util.DoubleConverter
import promise.model.IdentifiableList
import promise.utils.Acceptor
import promise.utils.Visitor

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
interface TableCrud<T : Identifiable<Int>, X> : DoubleConverter<T, Cursor, ContentValues>,
    DDLFunctions<X>,
    Acceptor<TableCrud<T, X>, Any> {

  override fun accept(t: Visitor<TableCrud<T, X>, Any>): Any = t.visit(this)

  fun acceptErasure(t: Visitor<TableCrud<*, X>, Any>): Any = t.visit(this)

  fun queryBuilder(): QueryBuilder

  fun single(cursor: Cursor): T

  fun collection(cursor: Cursor): IdentifiableList<out T>


  val TAG: String

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
    fun between(column: Column<Number>, a: Number, b: Number): IdentifiableList<out T>

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
    fun notIn(column: Column<Number>, vararg bounds: Number): IdentifiableList<out T>

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