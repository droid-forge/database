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

package promise.base

import promise.database.TypeConverter
import java.util.*

@TypeConverter
class AppTypeConverter {

  fun dateToString(date: Date?): String = (date ?: Date()).time.toString()

  fun stringToDate(data: String): Date = Date(data.toLong())

  fun toUniqueId(data: String): ID = ID(data)

  fun toString(data: ID?): String = data?.id ?: ""

}