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


import androidx.annotation.Nullable;

import promise.commons.model.List;
import promise.commons.model.Result;

/**
 * @param <T>
 * @param <K>
 * @param <X>
 */
public interface Store<T, K, X extends Throwable> {

  /**
   * @param k
   * @param callBack
   */
  void get(K k, Result<Extras<T>, X> callBack);

  /**
   * @param k
   * @param t
   * @param callBack
   */
  void delete(K k, T t, Result<Boolean, X> callBack);

  /**
   * @param k
   * @param t
   * @param callBack
   */
  void update(K k, T t, Result<Boolean, X> callBack);

  /**
   * @param k
   * @param t
   * @param callBack
   */
  void save(K k, T t, Result<Boolean, X> callBack);

  /**
   * @param k
   * @param callBack
   */
  void clear(K k, Result<Boolean, X> callBack);

  /**
   * @param callBack
   */
  void clear(Result<Boolean, X> callBack);

  /**
   * @param <T>
   * @param <E>
   */
  interface StoreExtra<T, E extends Throwable> {
    /**
     * @param list
     * @param callBack
     */
    default void getExtras(final List<? extends T> list, Result<Extras<T>, E> callBack) {
      callBack.response(
          new Extras<T>() {
            /**
             * @return
             */
            @Nullable
            @Override
            public T first() {
              return list.first();
            }

            /**
             * @return
             */
            @Nullable
            @Override
            public T last() {
              return list.last();
            }

            /**
             * @return
             */
            @Override
            public List<? extends T> all() {
              return list;
            }

            /**
             * @param limit
             * @return
             */
            @Override
            public List<? extends T> limit(int limit) {
              return list.take(limit);
            }

            /**
             * @param x
             * @param <X>
             * @return
             */
            @SafeVarargs
            @Override
            public final <X> List<? extends T> where(X... x) {
              return filter(list, x);
            }
          });
    }

    /**
     * @param list
     * @param y
     * @param <Y>
     * @return
     */
    <Y> List<? extends T> filter(List<? extends T> list, Y... y);
  }
}
