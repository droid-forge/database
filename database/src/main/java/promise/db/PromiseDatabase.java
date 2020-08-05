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

import promise.commons.model.Identifiable;

/**
 * Base class for classes annotated with DatabaseEntity
 */
public abstract class PromiseDatabase {

  private FastDatabase fastDatabase;

  public PromiseDatabase(FastDatabase fastDatabase) {
    this.fastDatabase = fastDatabase;
  }

  /**
   * @return an instance of FastDatabase
   */
  public FastDatabase getDatabaseInstance() {
    if (fastDatabase == null) throw new IllegalStateException("Database not initialized or created yet");
    return fastDatabase;
  }

  /**
   * returns the table associated with the entity class
   *
   * @param entityClass class of the entity persisted
   * @param <T>         entity
   * @return FastTable of the entity
   * @throws IllegalArgumentException if entity is not registered with the database
   */
  public abstract <T extends Identifiable<Integer>> FastTable<T> tableOf(Class<? extends T> entityClass) throws IllegalArgumentException;
}


