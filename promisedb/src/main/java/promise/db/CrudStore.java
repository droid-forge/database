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

package promise.db;

import android.database.sqlite.SQLiteDatabase;

import promise.commons.model.Identifiable;
import promise.commons.model.List;
import promise.commons.model.Result;

/**
 * @param <T>
 */
public class CrudStore<T extends Identifiable<Integer>> implements Store<T, Table<T, ? super SQLiteDatabase>, Throwable> {
  /**
   *
   */
  private Crud<SQLiteDatabase> crudStore;

  /**
   * @param crudStore
   */
  public CrudStore(Crud<SQLiteDatabase> crudStore) {
    this.crudStore = crudStore;
  }

  /**
   * @param tsqLiteDatabaseTable
   * @param callBack
   */
  @Override
  public void get(
      Table<T, ? super SQLiteDatabase> tsqLiteDatabaseTable,
      Result<StoreExtra<T>, Throwable> callBack) {
    StoreExtra.getExtras(crudStore.findAll(tsqLiteDatabaseTable), new StoreFilter<T>() {
      @SafeVarargs
      @Override
      public final <X> List<? extends T> filter(List<? extends T> list, X... x) {
        return list;
      }
    }, callBack);
  }

  /**
   * @param tsqLiteDatabaseTable
   * @param t
   * @param callBack
   */
  @Override
  public void delete(
      Table<T, ? super SQLiteDatabase> tsqLiteDatabaseTable,
      T t,
      Result<Boolean, Throwable> callBack) {
    try {
      callBack.response(crudStore.delete(tsqLiteDatabaseTable, t));
    } catch (Throwable e) {
      callBack.error(e);
    }
  }

  /**
   * @param tsqLiteDatabaseTable
   * @param t
   * @param callBack
   */
  @Override
  public void update(
      Table<T, ? super SQLiteDatabase> tsqLiteDatabaseTable,
      T t,
      Result<Boolean, Throwable> callBack) {
    try {
      callBack.response(crudStore.update(t, tsqLiteDatabaseTable));
    } catch (Throwable e) {
      callBack.error(e);
    }
  }

  /**
   * @param tsqLiteDatabaseTable
   * @param t
   * @param callBack
   */
  @Override
  public void save(
      Table<T, ? super SQLiteDatabase> tsqLiteDatabaseTable,
      T t,
      Result<Boolean, Throwable> callBack) {
    try {
      callBack.response(crudStore.save(t, tsqLiteDatabaseTable) > 0);
    } catch (Throwable e) {
      callBack.error(e);
    }
  }

  /**
   * @param tsqLiteDatabaseTable
   * @param callBack
   */
  @Override
  public void clear(
      Table<T, ? super SQLiteDatabase> tsqLiteDatabaseTable,
      Result<Boolean, Throwable> callBack) {
    try {
      callBack.response(crudStore.delete(tsqLiteDatabaseTable));
    } catch (Throwable e) {
      callBack.error(e);
    }
  }

  /**
   * @param callBack
   */
  @Override
  public void clear(Result<Boolean, Throwable> callBack) {
    try {
      callBack.response(crudStore.deleteAll());
    } catch (Throwable e) {
      callBack.error(e);
    }
  }
}
