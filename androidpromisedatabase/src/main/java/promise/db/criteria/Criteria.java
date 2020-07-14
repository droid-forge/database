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

package promise.db.criteria;

import promise.commons.model.List;
import promise.db.Column;
import promise.db.QueryBuilder;
import promise.db.projection.Projection;

public abstract class Criteria {
  // Null
  public static Criteria isNull(Column column) {
    return new BasicCriteria(Projection.column(column), BasicCriteria.Operators.IS_NULL, null);
  }

  public static Criteria notIsNull(Column column) {
    return new BasicCriteria(Projection.column(column), BasicCriteria.Operators.IS_NOT_NULL, null);
  }

  public static Criteria isNull(Projection projection) {
    return new BasicCriteria(projection, BasicCriteria.Operators.IS_NULL, null);
  }

  public static Criteria notIsNull(Projection projection) {
    return new BasicCriteria(projection, BasicCriteria.Operators.IS_NOT_NULL, null);
  }

  // Basic criterias
  public static Criteria equals(Column column, Object value) {
    return new BasicCriteria(Projection.column(column), BasicCriteria.Operators.EQUALS, value);
  }

  public static Criteria notEquals(Column column, Object value) {
    return new BasicCriteria(Projection.column(column), BasicCriteria.Operators.NOT_EQUALS, value);
  }

  public static Criteria greaterThan(Column column, Object value) {
    return new BasicCriteria(Projection.column(column), BasicCriteria.Operators.GREATER, value);
  }

  public static Criteria lesserThan(Column column, Object value) {
    return new BasicCriteria(Projection.column(column), BasicCriteria.Operators.LESSER, value);
  }

  public static Criteria greaterThanOrEqual(Column column, Object value) {
    return new BasicCriteria(
        Projection.column(column), BasicCriteria.Operators.GREATER_OR_EQUALS, value);
  }

  public static Criteria lesserThanOrEqual(Column column, Object value) {
    return new BasicCriteria(
        Projection.column(column), BasicCriteria.Operators.LESSER_OR_EQUALS, value);
  }

  public static Criteria equals(Projection column, Object value) {
    return new BasicCriteria(column, BasicCriteria.Operators.EQUALS, value);
  }

  public static Criteria notEquals(Projection column, Object value) {
    return new BasicCriteria(column, BasicCriteria.Operators.NOT_EQUALS, value);
  }

  public static Criteria greaterThan(Projection column, Object value) {
    return new BasicCriteria(column, BasicCriteria.Operators.GREATER, value);
  }

  public static Criteria lesserThan(Projection column, Object value) {
    return new BasicCriteria(column, BasicCriteria.Operators.LESSER, value);
  }

  public static Criteria greaterThanOrEqual(Projection column, Object value) {
    return new BasicCriteria(column, BasicCriteria.Operators.GREATER_OR_EQUALS, value);
  }

  public static Criteria lesserThanOrEqual(Projection column, Object value) {
    return new BasicCriteria(column, BasicCriteria.Operators.LESSER_OR_EQUALS, value);
  }

  // String-only criterias
  public static Criteria startsWith(Column column, String value) {
    return new BasicCriteria(Projection.column(column), BasicCriteria.Operators.LIKE, value + "%");
  }

  public static Criteria notStartsWith(Column column, String value) {
    return new BasicCriteria(
        Projection.column(column), BasicCriteria.Operators.NOT_LIKE, value + "%");
  }

  public static Criteria endsWith(Column column, String value) {
    return new BasicCriteria(Projection.column(column), BasicCriteria.Operators.LIKE, "%" + value);
  }

  public static Criteria notEndsWith(Column column, String value) {
    return new BasicCriteria(
        Projection.column(column), BasicCriteria.Operators.NOT_LIKE, "%" + value);
  }

  public static Criteria contains(Column column, String value) {
    return new BasicCriteria(
        Projection.column(column), BasicCriteria.Operators.LIKE, "%" + value + "%");
  }

  public static Criteria notContains(Column column, String value) {
    return new BasicCriteria(
        Projection.column(column), BasicCriteria.Operators.NOT_LIKE, "%" + value + "%");
  }

  public static Criteria startsWith(Projection column, String value) {
    return new BasicCriteria(column, BasicCriteria.Operators.LIKE, value + "%");
  }

  public static Criteria notStartsWith(Projection column, String value) {
    return new BasicCriteria(column, BasicCriteria.Operators.NOT_LIKE, value + "%");
  }

  public static Criteria endsWith(Projection column, String value) {
    return new BasicCriteria(column, BasicCriteria.Operators.LIKE, "%" + value);
  }

  public static Criteria notEndsWith(Projection column, String value) {
    return new BasicCriteria(column, BasicCriteria.Operators.NOT_LIKE, "%" + value);
  }

  public static Criteria contains(Projection column, String value) {
    return new BasicCriteria(column, BasicCriteria.Operators.LIKE, "%" + value + "%");
  }

  public static Criteria notContains(Projection column, String value) {
    return new BasicCriteria(column, BasicCriteria.Operators.NOT_LIKE, "%" + value + "%");
  }

  // Between
  public static Criteria between(Column column, Object valueMin, Object valueMax) {
    return new BetweenCriteria(Projection.column(column), valueMin, valueMax);
  }

  public static Criteria valueBetween(Object value, Column columnMin, Column columnMax) {
    return new ValueBetweenCriteria(
        value, Projection.column(columnMin), Projection.column(columnMax));
  }

  public static Criteria between(Projection column, Object valueMin, Object valueMax) {
    return new BetweenCriteria(column, valueMin, valueMax);
  }

  public static Criteria valueBetween(Object value, Projection columnMin, Projection columnMax) {
    return new ValueBetweenCriteria(value, columnMin, columnMax);
  }

  // Exists
  public static Criteria exists(QueryBuilder subQuery) {
    return new ExistsCriteria(subQuery);
  }

  public static Criteria notExists(QueryBuilder subQuery) {
    return new NotExistsCriteria(subQuery);
  }

  // In
  public static Criteria in(Column column, Object[] values) {
    return new InCriteria(Projection.column(column), values);
  }

  public static Criteria notIn(Column column, Object[] values) {
    return new NotInCriteria(Projection.column(column), values);
  }

  public static Criteria in(Column column, List<Object> values) {
    return new InCriteria(Projection.column(column), values);
  }

  public static Criteria notIn(Column column, List<Object> values) {
    return new NotInCriteria(Projection.column(column), values);
  }

  public static Criteria in(Projection column, Object[] values) {
    return new InCriteria(column, values);
  }

  public static Criteria notIn(Projection column, Object[] values) {
    return new NotInCriteria(column, values);
  }

  public static Criteria in(Projection column, List<Object> values) {
    return new InCriteria(column, values);
  }

  public static Criteria notIn(Projection column, List<Object> values) {
    return new NotInCriteria(column, values);
  }

  public abstract String build();

  public abstract List<String> buildParameters();

  public AndCriteria and(Criteria criteria) {
    return new AndCriteria(this, criteria);
  }

  public OrCriteria or(Criteria criteria) {
    return new OrCriteria(this, criteria);
  }
}
