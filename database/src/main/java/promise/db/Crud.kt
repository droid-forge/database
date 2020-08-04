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

import promise.commons.model.Identifiable
import promise.commons.model.List
import promise.model.IdentifiableList

/**
 *
 */
interface Crud<X> {


  /**
   *
   */
  fun <T : Identifiable<Int>> find(tableCrud: TableCrud<T, in X>): TableCrud.Extras<T>

  /**
   *
   */
  fun <T : Identifiable<Int>> findAll(tableCrud: TableCrud<T, in X>): IdentifiableList<out T>

  /**
   *
   */
  fun <T : Identifiable<Int>> update(t: T, tableCrud: TableCrud<T, in X>, column: Column<*>): Boolean

  /**
   *
   */
  fun <T : Identifiable<Int>> update(t: T, tableCrud: TableCrud<T, in X>): Boolean

  /**
   *
   */
  fun <T : Identifiable<Int>> findAll(tableCrud: TableCrud<T, in X>, vararg columns: Column<*>): IdentifiableList<out T>

  /**
   *
   */
  fun delete(tableCrud: TableCrud<*, in X>, column: Column<*>): Boolean

  /**
   *
   */
  fun <T : Identifiable<Int>> delete(tableCrud: TableCrud<T, in X>, t: T): Boolean

  /**
   *
   */
  fun delete(tableCrud: TableCrud<*, in X>): Boolean

  /**
   *
   */
  fun <T> delete(tableCrud: TableCrud<*, in X>, column: Column<T>, list: List<out T>): Boolean

  /**
   *
   */
  fun <T : Identifiable<Int>> save(t: T, tableCrud: TableCrud<T, in X>): Long

  /**
   *
   */
  fun <T : Identifiable<Int>> save(list: IdentifiableList<out T>, tableCrud: TableCrud<T, in X>): Boolean

  /**
   *
   */
  fun deleteAll(): Boolean

  /**
   *
   */
  fun getLastId(tableCrud: TableCrud<*, in X>): Int
}