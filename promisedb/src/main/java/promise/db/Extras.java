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

import promise.commons.model.List;


/**
 * Created on 6/27/18 by yoctopus.
 */
public interface Extras<T> {
    @Nullable
    T first();
    @Nullable T last();
    List<? extends T> all();
    List<? extends T> limit(int limit);
    <X> List<? extends T> where(X... x);
}
