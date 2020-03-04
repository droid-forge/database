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
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import promise.commons.Promise
import promise.commons.model.List
import promise.commons.model.Result
import promise.db.ReactiveFastDatabase
import promise.db.Table
import promise.model.IdentifiableList

class AppDatabase : ReactiveFastDatabase(name, version, null, null) {
  /**
   *
   */
  private val compositeDisposable: CompositeDisposable by lazy { CompositeDisposable() }

  /**
   * @return
   */
  override fun onTerminate(): CompositeDisposable {
    return compositeDisposable
  }

  /**
   *
   * @return
   */
  override fun tables(): List<Table<*, in SQLiteDatabase>> = List.fromArray(complexModelTable)

  fun allComplexModels(result: Result<IdentifiableList<out ComplexRecord>, Throwable>) {
    compositeDisposable.add(complexModelTable.findAllAsync()
        .subscribeOn(Schedulers.from(Promise.instance().executor()))
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ list ->
          if (list.isEmpty())
            saveSomeComplexModels(Result<Boolean, Throwable>()
                .withErrorCallBack { allComplexModels(result) }
                .withErrorCallBack {
                  result.error(it)
                }) else
            result.response(IdentifiableList(list))

        }, {
          result.error(it)
        }))
  }

  private fun saveSomeComplexModels(result: Result<Boolean, Throwable>) {
    compositeDisposable.add(
        complexModelTable.saveAsync(IdentifiableList(ComplexRecord.someModels()))
            .subscribeOn(Schedulers.from(Promise.instance().executor()))
            .observeOn(Schedulers.from(Promise.instance().executor()))
            .subscribe({
              result.response(it)
            }, {
              result.error(it)
            })
    )
  }

  override fun deleteAllAsync(): Maybe<Boolean> =
      super.deleteAllAsync().subscribeOn(Schedulers.from(Promise.instance().executor()))

  companion object {
    @Volatile
    var instance: AppDatabase? = null
    private var LOCK = Any()
    operator fun invoke(): AppDatabase = instance
        ?: synchronized(LOCK) {
          instance ?: AppDatabase()
              .also {
                instance = it
              }
        }

    const val name = "complex_db_name"
    const val version = 1
    val complexModelTable: ComplexRecordTable by lazy { ComplexRecordTable(AppDatabase()) }
  }
}