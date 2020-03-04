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

package promise.db.query;

import java.util.Collections;

import promise.commons.model.List;
import promise.db.Column;
import promise.db.Table;
import promise.db.query.criteria.Criteria;
import promise.db.query.from.From;
import promise.db.query.order.Order;
import promise.db.query.projection.AliasedProjection;
import promise.db.query.projection.Projection;

public class QueryBuilder {
  private List<Projection> projections;
  private From from;
  private Criteria criteria;
  private List<Projection> groupBy;
  private List<Order> orderBy;
  private int skip;
  private int take;
  private boolean distinct;

  private List<QueryBuilder> unionQueries;
  private boolean unionAll;

  public QueryBuilder() {
    projections = new List<>();
    from = null;
    criteria = null;
    groupBy = new List<>();
    orderBy = new List<>();
    skip = -1;
    take = -1;
    distinct = false;
    unionQueries = new List<>();
    unionAll = false;
  }

  public QueryBuilder select(Column... columns) {
    if (columns == null) return this;
    return select(Utils.buildColumnProjections(columns));
  }

  public QueryBuilder select(Projection... projections) {
    if (projections == null) return this;
    Collections.addAll(this.projections, projections);
    return this;
  }

  public QueryBuilder from(Table table) {
    return from(From.table(table));
  }

  public QueryBuilder from(QueryBuilder subQuery) {
    return from(From.subQuery(subQuery));
  }

  public QueryBuilder from(From from) {
    if (from != null) this.from = from;
    return this;
  }

  public QueryBuilder whereAnd(Criteria criteria) {
    if (criteria != null) {
      if (this.criteria == null) this.criteria = criteria;
      else this.criteria = this.criteria.and(criteria);
    }

    return this;
  }

  public QueryBuilder whereOr(Criteria criteria) {
    if (criteria != null) {
      if (this.criteria == null) this.criteria = criteria;
      else this.criteria = this.criteria.or(criteria);
    }

    return this;
  }

  public QueryBuilder groupBy(Column... columns) {
    if (columns == null) return this;

    return groupBy(Utils.buildColumnProjections(columns));
  }

  public QueryBuilder groupBy(Projection... projections) {
    if (projections == null) return this;
    Collections.addAll(this.groupBy, projections);

    return this;
  }

  public QueryBuilder clearGroupBy() {
    this.groupBy.clear();
    return this;
  }

  public QueryBuilder orderByAscending(Column... columns) {
    if (columns == null) return this;

    return orderByAscending(Utils.buildColumnProjections(columns));
  }

  public QueryBuilder orderByAscending(Projection... projections) {
    if (projections == null) return this;
    for (Projection projection : projections) {
      this.orderBy.add(Order.orderByAscending(projection));
    }
    return this;
  }

  public QueryBuilder orderByDescending(Column... columns) {
    if (columns == null) return this;

    return orderByDescending(Utils.buildColumnProjections(columns));
  }

  public QueryBuilder orderByDescending(Projection... projections) {
    if (projections == null) return this;

    for (Projection projection : projections) {
      this.orderBy.add(Order.orderByDescending(projection));
    }

    return this;
  }

  public QueryBuilder orderByAscendingIgnoreCase(Column... columns) {
    if (columns == null) return this;

    return orderByAscendingIgnoreCase(Utils.buildColumnProjections(columns));
  }

  public QueryBuilder orderByAscendingIgnoreCase(Projection... projections) {
    if (projections == null) return this;

    for (Projection projection : projections) {
      this.orderBy.add(Order.orderByAscendingIgnoreCase(projection));
    }

    return this;
  }

  public QueryBuilder orderByDescendingIgnoreCase(Column... columns) {
    if (columns == null) return this;

    return orderByDescendingIgnoreCase(Utils.buildColumnProjections(columns));
  }

  public QueryBuilder orderByDescendingIgnoreCase(Projection... projections) {
    if (projections == null) return this;

    for (Projection projection : projections) {
      this.orderBy.add(Order.orderByDescendingIgnoreCase(projection));
    }

    return this;
  }

  public QueryBuilder clearOrderBy() {
    this.orderBy.clear();
    return this;
  }

  public QueryBuilder skip(int skip) {
    this.skip = skip;
    return this;
  }

  public QueryBuilder skipNone() {
    this.skip = -1;
    return this;
  }

  public QueryBuilder take(int take) {
    this.take = take;
    return this;
  }

  public QueryBuilder takeAll() {
    this.take = -1;
    return this;
  }

  public QueryBuilder distinct() {
    this.distinct = true;
    return this;
  }

  public QueryBuilder notDistinct() {
    this.distinct = false;
    return this;
  }

  public QueryBuilder union(QueryBuilder query) {
    query.unionAll = false;
    unionQueries.add(query);

    return this;
  }

  public QueryBuilder unionAll(QueryBuilder query) {
    query.unionAll = true;
    unionQueries.add(query);

    return this;
  }

  public String build() {
    StringBuilder sb = new StringBuilder();

    buildSelectClause(sb);

    buildFromClause(sb);

    buildWhereClause(sb);

    buildGroupByClause(sb);

    buildUnionClause(sb);

    buildOrderByClause(sb);

    buildTakeClause(sb);

    buildSkipClause(sb);

    return sb.toString();
  }

  private void buildSkipClause(StringBuilder sb) {
    if (skip > 0) {
      sb.append(" OFFSET ");
      sb.append(skip);
    }
  }

  private void buildTakeClause(StringBuilder sb) {
    if (take > 0) {
      sb.append(" LIMIT ");
      sb.append(take);
    }
  }

  private void buildOrderByClause(StringBuilder sb) {
    if (orderBy.size() > 0) {
      sb.append(" ORDER BY ");
      for (Order o : orderBy) {
        sb.append(o.build());
        sb.append(", ");
      }

      sb.setLength(sb.length() - 2); // removes the ", " from the last entry
    }
  }

  private void buildUnionClause(StringBuilder sb) {
    List<Order> oldOrderBy;
    int oldSkip;
    int oldTake;

    for (QueryBuilder union : unionQueries) {
      sb.append(union.unionAll ? " UNION ALL " : " UNION ");

      oldOrderBy = union.orderBy;
      oldSkip = union.skip;
      oldTake = union.take;

      union.orderBy = new List<>();
      union.skip = -1;
      union.take = -1;

      sb.append(union.build());

      union.orderBy = oldOrderBy;
      union.skip = oldSkip;
      union.take = oldTake;
    }
  }

  private void buildGroupByClause(StringBuilder sb) {
    if (groupBy.size() > 0) {
      sb.append(" GROUP BY ");

      for (Projection p : groupBy) {
        if (p instanceof AliasedProjection) p = ((AliasedProjection) p).removeAlias();

        sb.append(p.build());
        sb.append(", ");
      }

      sb.setLength(sb.length() - 2); // removes the ", " from the last entry
    }
  }

  private void buildWhereClause(StringBuilder sb) {
    if (criteria != null) {
      sb.append("WHERE ");
      sb.append(criteria.build());
    }
  }

  private void buildFromClause(StringBuilder sb) {
    if (from != null) {
      sb.append("FROM ");
      sb.append(from.build());
      sb.append(" ");
    }
  }

  private void buildSelectClause(StringBuilder sb) {
    sb.append("SELECT ");
    if (distinct) sb.append("DISTINCT ");
    if (projections.size() <= 0) sb.append("*");
    else {
      for (Projection p : projections) {
        sb.append(p.build());
        sb.append(", ");
      }

      sb.setLength(sb.length() - 2); // removes the ", " from the last entry
    }
    sb.append(" ");
  }

  public String[] buildParameters() {
    List<String> ret = new List<>();
    List<Order> oldOrderBy;
    int oldSkip;
    int oldTake;

    buildSelectClauseParameters(ret);

    if (from != null) ret.addAll(from.buildParameters());

    if (criteria != null) ret.addAll(criteria.buildParameters());

    for (Projection p : groupBy) {
      ret.addAll(p.buildParameters());
    }

    for (QueryBuilder union : unionQueries) {
      oldOrderBy = union.orderBy;
      oldSkip = union.skip;
      oldTake = union.take;

      union.orderBy = new List<>();
      union.skip = -1;
      union.take = -1;

      ret.addAll(List.fromArray(union.buildParameters()));

      union.orderBy = oldOrderBy;
      union.skip = oldSkip;
      union.take = oldTake;
    }

    for (Order o : orderBy) {
      ret.addAll(o.buildParameters());
    }
    preProcessDateValues(ret);
    String[] result = new String[ret.size()];

    for (int i = 0; i < result.length; i++) {
      result[i] = ret.get(i);
    }

    return result;
  }

  private void buildSelectClauseParameters(List<String> ret) {
    for (Projection p : projections) {
      ret.addAll(p.buildParameters());
    }
  }

  public String toDebugSqlString() {
    String[] parameters = buildParameters();
    String saida = build();

    if (parameters != null) {
      for (Object p : parameters) {
        if (p == null) saida = saida.replaceFirst("\\?", "NULL");
        else saida = saida.replaceFirst("\\?", escapeSQLString(Utils.toString(p)));
      }
    }

    return saida;
  }

  private void preProcessDateValues(List<String> values) {
    Object value;
    int index = 0;

    while (index < values.size()) {
      value = values.get(index);

      /*if(value instanceof LocalDateTime) {
      	values.remove(index);
      	values.add(index, Utils.dateToString(((LocalDateTime)value), dateTimeFormat));
      } else if(value instanceof LocalDate) {
      	values.remove(index);
      	values.add(index, Utils.dateToString(((LocalDate)value), dateFormat));
      }*/

      index++;
    }
  }

  private String escapeSQLString(String sqlString) {
    // Copied from Android source: DatabaseUtils.appendEscapedSQLString
    StringBuilder sb = new StringBuilder();
    sb.append('\'');

    if (sqlString.indexOf('\'') != -1) {
      int length = sqlString.length();
      for (int i = 0; i < length; i++) {
        char c = sqlString.charAt(i);
        if (c == '\'') {
          sb.append('\'');
        }
        sb.append(c);
      }
    } else sb.append(sqlString);

    sb.append('\'');
    return sb.toString();
  }
}
