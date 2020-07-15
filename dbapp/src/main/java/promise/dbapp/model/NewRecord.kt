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
import promise.commons.model.List
import promise.db.Persistable
import promise.db.PrimaryKeyAutoIncrement

// added when version of database is 1
@Persistable(name = "new_records")
class NewRecord : Identifiable<Int> {

  @PrimaryKeyAutoIncrement
  var pkId = 0
  override fun getId(): Int = pkId

  override fun setId(t: Int) {
    pkId = t
  }

  var toDoName: String? = null
  var todoDesc: String? = null

  companion object {

    fun someModels(): List<NewRecord> = List.fromArray(NewRecord().apply {
      toDoName = "toda name 1"
      todoDesc = "desc 1"
    }, NewRecord().apply {
      toDoName = "toda name 2"
      todoDesc = "desc 3"
    })
  }
}