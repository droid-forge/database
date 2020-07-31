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
import io.reactivex.Maybe
import io.reactivex.Single
import promise.commons.model.Identifiable
import promise.commons.model.List
import promise.model.IdentifiableList

/**
 *
 */
interface DMLFunctions<T : Identifiable<Int>> {

  /**
   *
   */
  fun querySql(sql: String): Cursor

  /**
   * @param t
   * @return
   */
  fun save(t: T): Long

  /**
   * @param t
   * @return
   */
  fun saveAsync(t: T): Single<Long>

  /**
   * @param list
   * @return
   */
  fun save(list: IdentifiableList<out T>): Boolean

  /**
   * @param list
   * @return
   */
  fun saveAsync(list: IdentifiableList<out T>): Single<Boolean>

  /**
   * @param t
   * @return
   */
  fun update(t: T): Boolean

  /**
   * @param t
   * @return
   */
  fun updateAsync(t: T): Maybe<Boolean>

  /**
   * @param t
   * @param column
   * @return
   */
  fun update(t: T, column: Column<*>): Boolean

  /**
   * @param t
   * @param column
   * @return
   */
  fun updateAsync(t: T, column: Column<*>): Maybe<Boolean>

  /**
   *
   */
  fun queryBuilder(): QueryBuilder

  /**
   *
   */
  fun query(queryBuilder: QueryBuilder): Cursor

  /**
   *
   */
  fun queryAsync(queryBuilder: QueryBuilder): Single<Cursor>

  /**
   * @return
   */
  fun find(): TableCrud.Extras<T>

  /**
   * @return
   */
  fun findById(idLong: Long): T?

  /**
   * @return
   */
  fun findOne(vararg columns: Column<*>): T?

  /**
   * @return
   * @throws TableError
   */
  @Throws(TableError::class)
  fun findAsync(): ReactiveTable.Extras<T>

  /**
   * @return
   */
  fun findAll(): IdentifiableList<out T>

  /**
   * @return
   */
  fun findAllAsync(): Maybe<IdentifiableList<out T>>

  /**
   * @param column
   * @return
   */
  fun findAll(vararg column: Column<*>): IdentifiableList<out T>

  /**
   * @param column
   * @return
   */
  fun findAllAsync(vararg column: Column<*>): Maybe<IdentifiableList<out T>>

  /**
   * @param column
   * @return
   */
  fun delete(column: Column<*>): Boolean

  /**
   * @param column
   * @return
   */
  fun deleteAsync(column: Column<*>): Maybe<Boolean>

  /**
   * @param t
   * @return
   */
  fun delete(t: T): Boolean

  /**
   * @param t
   * @return
   */
  fun deleteAsync(t: T): Maybe<Boolean>

  /**
   * @param column
   * @param list
   * @param <N>
   * @return
  </N> */
  fun <N> delete(column: Column<N>, list: List<out N>): Boolean

  /**
   * @param column
   * @param list
   * @param <C>
   * @return
  </C> */
  fun <C> deleteAsync(column: Column<C>, list: List<out C>): Maybe<Boolean>

  /**
   * @return
   */
  fun clear(): Boolean

  /**
   * @return
   */
  fun clearAsync(): Maybe<Boolean>

  /**
   * @return
   */
  val lastId: Int

  /**
   * @return
   */
  val lastIdAsync: Maybe<Int>
}