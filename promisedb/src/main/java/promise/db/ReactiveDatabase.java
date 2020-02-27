/*
 *
 *  * Copyright 2017, Peter Vincent
 *  * Licensed under the Apache License, Version 2.0, Promise.
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package promise.db;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;

import androidx.annotation.Nullable;

import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import promise.commons.Promise;
import promise.commons.data.log.LogUtil;
import promise.commons.model.Identifiable;
import promise.commons.model.List;
import promise.commons.model.Result;
import promise.commons.model.function.MapFunction;
import promise.commons.util.DoubleConverter;
import promise.db.query.QueryBuilder;
import promise.model.SList;
import promise.model.SModel;

/**
 *
 */
public abstract class ReactiveDatabase extends FastDatabase implements ReactiveCrud<SQLiteDatabase> {

  private String TAG = LogUtil.makeTag(ReactiveDatabase.class);

  /**
   * @param name
   * @param factory
   * @param version
   * @param errorHandler
   */
  private ReactiveDatabase(
      String name,
      SQLiteDatabase.CursorFactory factory,
      int version,
      DatabaseErrorHandler errorHandler) {
    super(name, factory, version, errorHandler);
    LogUtil.d(TAG, "fast db init");
    Promise.instance().listen(LogUtil.makeTag(Promise.class), new Result<>()
        .withCallBack(o -> {
          if (o instanceof String) {
            if (o.equals(Promise.CLEANING_UP_RESOURCES)) {
              CompositeDisposable disposable = onTerminate();
              if (disposable != null) disposable.dispose();
            }
          }
        }));
  }

  /**
   * @param name
   * @param version
   * @param cursorListener
   * @param listener
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public ReactiveDatabase(String name, int version, final DatabaseCursorFactory.Listener cursorListener, final Corrupt listener) {
    this(
        name,
        cursorListener != null ? new DatabaseCursorFactory(cursorListener) : null,
        version,
        dbObj -> {
          assert listener != null;
          listener.onCorrupt();
        });
  }

  /**
   * @param version
   */
  public ReactiveDatabase(int version) {
    this(DEFAULT_NAME, version, null, null);
  }

  /**
   * @param table
   * @param database
   * @param <T>
   * @return
   */
  private <T extends Identifiable<Integer>> Single<Boolean> dropAsync(Table<T, ? super SQLiteDatabase> table, SQLiteDatabase database) {
    return Single.fromCallable(() -> table.onDrop(database));
  }

  /**
   * @param builder
   * @return
   */
  public Single<Cursor> queryAsync(final QueryBuilder builder) {
    return Single.fromCallable(() -> {
      String sql = builder.build();
      String[] params = builder.buildParameters();
      return getReadableDatabase().rawQuery(sql, params);
    });
  }

  /**
   * @param table
   * @param <T>
   * @return
   */
  @Override
  public <T extends SModel> ReactiveTable.Extras<T> readAsync(Table<T, ? super SQLiteDatabase> table) throws ModelError {
    Model<T> tTable = model(checkTableExist(table));
    return new QueryExtras<T>(tTable, getReadableDatabase()) {
      @Override
      public ContentValues serialize(T t) {
        return tTable.serialize(t);
      }

      @Override
      public T deserialize(Cursor cursor) {
        return tTable.deserialize(cursor);
      }
    };
  }

  /**
   * @param table
   * @param <T>
   * @return
   */
  @Override
  public <T extends Identifiable<Integer>> Maybe<SList<? extends T>> readAllAsync(Table<T, ? super SQLiteDatabase> table) {
    return Maybe.fromCallable(() -> readAll(table));
  }

  /**
   * @param table
   * @param column
   * @param <T>
   * @return
   */
  @Override
  public <T extends Identifiable<Integer>> Maybe<SList<? extends T>> readAllAsync(Table<T, ? super SQLiteDatabase> table, Column... column) {
    return Maybe.fromCallable(() -> table.onReadAll(getReadableDatabase(), column));
  }

  /**
   * @param t
   * @param table
   * @param column
   * @param <T>
   * @return
   */
  @Override
  public <T extends Identifiable<Integer>> Maybe<Boolean> updateAsync(T t, Table<T, ? super SQLiteDatabase> table, Column column) {
    return Maybe.fromCallable(() -> table.onUpdate(t, getWritableDatabase(), column));
  }

  /**
   * @param t
   * @param table
   * @param <T>
   * @return
   */
  @Override
  public <T extends Identifiable<Integer>> Maybe<Boolean> updateAsync(T t, Table<T, ? super SQLiteDatabase> table) {
    return Maybe.fromCallable(() -> table.onUpdate(t, getWritableDatabase()));
  }

  /**
   * @param table
   * @param column
   * @param <T>
   * @return
   */
  @Override
  public <T extends Identifiable<Integer>> Maybe<Boolean> deleteAsync(Table<T, ? super SQLiteDatabase> table, Column column) {
    return Maybe.fromCallable(() -> table.onDelete(getWritableDatabase(), column));
  }

  /**
   * @param table
   * @param t
   * @param <T>
   * @return
   */
  @Override
  public <T extends Identifiable<Integer>> Maybe<Boolean> deleteAsync(Table<T, ? super SQLiteDatabase> table, T t) {
    return Maybe.fromCallable(() -> table.onDelete(t, getWritableDatabase()));
  }

  /**
   * @param table
   * @return
   */
  @Override
  public Maybe<Boolean> deleteAsync(Table<?, ? super SQLiteDatabase> table) {
    return Maybe.fromCallable(() -> table.onDelete(getWritableDatabase()));
  }

  /**
   * @param table
   * @param column
   * @param list
   * @param <C>
   * @return
   */
  @Override
  public <C> Maybe<Boolean> deleteAsync(Table<?, ? super SQLiteDatabase> table, Column<C> column, List<? extends C> list) {
    return Maybe.fromCallable(() -> table.onDelete(getWritableDatabase(), column, list));
  }

  /**
   * @param t
   * @param table
   * @param <T>
   * @return
   */
  @Override
  public <T extends Identifiable<Integer>> Single<Long> saveAsync(T t, Table<T, ? super SQLiteDatabase> table) {
    return Single.fromCallable(() -> table.onSave(t, getWritableDatabase()));
  }

  /**
   * @param list
   * @param table
   * @param <T>
   * @return
   */
  @Override
  public <T extends Identifiable<Integer>> Single<Boolean> saveAsync(SList<? extends T> list, Table<T, ? super SQLiteDatabase> table) {
    return Single.fromCallable(() -> table.onSave(list, getWritableDatabase()));
  }

  /**
   * @return
   */
  @Override
  public Maybe<Boolean> deleteAllAsync() {
    return Maybe.zip(tables().map((MapFunction<MaybeSource<?>, Table<?, ? super SQLiteDatabase>>) this::deleteAsync),
        objects -> List.fromArray(objects).allMatch(aBoolean -> aBoolean instanceof Boolean &&
            (Boolean) aBoolean));
  }

  /**
   * @param table
   * @param <T>
   * @return
   */
  @Override
  public <T extends Identifiable<Integer>> Maybe<Integer> getLastIdAsync(Table<T, ? super SQLiteDatabase> table) {
    return Maybe.fromCallable(() -> table.onGetLastId(getReadableDatabase()));
  }

  /*private IndexCreated getIndexCreated(Table<?, SQLiteDatabase> table) {
    for (Iterator<Map.Entry<IndexCreated, Table<?, SQLiteDatabase>>> iterator =
            indexCreatedTableHashMap.entrySet().iterator();
        iterator.hasNext(); ) {
      Map.Entry<IndexCreated, Table<?, SQLiteDatabase>> entry = iterator.next();
      Table<?, SQLiteDatabase> table1 = entry.getValue();
      if (table1.getName().equalsIgnoreCase(table.getName())) return entry.getKey();
    }
    return new IndexCreated(0, false);
  }*/

  /**
   * @return
   */
  public abstract CompositeDisposable onTerminate();

  /**
   * @param <T>
   * @param table
   * @return
   * @throws ModelError
   */
  private <T extends SModel> Model<T> model(final Table<T, ? super SQLiteDatabase> table) throws ModelError {
    if (!(table instanceof Model))
      throw new ModelError(new IllegalStateException("Passed table not a model instance"));
    return (Model<T>) table;
  }

  /**
   * @param table
   * @return
   * @throws ModelError
   */
  private Model modelWithoutType(final Table<?, ? super SQLiteDatabase> table) throws ModelError {
    if (!(table instanceof Model))
      throw new ModelError(new IllegalStateException("Passed table not a model instance"));
    return (Model<?>) table;
  }

  private static class IndexCreated {
    int id;
    boolean created;

    IndexCreated(int id, boolean created) {
      this.id = id;
      this.created = created;
    }

    @Override
    public boolean equals(Object object) {
      if (this == object) return true;
      if (!(object instanceof IndexCreated)) return false;
      IndexCreated that = (IndexCreated) object;
      return id == that.id && created == that.created;
    }

    @Override
    public int hashCode() {
      int result = id;
      result = 31 * result + (created ? 1 : 0);
      return result;
    }
  }

  /**
   * @param <T>
   */
  private abstract class QueryExtras<T extends Identifiable<Integer>> implements ReactiveTable.Extras<T>, DoubleConverter<T, Cursor, ContentValues> {

    private SQLiteDatabase database;
    private Table<T, ? super SQLiteDatabase> table;

    QueryExtras(Table<T, ? super SQLiteDatabase> table, SQLiteDatabase database) {
      this.table = table;
      this.database = database;
    }

    /**
     * @return
     */
    @Nullable
    @Override
    public Maybe<T> first() {
      return Maybe.fromCallable(() -> table.read(database).first());
    }

    /**
     * @return
     */
    @Nullable
    @Override
    public Maybe<T> last() {
      return Maybe.fromCallable(() -> table.read(database).last());
    }

    /**
     * @return
     */
    @Override
    public Maybe<SList<? extends T>> all() {
      return Maybe.fromCallable(() -> table.read(database).all());
    }

    /**
     * @param limit
     * @return
     */
    @Override
    public Maybe<SList<? extends T>> limit(final int limit) {
      return Maybe.fromCallable(() -> table.read(database).limit(limit));
    }

    /**
     * @param skip
     * @param limit
     * @return
     */
    @Override
    public Maybe<SList<? extends T>> paginate(final int skip, final int limit) {
      return Maybe.fromCallable(() -> table.read(database).paginate(skip, limit));
    }

    /**
     * @param column
     * @param a
     * @param b
     * @return
     */
    @Override
    public <N extends Number> Maybe<SList<? extends T>> between(final Column<N> column, final N a, final N b) {
      return Maybe.fromCallable(() -> table.read(database).between(column, a, b));
    }

    /**
     * @param column
     * @return
     */
    @Override
    public Maybe<SList<? extends T>> where(final Column[] column) {
      return Maybe.fromCallable(() -> table.read(database).where(column));
    }

    /**
     * @param column
     * @param bounds
     * @return
     */
    @SafeVarargs
    @Override
    public final <N extends Number> Maybe<SList<? extends T>> notIn(final Column<N> column, final N... bounds) {
      return Maybe.fromCallable(() -> table.read(database).notIn(column, bounds));
    }

    /**
     * @param column
     * @return
     */
    @Override
    public Maybe<SList<? extends T>> like(final Column[] column) {
      return Maybe.fromCallable(() -> table.read(database).like(column));
    }

    /**
     * @param column
     * @return
     */
    @Override
    public Maybe<SList<? extends T>> orderBy(final Column column) {
      return Maybe.fromCallable(() -> table.read(database).orderBy(column));
    }

    /**
     * @param column
     * @return
     */
    @Override
    public Maybe<SList<? extends T>> groupBy(final Column column) {
      return Maybe.fromCallable(() -> table.read(database).groupBy(column));
    }

    /**
     * @param column
     * @param column1
     * @return
     */
    @Override
    public Maybe<SList<? extends T>> groupAndOrderBy(final Column column, final Column column1) {
      return Maybe.fromCallable(() -> table.read(database).groupAndOrderBy(column, column1));
    }
  }

}
