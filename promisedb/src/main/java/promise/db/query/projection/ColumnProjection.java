package promise.db.query.projection;

import promise.commons.model.List;
import promise.db.Column;
import promise.db.Table;
import promise.db.Utils;

public class ColumnProjection extends Projection {
  private Table table;
  private Column column;

  public ColumnProjection(Table table, Column column) {
    this.table = table;
    this.column = column;
  }

  @Override
  public String build() {
    String ret = "";

    if (!Utils.isNullOrWhiteSpace(table != null ? table.getName(): "")) ret = table.getName();

    if (!Utils.isNullOrWhiteSpace(column.getName())) ret = ret + column.getName();

    return ret;
  }

  @Override
  public List<String> buildParameters() {
    return Utils.EMPTY_LIST.map(
        String::valueOf);
  }
}
