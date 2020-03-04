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


import io.reactivex.Maybe;
import io.reactivex.Single;
import promise.commons.model.Identifiable;
import promise.commons.model.List;
import promise.model.SList;
import promise.model.SModel;

interface ReactiveCrud<X> {

  <T extends SModel> ReactiveTable.Extras<T> readAsync(Table<T, ? super X> table) throws ModelError;

  <T extends Identifiable<Integer>> Maybe<SList<? extends T>> readAllAsync(Table<T, ? super X> table);

  <T extends Identifiable<Integer>> Maybe<SList<? extends T>> readAllAsync(Table<T, ? super X> table, Column... column);

  <T extends Identifiable<Integer>> Maybe<Boolean> updateAsync(T t, Table<T, ? super X> table, Column column);

  <T extends Identifiable<Integer>> Maybe<Boolean> updateAsync(T t, Table<T, ? super X> table);

  <T extends Identifiable<Integer>> Maybe<Boolean> deleteAsync(Table<T, ? super X> table, Column column);

  <T extends Identifiable<Integer>> Maybe<Boolean> deleteAsync(Table<T, ? super X> table, T t);

  Maybe<Boolean> deleteAsync(Table<?, ? super X> table);

  <C> Maybe<Boolean> deleteAsync(Table<?, ? super X> table, Column<C> column, List<? extends C> list);

  <T extends Identifiable<Integer>> Single<Long> saveAsync(T t, Table<T, ? super X> table);

  <T extends Identifiable<Integer>> Single<Boolean> saveAsync(SList<? extends T> list, Table<T, ? super X> table);

  Maybe<Boolean> deleteAllAsync();

  <T extends Identifiable<Integer>> Maybe<Integer> getLastIdAsync(Table<T, ? super X> table);
}
