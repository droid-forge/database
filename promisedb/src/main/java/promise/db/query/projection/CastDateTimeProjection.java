package promise.db.query.projection;

import promise.commons.model.List;
import promise.db.Utils;

public class CastDateTimeProjection extends Projection {
  private Projection projection;

  public CastDateTimeProjection(Projection projection) {
    this.projection = projection;
  }

  @Override
  public String build() {
    String ret = (projection != null ? projection.build() : "");
    return "DATETIME(" + ret + ")";
  }

  @Override
  public List<String> buildParameters() {
    if (projection != null) return projection.buildParameters();
    else
      return Utils.EMPTY_LIST.map(
          String::valueOf);
  }
}
