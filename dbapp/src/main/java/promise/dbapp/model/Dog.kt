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

package promise.dbapp.model

import promise.commons.model.Identifiable
import promise.db.*
import promise.db.Number

@Entity(
    tableName = "d",
    compoundIndices = [
      Entity.CompoundIndex(columns = ["a", "c"], unique = true)
    ]
)
@AddedEntity(fromVersion = 5, toVersion = 6)
class Dog : Identifiable<Int> {

  @VarChar(columnName = "c", length = 40, unique = true)
  @Index(true)
  var color: String? = null

  @Number(columnName = "a")
  var age: Int? = null

  @PrimaryKeyAutoIncrement
  var uid: Int? = null

  override fun getId(): Int = uid!!

  override fun setId(t: Int) {
    this.uid = t
  }
}