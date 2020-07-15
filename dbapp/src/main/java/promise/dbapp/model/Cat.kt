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
import promise.db.Persistable
import promise.db.Number
import promise.db.PrimaryKey
import promise.db.PrimaryKeyAutoIncrement
import promise.db.VarChar

@Persistable(name = "cats")
class Cat: Identifiable<Int> {
    @VarChar(length = 20) var name: String? = null
    @Number var age: Int? = null

    @PrimaryKey
    var legs: Int? = null

    var bodyType: String? = null

    @PrimaryKeyAutoIncrement
    var uid: Int? = null

    override fun getId(): Int {
        return uid!!
    }

    override fun setId(t: Int) {

    }
}