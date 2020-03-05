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
import io.reactivex.Single
import promise.commons.model.Identifiable
import promise.commons.model.List
import promise.model.IdentifiableList

internal interface ReactiveCrud<X> {
  @Throws(TableError::class)
  fun <T : Identifiable<Int>> readAsync(tableCrud: TableCrud<T, in X>): ReactiveTable.Extras<T>
  fun <T : Identifiable<Int>> readAllAsync(tableCrud: TableCrud<T, in X>): Maybe<IdentifiableList<out T>>
  fun <T : Identifiable<Int>> readAllAsync(tableCrud: TableCrud<T, in X>, vararg column: Column<*>): Maybe<IdentifiableList<out T>>
  fun <T : Identifiable<Int>> updateAsync(t: T, tableCrud: TableCrud<T, in X>, column: Column<*>): Maybe<Boolean>
  fun <T : Identifiable<Int>> updateAsync(t: T, tableCrud: TableCrud<T, in X>): Maybe<Boolean>
  fun <T : Identifiable<Int>> deleteAsync(tableCrud: TableCrud<T, in X>, column: Column<*>): Maybe<Boolean>
  fun <T : Identifiable<Int>> deleteAsync(tableCrud: TableCrud<T, in X>, t: T): Maybe<Boolean>
  fun deleteAsync(tableCrud: TableCrud<*, in X>): Maybe<Boolean>
  fun <C> deleteAsync(tableCrud: TableCrud<*, in X>, column: Column<C>, list: List<out C>): Maybe<Boolean>
  fun <T : Identifiable<Int>> saveAsync(t: T, tableCrud: TableCrud<T, in X>): Single<Long>
  fun <T : Identifiable<Int>> saveAsync(list: IdentifiableList<out T>, tableCrud: TableCrud<T, in X>): Single<Boolean>
  fun deleteAllAsync(): Maybe<Boolean>
  fun <T : Identifiable<Int>> getLastIdAsync(tableCrud: TableCrud<T, in X>): Maybe<Int>
}