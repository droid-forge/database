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

package promise.db.query.order;

import promise.commons.model.List;
import promise.db.Column;
import promise.db.query.projection.AliasedProjection;
import promise.db.query.projection.Projection;

public abstract class Order {
  protected Projection projection;

  public Order(Projection projection) {
    this.projection = projection;

    if (this.projection instanceof AliasedProjection)
      this.projection = ((AliasedProjection) this.projection).removeAlias();
  }

  public static Order orderByAscending(Column column) {
    return new OrderAscending(Projection.column(column));
  }

  public static Order orderByDescending(Column column) {
    return new OrderDescending(Projection.column(column));
  }

  public static Order orderByAscending(Projection projection) {
    return new OrderAscending(projection);
  }

  public static Order orderByDescending(Projection projection) {
    return new OrderDescending(projection);
  }

  public static Order orderByAscendingIgnoreCase(Column column) {
    return new OrderAscendingIgnoreCase(Projection.column(column));
  }

  public static Order orderByDescendingIgnoreCase(Column column) {
    return new OrderDescendingIgnoreCase(Projection.column(column));
  }

  public static Order orderByAscendingIgnoreCase(Projection projection) {
    return new OrderAscendingIgnoreCase(projection);
  }

  public static Order orderByDescendingIgnoreCase(Projection projection) {
    return new OrderDescendingIgnoreCase(projection);
  }

  public abstract String build();

  public abstract List<String> buildParameters();
}
