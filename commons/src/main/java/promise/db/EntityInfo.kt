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

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class AddedEntity(val fromVersion: Int,
                             val toVersion: Int)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Entity(val tableName: String = "",
                        /**
                         * @return
                         */
                        val compoundIndices: Array<CompoundIndex> = []
) {
  /**
   *
   */
  @Target(AnnotationTarget.FIELD)
  @Retention(AnnotationRetention.SOURCE)
  annotation class CompoundIndex(
      val columns: Array<String>,
      /**
       * @return
       */
      val unique: Boolean = false)

  /**
   *
   */

}

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class Index

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class ForeignKey(
    /**
     * @return
     */
    val referencedEntity: KClass<*>,
    /**
     * @return
     */
    val referencedEntityColumnName: String = "id")


@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class HasOne

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class HasMany



