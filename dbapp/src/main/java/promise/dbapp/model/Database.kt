package promise.dbapp.model

import android.database.sqlite.SQLiteDatabase
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import promise.commons.Promise
import promise.model.SList
import promise.db.ReactiveDatabase
import promise.db.Table
import promise.commons.model.List
import promise.commons.model.Result

class Database : ReactiveDatabase(name, version, null, null) {
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
   * @param database
   * @param oldVersion
   * @param newVersion
   * @return
   */
  override fun onUpgradeDatabase(database: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

  }


  /**
   *
   * @return
   */
  override fun tables(): List<Table<*, in SQLiteDatabase>> = List.fromArray(complexModelTable)

  fun allComplexModels(result: Result<SList<out ComplexModel>, Throwable>) {
    compositeDisposable.add(readAllAsync(complexModelTable)
        .subscribeOn(Schedulers.from(Promise.instance().executor()))
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ list ->
          if (list.isEmpty())
            saveSomeComplexModels(Result<Boolean, Throwable>()
                .withErrorCallBack { allComplexModels(result) }
                .withErrorCallBack {
                  result.error(it) }) else
            result.response(SList(list))

        }, {
          result.error(it)
        }))
  }

  private fun saveSomeComplexModels(result: Result<Boolean, Throwable>) {
    compositeDisposable.add(saveAsync(SList(ComplexModel.someModels()), complexModelTable)
        .subscribeOn(Schedulers.from(Promise.instance().executor()))
        .observeOn(Schedulers.from(Promise.instance().executor()))
        .subscribe({
          result.response(it)
        }, {
          result.error(it)
        }))
  }

  override fun deleteAllAsync(): Maybe<Boolean> =
      super.deleteAllAsync().subscribeOn(Schedulers.from(Promise.instance().executor()))

  companion object {
    const val name = "complex_db_name"
    const val version = 1
    private val complexModelTable: ComplexModelTable by lazy { ComplexModelTable() }
  }
}