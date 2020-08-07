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

import io.reactivex.Maybe
import promise.commons.model.Identifiable
import promise.database.Number
import promise.model.IdentifiableList

/**
 * models concise readAsync queries in an reactive way
 * see [TableCrud]
 */
interface ReactiveTable {
  /**
   * modeling concise readAsync queries
   * see [TableCrud.Extras]
   *
   * @param <T> the type of data to be stored in the table
  </T> */
  interface Extras<T : Identifiable<Int>> {
    /**
     * gets the first record in the table
     * see [TableCrud.Extras.first]
     *
     * @return the first records
     */
    fun first(): Maybe<T>

    /**
     * gets the last record in the table
     * see [TableCrud.Extras.last]
     *
     * @return the last record in the table
     */
    fun last(): Maybe<T>

    /**
     * gets all the records in the table
     * see [TableCrud.Extras.all]
     *
     * @return the list if records
     */
    fun all(): Maybe<IdentifiableList<out T>>

    /**
     * gets the top records marked by the limit
     *
     * @param limit limit to take from the table
     * see [TableCrud.Extras.limit]
     * @return a list of records
     */
    fun limit(limit: Int): Maybe<IdentifiableList<out T>>

    /**
     * paginates the data between the offset and limit
     * see [TableCrud.Extras.paginate]
     *
     * @param skip  offset to take
     * @param limit limit to fetch
     * @return a list of records
     */
    fun paginate(skip: Int, limit: Int): Maybe<IdentifiableList<out T>>

    /**
     * @param skip
     * @param limit
     * @return
     */
    fun paginateDescending(skip: Int, limit: Int): Maybe<IdentifiableList<out T>>

    /**
     * gets data that matches between the values passed in the specified column
     * see [TableCrud.Extras.between]
     *
     * @param column field to match between
     * @param a      lower bound of between
     * @param b      upper bound of between
     * @return a list of matched records
     */
    fun between(column: Column<Number>, a: Number, b: Number): Maybe<IdentifiableList<out T>>

    /**
     * gets all the data matched in where in the columns
     * see [TableCrud.Extras.where]
     *
     * @param column columns to match where clause
     * @return a list of records matching the criteria
     */
    fun where(vararg column: Column<*>): Maybe<IdentifiableList<out T>>

    /**
     * gets all the data not within the specified bounds in the specified column
     * see [TableCrud.Extras.notIn]
     *
     * @param column field to checkout bounds
     * @param bounds specified not in bounds
     * @param <N>    type of bound
     * @return list of records
    </N> */
    fun notIn(column: Column<Number>, vararg bounds: Number): Maybe<IdentifiableList<out T>>

    /**
     * fetches all the data where the column contains the value
     * see [TableCrud.Extras.limit]
     *
     * @param column field to build like clause
     * @return list of records
     */
    fun like(vararg column: Column<String>): Maybe<IdentifiableList<out T>>

    /**
     * fetches all the data in the order specified in the column
     * see [TableCrud.Extras.orderBy]
     *
     * @param column field to build order by clause
     * @return a list of records ordered
     */
    fun orderBy(column: Column<*>): Maybe<IdentifiableList<out T>>

    /**
     * fetches all the data in the group by condition specified in the column
     * see [TableCrud.Extras.groupBy]
     *
     * @param column field to build the group by clause
     * @return a list of records grouped by the condition in the column
     */
    fun groupBy(column: Column<*>): Maybe<IdentifiableList<out T>>

    /**
     * groups and orders the result set by the two provided sets of columns
     * see [TableCrud.Extras.groupAndOrderBy]
     *
     * @param column  field to group by
     * @param column1 field to order by
     * @return a list of data grouped and ordered in the given criteria
     */
    fun groupAndOrderBy(column: Column<*>, column1: Column<*>): Maybe<IdentifiableList<out T>>
  }
}