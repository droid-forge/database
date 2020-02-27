package promise.db.query.projection;

import promise.commons.model.List;
import promise.db.Utils;

public class CastIntProjection extends Projection {
  private Projection projection;

  public CastIntProjection(Projection projection) {
    this.projection = projection;
  }

  @Override
  public String build() {
    String ret = (projection != null ? projection.build() : "");
    return "CAST(" + ret + " AS INTEGER)";
  }

  @Override
  public List<String> buildParameters() {
    if (projection != null) return projection.buildParameters();
    else
      return Utils.EMPTY_LIST.map(
          String::valueOf);
  }
}
