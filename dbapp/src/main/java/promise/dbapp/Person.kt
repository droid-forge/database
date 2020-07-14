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

package promise.dbapp

import promise.commons.model.Identifiable
import promise.db.MigrationAction
import promise.db.MigrationActions
import promise.db.Persistable
import promise.db.Number
import promise.db.PrimaryKeyAutoIncrement
import promise.db.Varchar
import promise.dbapp.model.Cat

@Persistable(name = "persons")
class Person: Identifiable<Int> {
    @Varchar(name = "nm", length = 40) var name: String? = null

    @MigrationAction(from = 2, to = 3, action = MigrationActions.DROP)
    @Number(name = "ag") var age: Int? = null

    @MigrationAction(from = 1, to = 2, action = MigrationActions.CREATE)
    var marks: Int? = null

    var cat: Cat? = null

    @PrimaryKeyAutoIncrement
    var uid: Int? = null

    override fun getId(): Int {
        return uid!!
    }

    override fun setId(t: Int) {

    }
}