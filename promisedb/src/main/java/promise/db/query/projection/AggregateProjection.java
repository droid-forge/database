package promise.db.query.projection;

import promise.commons.model.List;
import promise.db.Utils;

public class AggregateProjection extends Projection {
  private Projection projection;
  private int type;
  public AggregateProjection(Projection projection, int type) {
    this.projection = projection;
    this.type = type;
  }

  @Override
  public String build() {
    String ret = (projection != null ? projection.build() : "");

    if (type == Type.MIN) return "MIN(" + ret + ")";
    else if (type == Type.MAX) return "MAX(" + ret + ")";
    else if (type == Type.SUM) return "SUM(" + ret + ")";
    else if (type == Type.AVG) return "AVG(" + ret + ")";
    else if (type == Type.COUNT) return "COUNT(" + ret + ")";
    else return ret;
  }

  @Override
  public List<String> buildParameters() {
    if (projection != null) return projection.buildParameters();
    else return Utils.EMPTY_LIST.map(String::valueOf);
  }

  public static class Type {
    public static final int MIN = 1;
    public static final int MAX = 2;
    public static final int SUM = 3;
    public static final int AVG = 4;
    public static final int COUNT = 5;
  }
}
