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

interface DMLFunctions<T : Identifiable<Int>> {

  fun transact(block: FastTable<T>.() -> Unit)

  fun querySql(sql: String): Cursor

  fun save(t: T): Long

  fun saveAsync(t: T): Single<Long>

  fun save(list: IdentifiableList<out T>): Boolean

  fun saveAsync(list: IdentifiableList<out T>): Single<Boolean>

  fun update(t: T): Boolean

  fun updateAsync(t: T): Maybe<Boolean>

  fun update(t: T, column: Column<*>): Boolean

  fun updateAsync(t: T, column: Column<*>): Maybe<Boolean>

  fun queryBuilder(): QueryBuilder

  fun query(queryBuilder: QueryBuilder): Cursor

  fun queryAsync(queryBuilder: QueryBuilder): Single<Cursor>

  fun find(): TableCrud.Extras<T>

  fun findById(idLong: Long): T?

  fun findOne(vararg columns: Column<*>): T?

  @Throws(TableError::class)
  fun findAsync(): ReactiveTable.Extras<T>

  fun findAll(): IdentifiableList<out T>

  fun findAllAsync(): Maybe<IdentifiableList<out T>>

  fun findAll(vararg column: Column<*>): IdentifiableList<out T>

  fun findAllAsync(vararg column: Column<*>): Maybe<IdentifiableList<out T>>

  fun delete(column: Column<*>): Boolean

  fun deleteAsync(column: Column<*>): Maybe<Boolean>

  fun delete(t: T): Boolean

  fun deleteAsync(t: T): Maybe<Boolean>

  fun <N> delete(column: Column<N>, list: List<out N>): Boolean

  fun <C> deleteAsync(column: Column<C>, list: List<out C>): Maybe<Boolean>

  fun clear(): Boolean

  fun clearAsync(): Maybe<Boolean>

  val lastId: Int

  val lastIdAsync: Maybe<Int>
}