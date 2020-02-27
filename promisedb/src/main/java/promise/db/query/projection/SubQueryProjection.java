package promise.db.query.projection;

import promise.commons.model.List;
import promise.db.Utils;
import promise.db.query.QueryBuilder;

public class SubQueryProjection extends Projection {
  private QueryBuilder subQuery;

  public SubQueryProjection(QueryBuilder subQuery) {
    this.subQuery = subQuery;
  }

  @Override
  public String build() {
    if (subQuery != null) return "(" + subQuery.build() + ")";
    else return "";
  }

  @Override
  public List<String> buildParameters() {
    if (subQuery != null) return List.fromArray(subQuery.buildParameters());
    else
      return Utils.EMPTY_LIST.map(
          String::valueOf);
  }
}
