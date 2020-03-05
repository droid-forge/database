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

import android.database.sqlite.SQLiteDatabase
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import promise.commons.Promise
import promise.commons.data.log.LogUtil
import promise.commons.model.Result
import promise.db.FastDatabase
import promise.db.Migration
import promise.db.Database
import promise.db.FastDatabase.Companion.createDatabase
import promise.model.IdentifiableList

@Database(
    name = "complex_db_name",
    version = 2,
    tables = [
      ComplexRecordTable::class,
      NewRecordTable::class
    ]
)
object AppDatabase {

  fun allComplexModels(result: Result<IdentifiableList<out ComplexRecord>, Throwable>) {
    val items = complexModelTable.findAll()
    if (items.isEmpty()) {
      saveSomeComplexModels(Result<Boolean, Throwable>()
          .withCallBack {
            allComplexModels(result)
          })
      return
    }
    result.response(items)
   }

  private fun saveSomeComplexModels(result: Result<Boolean, Throwable>) {
    complexModelTable.save(IdentifiableList(ComplexRecord.someModels()))
    result.response(true)
  }

  val instance = createDatabase(AppDatabase::class.java,
      object : Migration {
        override fun onMigrate(database: FastDatabase,
                               sqLiteDatabase: SQLiteDatabase,
                               oldVersion: Int,
                               newVersion: Int) {
          if (oldVersion == 1 && newVersion == 2) {
            // we added new record table when database version is 1, and therefore need to add it version 2
            database.add(sqLiteDatabase, database.obtain(NewRecordTable::class.java))
          }
        }
      })

  val complexModelTable: ComplexRecordTable by lazy {
    instance.obtain<ComplexRecordTable>(ComplexRecordTable::class.java)
  }
  val newRecordTable: NewRecordTable by lazy {
    instance.obtain<NewRecordTable>(NewRecordTable::class.java)
  }
}