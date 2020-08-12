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

package promise.database.compiler.migration

import promise.database.MigrationOptions

class TableMigration {
   var versionChange: VersionChange? = null
   var field: String? = null
   var action: MigrationOptions? = null

   override fun equals(other: Any?): Boolean {
      if (javaClass != other?.javaClass) return false
      other as TableMigration

      if (versionChange != other.versionChange) return false
      if (field != other.field) return false
      if (action != other.action) return false
      return true
   }

   override fun hashCode(): Int {
      var result = versionChange?.hashCode() ?: 0
      result = 31 * result + (field?.hashCode() ?: 0)
      result = 31 * result + (action?.hashCode() ?: 0)
      return result
   }
}