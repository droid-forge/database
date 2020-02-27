package promise.db.query.from;

import promise.commons.model.List;
import promise.db.Column;
import promise.db.Table;
import promise.db.query.QueryBuilder;
import promise.db.query.criteria.Criteria;
import promise.db.query.projection.Projection;

public abstract class From {
  public static TableFrom table(Table table) {
    return new TableFrom(table);
  }

  public static SubQueryFrom subQuery(QueryBuilder subQuery) {
    return new SubQueryFrom(subQuery);
  }

  public PartialJoin innerJoin(Table table) {
    return innerJoin(From.table(table));
  }

  public PartialJoin innerJoin(QueryBuilder subQuery) {
    return innerJoin(From.subQuery(subQuery));
  }

  public PartialJoin innerJoin(From table) {
    return new PartialJoin(this, table, "INNER JOIN");
  }

  public PartialJoin leftJoin(Table table) {
    return leftJoin(From.table(table));
  }

  public PartialJoin leftJoin(QueryBuilder subQuery) {
    return leftJoin(From.subQuery(subQuery));
  }

  public PartialJoin leftJoin(From table) {
    return new PartialJoin(this, table, "LEFT JOIN");
  }

  public abstract String build();

  public abstract List<String> buildParameters();

  public static class PartialJoin {
    private String joinType;
    private From left;
    private From right;

    protected PartialJoin(From left, From right, String joinType) {
      this.joinType = joinType;
      this.left = left;
      this.right = right;
    }

    public JoinFrom on(Column leftColumn, Column rightColumn) {
      return on(Criteria.equals(Projection.column(leftColumn), Projection.column(rightColumn)));
    }

    public JoinFrom on(Criteria criteria) {
      return new JoinFrom(left, right, joinType, criteria);
    }
  }
}
