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

package com.dev4vin.dbappbase.model

import promise.commons.model.Identifiable
import promise.db.AddedEntity
import promise.db.Entity
import promise.db.Index
import promise.db.PrimaryKeyAutoIncrement

@Entity(
    tableName = "songs"
)
class Song : Identifiable<Int> {

  @Index(true)
  var name: String? = null

  var type: String? = null

  @PrimaryKeyAutoIncrement
  var uid: Int? = null

  override fun getId(): Int = uid!!

  override fun setId(t: Int) {
    this.uid = t
  }
}