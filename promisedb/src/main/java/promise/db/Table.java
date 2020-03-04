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
 *
 *
 */

package promise.db;


import androidx.annotation.Nullable;

import promise.commons.model.Identifiable;
import promise.commons.model.List;
import promise.model.SList;

/**
 * the abstract implementation of all possible operations to be done on an table
 * see {@link android.database.sqlite.SQLiteDatabase} for a sample
 * of type of {@link X}
 * @param <T> the type to be stored in this particular table must be a type of {@link Identifiable}
 * @param <X> the type of database that this table resides in
 */
public interface Table<T extends Identifiable<Integer>, X> {
  /** creates the table in the database
   * @param x the database instance
   * @return true if the table is created
   * @throws ModelError if theirs an error creating the table
   */

  boolean onCreate(X x) throws ModelError;

  /** upgraded the table from one version to the next
   * @param x database instance
   * @param v1 last version of {@link X}
   * @param v2 next version of {@link X}
   * @return true if table is updated
   * @throws ModelError if theirs problem running the migration
   */
  void onUpgrade(X x, int v1, int v2) throws ModelError;

  /** provides concise readAsync from the table
   * see {@link Extras}
   * @param x the database instance
   * @return an instance of extras with more concise methods for querying the data set
   */
  Extras<T> find(X x);

  /** gets all the records in the table
   * @param x the database instance
   * @param close if the connection is to be closed
   * @return a list if the records
   */
  SList<? extends T> onFindAll(X x, boolean close);

  /** gets all the records matching the conditions specified in the columns
   * @param x database instance
   * @param column specifies where or order by conditions
   * @return a list if records
   */
  SList<? extends T> onFindAll(X x, Column... column);

  /** updates an item in the table specified by the condition in the column
   * see {@link Column#getOperand()} for sample conditional operations
   * @param t record to updateAsync
   * @param x database instance
   * @param column field specifying the updateAsync condition
   * @return true id record is updated
   * @throws ModelError if there is missing updating information or error in sql script
   */
  boolean onUpdate(T t, X x, Column column) throws ModelError;

  /** updates an item with an id with a value more that zero
   * see {@link Model#id}
   * @param t record to updateAsync
   * @param x database instance
   * @return true if record is updated otherwise false
   */
  boolean onUpdate(T t, X x);

  /** deleteAsync a record from the table specified by the condition in the column
   * see {@link Column#getOperand()} for sample conditional operations
   * @param x database instance
   * @param column field to infer where condition
   * @return true if record deleted otherwise false
   */
  boolean onDelete(X x, Column column);

  /** deletes an item with an id with a value more that zero
   * see {@link Model#id}
   * @param t record to updateAsync
   * @param x database instance
   * @return true if record is deleted otherwise false
   */
  boolean onDelete(T t, X x);

  /** truncates the table
   * @param x database instance
   * @return true if truncated otherwise false
   */
  boolean onDelete(X x);

  /** deletes a number of rows where the column matches each item in list
   * @param x database instance
   * @param column field to match each item in list
   * @param list matches to deleteAsync from
   * @param <C> type of match must not be derived
   * @return true if deleted otherwise false
   */
  <C> boolean onDelete(X x, Column<C> column, List<? extends C> list);

  /** saves an item in the table
   * if it has an id more than zero, it updates it instead
   * @param t item to saveAsync
   * @param x database instance
   * @return reference of affected row
   */
  long onSave(T t, X x);

  /** saveAsync multiple items to the table
   * @param list items to saveAsync
   * @param x database instance
   * @return true if all items are saved otherwise false
   */
  boolean onSave(SList<? extends T> list, X x);

  /**
   * @param x
   * @return
   * @throws ModelError
   */
  boolean onDrop(X x) throws ModelError;

  int onGetLastId(X x);

  /**
   * @return
   */
  String getName();

  /**
   * @param <T>
   */
  interface Extras<T extends Identifiable<Integer>> {
    /**
     * @return
     */
    @Nullable
    T first();

    /**
     * @return
     */
    @Nullable
    T last();

    /**
     * @return
     */
    SList<? extends T> all();

    /**
     * @param limit
     * @return
     */
    SList<? extends T> limit(int limit);

    /**
     * @param skip
     * @param limit
     * @return
     */
    SList<? extends T> paginate(int skip, int limit);

    /**
     * @param skip
     * @param limit
     * @return
     */
    SList<? extends T> paginateDescending(int skip, int limit);

    /**
     * @param column
     * @param a
     * @param b
     * @return
     */
    <N extends Number>SList<? extends T> between(Column<N> column, N a, N b);

    /**
     * @param column
     * @return
     */
    SList<? extends T> where(Column... column);

    /**
     * @param column
     * @param bounds
     * @return
     */
    <N extends Number>SList<? extends T> notIn(Column<N> column, N... bounds);

    /**
     * @param column
     * @return
     */
    SList<? extends T> like(Column... column);

    /**
     * @param column
     * @return
     */
    SList<? extends T> orderBy(Column column);

    /**
     * @param column
     * @return
     */
    SList<? extends T> groupBy(Column column);

    /**
     * @param column
     * @param column1
     * @return
     */
    SList<? extends T> groupAndOrderBy(Column column, Column column1);
  }
}
