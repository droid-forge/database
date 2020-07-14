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

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Persistable(val name: String = "",
                             /**
                              * @return
                              */
                             val indexes: Array<EntityIndex> = [],
                             /**
                              * @return
                              */
                             val foreignKeys: Array<EntityForeignKey> = []
) {
  /**
   *
   */
  @Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
  @Retention(AnnotationRetention.SOURCE)
  annotation class EntityIndex(
      /**
       * @return
       */
      val columnName: String = "",
      /**
       * @return
       */
      val unique: Boolean = false)

  /**
   *
   */
  @Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
  @Retention(AnnotationRetention.SOURCE)
  annotation class EntityForeignKey(
      /**
       * @return
       */
      val columnName: String,
      /**
       * @return
       */
      val referencedTableName: String,
      /**
       * @return
       */
      val referencedColumnName: String)
}
