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

import android.os.Parcel
import promise.commons.model.Identifiable
import promise.commons.util.Conditions
import promise.model.TimeStamped
import java.lang.Exception

/**
 *
 */
abstract class ActiveRecord<T: Identifiable<Int>>() : TimeStamped() {
  /**
   *
   */
  internal var table: FastTable<T>? = null

  /**
   *
   */
  constructor(parcel: Parcel?): this() {
    super.readFromParcel(parcel)
  }

  /**
   *
   */
  @Throws(Exception::class)
  fun save(): Long {
    Conditions.checkNotNull(table, "This record has not been read from the database and can't be saved")
    return table!!.save(getEntity())
  }

  /**
   *
   */
  @Throws(Exception::class)
  fun update(): Boolean {
    Conditions.checkNotNull(table, "This record has not been read from the database and can't be updated")
    return table!!.update(getEntity())
  }

  /**
   *
   */
  @Throws(Exception::class)
  fun delete(): Boolean {
    Conditions.checkNotNull(table, "This record has not been read from the database and can't be deleted")
    return table!!.delete(getEntity())
  }

  /**
   *
   */
  abstract fun getEntity(): T
}