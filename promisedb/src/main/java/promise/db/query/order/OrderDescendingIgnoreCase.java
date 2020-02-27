package promise.db.query.order;

import promise.commons.model.List;
import promise.db.Utils;
import promise.db.query.projection.Projection;

public class OrderDescendingIgnoreCase extends Order {

  public OrderDescendingIgnoreCase(Projection projection) {
    super(projection);
  }

  @Override
  public String build() {
    String ret = " COLLATE NOCASE DESC";

    if (projection != null) ret = projection.build() + ret;

    return ret;
  }

  @Override
  public List<String> buildParameters() {
    if (projection != null) return projection.buildParameters();
    else
      return Utils.EMPTY_LIST.map(
          String::valueOf);
  }
}
