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

package promise.database.compiler.utils;

public class Category<K, T> {
  private List<? extends T> list;
  private K name;

  Category(K name) {
    this.name = name;
  }

  public List<? extends T> list() {
    return list;
  }

  public Category<K, T> list(List<? extends T> list) {
    this.list = list;
    return this;
  }

  public K name() {
    return name;
  }

  public Category<K, T> name(K name) {
    this.name = name;
    return this;
  }
}

