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

import android.os.Parcel
import android.os.Parcelable
import promise.commons.model.List
import promise.db.Ignore
import promise.db.Migrate
import promise.db.Migrations
import promise.db.Number
import promise.db.Persistable
import promise.db.VarChar
import promise.model.TimeAware
import java.sql.Blob

@Persistable("complex_records")
class ComplexRecord constructor() : TimeAware() {

  var intVariable: Int? = null

  @Number
  var floatVariable: Float? = null

  @Ignore
  var doubleVariable: Double? = null

  @VarChar(length = 244, unique = true)
  var stringVariable: String? = null

  @Migrate(from = 1, to = 2, action = Migrations.CREATE)
  var booleanVariable: Boolean = false

  // added new field
  var flagString: String? = null
  @promise.db.Blob
  var picBlob: Blob? = null


  constructor(source: Parcel) : this() {
    intVariable = source.readInt()
  }

  override fun describeContents() = 0

  override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {}

  override fun toString(): String =
      "Complex(intVariable=$intVariable, floatVariable=$floatVariable, doubleVariable=$doubleVariable, stringVariable=$stringVariable)\n"

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<ComplexRecord> = object : Parcelable.Creator<ComplexRecord> {
      override fun createFromParcel(source: Parcel): ComplexRecord = ComplexRecord(source)
      override fun newArray(size: Int): Array<ComplexRecord?> = arrayOfNulls(size)
    }

    fun someModels(): List<ComplexRecord> = List.fromArray(ComplexRecord().apply {
      intVariable = 1
      floatVariable = 0.2f
      doubleVariable = 3.567
      stringVariable = "some string"
    }, ComplexRecord().apply {
      intVariable = 2
      floatVariable = 0.5f
      doubleVariable = 3.87
      stringVariable = "some string 2"
    }, ComplexRecord().apply {
      intVariable = 5
      floatVariable = 0.7f
      doubleVariable = 10.987655
      stringVariable = "some string 3"
    }, ComplexRecord().apply {
      intVariable = 10
      floatVariable = 62f
      doubleVariable = 567.7865
      stringVariable = "some string 4"
    }, ComplexRecord().apply {
      intVariable = 18
      floatVariable = 100.3f
      doubleVariable = 456.987
      stringVariable = "some string 5"
    },ComplexRecord().apply {
      intVariable = 5
      floatVariable = 0.7f
      doubleVariable = 10.987655
      stringVariable = "some string 3"
    }, ComplexRecord().apply {
      intVariable = 10
      floatVariable = 62f
      doubleVariable = 567.7865
      stringVariable = "some string 4"
    }, ComplexRecord().apply {
      intVariable = 18
      floatVariable = 100.3f
      doubleVariable = 456.987
      stringVariable = "some string 5"
    },ComplexRecord().apply {
      intVariable = 5
      floatVariable = 0.7f
      doubleVariable = 10.987655
      stringVariable = "some string 3"
    }, ComplexRecord().apply {
      intVariable = 10
      floatVariable = 62f
      doubleVariable = 567.7865
      stringVariable = "some string 4"
    }, ComplexRecord().apply {
      intVariable = 18
      floatVariable = 100.3f
      doubleVariable = 456.987
      stringVariable = "some string 5"
    },ComplexRecord().apply {
      intVariable = 5
      floatVariable = 0.7f
      doubleVariable = 10.987655
      stringVariable = "some string 3"
    }, ComplexRecord().apply {
      intVariable = 10
      floatVariable = 62f
      doubleVariable = 567.7865
      stringVariable = "some string 4"
    }, ComplexRecord().apply {
      intVariable = 18
      floatVariable = 100.3f
      doubleVariable = 456.987
      stringVariable = "some string 5"
    },ComplexRecord().apply {
      intVariable = 5
      floatVariable = 0.7f
      doubleVariable = 10.987655
      stringVariable = "some string 3"
    }, ComplexRecord().apply {
      intVariable = 10
      floatVariable = 62f
      doubleVariable = 567.7865
      stringVariable = "some string 4"
    }, ComplexRecord().apply {
      intVariable = 18
      floatVariable = 100.3f
      doubleVariable = 456.987
      stringVariable = "some string 5"
    },ComplexRecord().apply {
      intVariable = 5
      floatVariable = 0.7f
      doubleVariable = 10.987655
      stringVariable = "some string 3"
    }, ComplexRecord().apply {
      intVariable = 10
      floatVariable = 62f
      doubleVariable = 567.7865
      stringVariable = "some string 4"
    }, ComplexRecord().apply {
      intVariable = 18
      floatVariable = 100.3f
      doubleVariable = 456.987
      stringVariable = "some string 5"
    })
  }
}