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

interface DDLFunctions<in X> {
  /**
   * creates the table in the database
   *
   * @param x the database instance
   * @return true if the table is created
   * @throws TableError if theirs an error creating the table
   */
  @Throws(TableError::class)
  fun onCreate(x: X): Boolean

  /**
   * upgraded the table from one version to the next
   *
   * @param x  database instance
   * @param v1 last version of [X]
   * @param v2 next version of [X]
   * @throws TableError if theirs problem running the migration
   */
  @Throws(TableError::class)
  fun onUpgrade(x: X, v1: Int, v2: Int)
}