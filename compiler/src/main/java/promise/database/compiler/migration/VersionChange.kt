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

class VersionChange {
  var fromVersion: Int = 0
  var toVersion: Int = 0
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as VersionChange
    if (fromVersion != other.fromVersion) return false
    if (toVersion != other.toVersion) return false
    return true
  }

  override fun hashCode(): Int {
    var result = fromVersion
    result = 31 * result + toVersion
    return result
  }
}
