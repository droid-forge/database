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
import promise.dbapp.model.Cat

@Entity(
    tableName = "p",
    compoundIndices = [
      Entity.CompoundIndex(columns = ["a", "m"]),
      Entity.CompoundIndex(columns = ["n", "m"], unique = true)
    ]
)
@AddedEntity(fromVersion = 2, toVersion = 3)
class Person : Identifiable<Int> {

  @VarChar(columnName = "n", length = 40, unique = true)
  @Index(true)
  var name: String? = null

  @Migrate(fromVersion = 3, toVersion = 4, action = MigrationOptions.DROP)
  @Number(columnName = "a")
  var age: Int? = null

  @Migrations([
    Migrate(fromVersion = 4, toVersion = 5, action = MigrationOptions.CREATE),
    Migrate(fromVersion = 7, toVersion = 8, action = MigrationOptions.DROP)
  ])

  @ForeignKey(referencedEntity = Cat::class, referencedEntityColumnName = "legs")
  @Number(columnName = "m")
  var marks: Int? = null

  @OneToOne
  var cat: Cat? = null

  @PrimaryKeyAutoIncrement
  var uid: Int? = null

  override fun getId(): Int = uid!!

  override fun setId(t: Int) {
    this.uid = t
  }
}