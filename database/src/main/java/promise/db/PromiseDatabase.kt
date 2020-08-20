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
import promise.utils.Visitor

/**
 * Base class for classes annotated with DatabaseEntity
 */
abstract class PromiseDatabase(private val fastDatabase: FastDatabase?) {

  /**
   * @return an instance of FastDatabase
   */
  val databaseInstance: FastDatabase
    get() {
      checkNotNull(fastDatabase) { "Database not initialized or created yet" }
      return fastDatabase
    }

  abstract fun <T : Identifiable<Int>> getEntityClassVisitor(): Visitor<Class<out T>, FastTable<T>>

  /**
   * returns the table associated with the entity class
   *
   * @param entityClass class of the entity persisted
   * @param <T>         entity
   * @return FastTable of the entity
   * @throws IllegalArgumentException if entity is not registered with the database
  </T> */
  @Throws(IllegalArgumentException::class)
  fun <T : Identifiable<Int>> tableOf(entityClass: Class<out T>): FastTable<T> {
    val t: Visitor<Class<out T>, FastTable<T>> = getEntityClassVisitor()
    return t.visit(entityClass)
  }

}