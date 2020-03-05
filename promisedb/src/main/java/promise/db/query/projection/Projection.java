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

package promise.db.query.projection;

import promise.commons.model.List;
import promise.db.Column;
import promise.db.TableCrud;
import promise.db.query.QueryBuilder;

public abstract class Projection {
  // Simple column
  public static ColumnProjection column(Column column) {
    return new ColumnProjection(null, column);
  }

  public static ColumnProjection column(TableCrud table, Column column) {
    return new ColumnProjection(table, column);
  }

  // Constant
  public static ConstantProjection constant(Object constant) {
    return new ConstantProjection(constant);
  }

  // Aggregate functions
  public static AggregateProjection min(Column<Integer> column) {
    return min(column(column));
  }

  public static AggregateProjection max(Column<Integer> column) {
    return max(column(column));
  }

  public static AggregateProjection sum(Column<Integer> column) {
    return sum(column(column));
  }

  public static AggregateProjection avg(Column<Integer> column) {
    return avg(column(column));
  }

  public static AggregateProjection count(Column column) {
    return count(column(column));
  }

  public static AggregateProjection countRows() {
    return count(column(new Column("*", Column.Type.TEXT.NULLABLE())));
  }

  public static AggregateProjection min(Projection projection) {
    return new AggregateProjection(projection, AggregateProjection.Type.MIN);
  }

  public static AggregateProjection max(Projection projection) {
    return new AggregateProjection(projection, AggregateProjection.Type.MAX);
  }

  public static AggregateProjection sum(Projection projection) {
    return new AggregateProjection(projection, AggregateProjection.Type.SUM);
  }

  public static AggregateProjection avg(Projection projection) {
    return new AggregateProjection(projection, AggregateProjection.Type.AVG);
  }

  public static AggregateProjection count(Projection projection) {
    return new AggregateProjection(projection, AggregateProjection.Type.COUNT);
  }

  // SubQuery
  public static SubQueryProjection subQuery(QueryBuilder subQuery) {
    return new SubQueryProjection(subQuery);
  }

  public Projection as(String alias) {
    return new AliasedProjection(this, alias);
  }

  public Projection castAsDate() {
    return new CastDateProjection(this);
  }

  public Projection castAsDateTime() {
    return new CastDateTimeProjection(this);
  }

  public Projection castAsReal() {
    return new CastRealProjection(this);
  }

  public Projection castAsInt() {
    return new CastIntProjection(this);
  }

  public Projection castAsString() {
    return new CastStringProjection(this);
  }

  public abstract String build();

  public abstract List<String> buildParameters();
}
