package promise.db.query.criteria;

import promise.commons.model.List;
import promise.db.query.projection.AliasedProjection;
import promise.db.query.projection.Projection;

public class ValueBetweenCriteria extends Criteria {
  private Object value;
  private Projection projectionStart;
  private Projection projectionEnd;

  public ValueBetweenCriteria(Object value, Projection projectionStart, Projection projectionEnd) {
    this.value = value;
    this.projectionStart = projectionStart;
    this.projectionEnd = projectionEnd;

    if (this.projectionStart instanceof AliasedProjection)
      this.projectionStart = ((AliasedProjection) this.projectionStart).removeAlias();

    if (this.projectionEnd instanceof AliasedProjection)
      this.projectionEnd = ((AliasedProjection) this.projectionEnd).removeAlias();
  }

  @Override
  public String build() {
    StringBuilder sb = new StringBuilder();

    sb.append((value != null ? "?" : "NULL"));
    sb.append(" BETWEEN ");
    sb.append((projectionStart != null ? projectionStart.build() : "NULL"));
    sb.append(" AND ");
    sb.append((projectionEnd != null ? projectionEnd.build() : "NULL"));

    return sb.toString();
  }

  @Override
  public List<String> buildParameters() {
    List<Object> ret = new List<Object>();

    if (value != null) ret.add(value);

    if (projectionStart != null) ret.addAll(projectionStart.buildParameters());

    if (projectionEnd != null) ret.addAll(projectionEnd.buildParameters());

    return ret.map(
        String::valueOf);
  }
}
