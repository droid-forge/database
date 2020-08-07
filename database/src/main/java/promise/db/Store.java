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

import org.jetbrains.annotations.Nullable;

import promise.commons.model.List;
import promise.commons.tx.PromiseResult;

public interface Store<T, K, X extends Throwable> {

  void get(K k, PromiseResult<StoreExtra<T>, X> callBack);

  void delete(K k, T t, PromiseResult<Boolean, X> callBack);

  void update(K k, T t, PromiseResult<Boolean, X> callBack);

  void save(K k, T t, PromiseResult<Boolean, X> callBack);

  void clear(K k, PromiseResult<Boolean, X> callBack);

  void clear(PromiseResult<Boolean, X> callBack);

  interface StoreFilter<T> {
    <X> List<? extends T> filter(List<? extends T> list, X... x);
  }

  abstract class StoreExtra<T> {

    static <T, E extends Throwable> void getExtras(final List<? extends T> list,
                                                   StoreFilter<T> storeFilter,
                                                   PromiseResult<StoreExtra<T>, E> callBack) {
      callBack.response(
          new StoreExtra<T>() {

            @Nullable
            @Override
            public T first() {
              return list.first();
            }

            @Nullable
            @Override
            public T last() {
              return list.last();
            }

            @Override
            public List<? extends T> all() {
              return list;
            }

            @Override
            public List<? extends T> limit(int limit) {
              return list.take(limit);
            }

            @SafeVarargs
            @Override
            public final <X> List<? extends T> where(X... x) {
              return storeFilter.filter(list, x);
            }
          });
    }

    @Nullable
    abstract T first();

    abstract @Nullable
    T last();

    abstract List<? extends T> all();

    abstract List<? extends T> limit(int limit);

    abstract <X> List<? extends T> where(X... x);
  }

}
