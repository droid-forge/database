package promise.db;

import android.database.sqlite.SQLiteDatabase;

import promise.commons.model.List;
import promise.commons.model.Result;
import promise.model.SModel;


/**
 * @param <T>
 */
public class CrudStore<T extends SModel> implements Store<T, Table<T, ? super SQLiteDatabase>, Throwable> {
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
      Result<Extras<T>, Throwable> callBack) {
    new StoreExtra<T, Throwable>() {
      @SafeVarargs
      @Override
      public final <Y> List<? extends T> filter(List<? extends T> list, Y... y) {
        return list;
      }
    }.getExtras(crudStore.readAll(tsqLiteDatabaseTable), callBack);
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
