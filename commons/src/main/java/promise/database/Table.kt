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
package promise.database

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Table(val tableName: String,
                       val indices: Array<Index> = [],
                       val compoundIndexes: Array<CompoundIndex> = [],
                       val foreignKeys: Array<ForeignKey> = []) {
  @Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
  @Retention(AnnotationRetention.RUNTIME)
  annotation class Index(val columnName: String)

  @Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
  @Retention(AnnotationRetention.RUNTIME)
  annotation class CompoundIndex(val unique: Boolean = false,
                                 val indexes: Array<Index> = [])

  @Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
  @Retention(AnnotationRetention.RUNTIME)
  annotation class ForeignKey(val columnName: String,
                              val referencedTableName: String,
                              val referencedColumnName: String)
}