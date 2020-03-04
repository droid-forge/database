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


import androidx.annotation.Nullable;

import io.reactivex.Maybe;
import promise.commons.model.Identifiable;
import promise.model.IdentifiableList;

/**
 * models concise readAsync queries in an reactive way
 * see {@link Table}
 */
public interface ReactiveTable {

  /**
   * modeling concise readAsync queries
   * see {@link Table.Extras}
   *
   * @param <T> the type of data to be stored in the table
   */
  interface Extras<T extends Identifiable<Integer>> {
    /**
     * gets the first record in the table
     * see {@link Table.Extras#first()}
     *
     * @return the first records
     */
    @Nullable
    Maybe<T> first();

    /**
     * gets the last record in the table
     * see {@link Table.Extras#last()}
     *
     * @return the last record in the table
     */
    @Nullable
    Maybe<T> last();

    /**
     * gets all the records in the table
     * see {@link Table.Extras#all()}
     *
     * @return the list if records
     */
    Maybe<IdentifiableList<? extends T>> all();

    /**
     * gets the top records marked by the limit
     *
     * @param limit limit to take from the table
     *              see {@link Table.Extras#limit(int)}
     * @return a list of records
     */
    Maybe<IdentifiableList<? extends T>> limit(int limit);

    /**
     * paginates the data between the offset and limit
     * see {@link Table.Extras#paginate(int, int)}
     *
     * @param skip  offset to take
     * @param limit limit to fetch
     * @return a list of records
     */
    Maybe<IdentifiableList<? extends T>> paginate(int skip, int limit);

    /**
     * @param skip
     * @param limit
     * @return
     */
    Maybe<IdentifiableList<? extends T>> paginateDescending(int skip, int limit);

    /**
     * gets data that matches between the values passed in the specified column
     * see {@link Table.Extras#between(Column, N, N)}
     *
     * @param column field to match between
     * @param a      lower bound of between
     * @param b      upper bound of between
     * @return a list of matched records
     */
    <N extends Number> Maybe<IdentifiableList<? extends T>> between(Column<N> column, N a, N b);

    /**
     * gets all the data matched in where in the columns
     * see {@link Table.Extras#where(Column[])}
     *
     * @param column columns to match where clause
     * @return a list of records matching the criteria
     */
    Maybe<IdentifiableList<? extends T>> where(Column... column);

    /**
     * gets all the data not within the specified bounds in the specified column
     * see {@link Table.Extras#notIn(Column, N...)}
     *
     * @param column field to checkout bounds
     * @param bounds specified not in bounds
     * @param <N>    type of bound
     * @return list of records
     */
    <N extends Number> Maybe<IdentifiableList<? extends T>> notIn(Column<N> column, N... bounds);

    /**
     * fetches all the data where the column contains the value
     * see {@link Table.Extras#limit(int)}
     *
     * @param column field to build like clause
     * @return list of records
     */
    Maybe<IdentifiableList<? extends T>> like(Column<String>... column);

    /**
     * fetches all the data in the order specified in the column
     * see {@link Table.Extras#orderBy(Column)}
     *
     * @param column field to build order by clause
     * @return a list of records ordered
     */
    Maybe<IdentifiableList<? extends T>> orderBy(Column column);

    /**
     * fetches all the data in the group by condition specified in the column
     * see {@link Table.Extras#groupBy(Column)}
     *
     * @param column field to build the group by clause
     * @return a list of records grouped by the condition in the column
     */
    Maybe<IdentifiableList<? extends T>> groupBy(Column column);

    /**
     * groups and orders the result set by the two provided sets of columns
     * see {@link Table.Extras#groupAndOrderBy(Column, Column)}
     *
     * @param column  field to group by
     * @param column1 field to order by
     * @return a list of data grouped and ordered in the given criteria
     */
    Maybe<IdentifiableList<? extends T>> groupAndOrderBy(Column column, Column column1);
  }
}
