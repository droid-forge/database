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

package promise.model;

import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;

import promise.commons.model.Identifiable;
import promise.commons.model.List;
import promise.db.ActiveRecord;

public class IdentifiableList<T extends Identifiable<Integer>> extends List<T> {
  public IdentifiableList(Collection<? extends T> c) {
    super(c);
  }

  public IdentifiableList() {
    super();
  }

  public IdentifiableList(int initialCapacity) {
    super(initialCapacity);
  }

  @Nullable
  public T getWithID(int key) {
    if (key < 1) throw new IllegalArgumentException("key less than 1 is not allowed");
    for (T t : this) if (t.getId() == key) return t;
    return null;
  }

  public int getID(T t) {
    if (getWithID(t.getId()) != null) return t.getId();
    else return -1;
  }

  public int getIndex(int key) {
    int index = 0;
    for (T t : this) {
      if (t.getId() == key) return index;
      index++;
    }
    return -1;
  }

  public int getIndex(T t) {
    return getIndex(t.getId());
  }

  public boolean removeWithID(int id) {
    int index = getIndex(id);
    if (index != -1) {
      super.remove(index);
      return true;
    }
    return false;
  }

  public boolean remove(T t) {
    if (t instanceof ActiveRecord<?>) {
      try {
        ((ActiveRecord) t).delete();
      } catch (Exception ignored) {
      }
    }
    super.remove(getIndex(t));
    return true;
  }

  public void delete(T t) {
    if (t instanceof ActiveRecord<?>) {
      try {
        ((ActiveRecord) t).delete();
      } catch (Exception ignored) {
      }
    }
    removeWithID(t.getId());
  }

  public void update(T t) {
    if (t instanceof ActiveRecord<?>) {
      try {
        ((ActiveRecord) t).update();
      } catch (Exception ignored) {
      }
    }
    super.set(getIndex(t), t);
  }

  public IdentifiableList<T> reverseWithID() {
    sortWithID();
    reverse();
    return this;
  }

  public IdentifiableList<T> sortWithID() {
    Collections.sort(this,
        (o1, o2) -> o1.getId() < o2.getId() ? 1 :
            o1.getId() > o2.getId() ? -1 : 0);
    return this;
  }

}
