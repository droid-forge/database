package promise.db.query.projection;

import promise.commons.model.List;
import promise.commons.model.function.MapFunction;
import promise.db.Utils;

public class AliasedProjection extends Projection {
  private Projection projection;
  private String alias;

  public AliasedProjection(Projection projection, String alias) {
    this.projection = projection;
    this.alias = alias;
  }

  @Override
  public Projection as(String alias) {
    this.alias = alias;
    return this;
  }

  @Override
  public Projection castAsDate() {
    if (projection != null) projection = projection.castAsDate();

    return this;
  }

  @Override
  public Projection castAsDateTime() {
    if (projection != null) projection = projection.castAsDateTime();

    return this;
  }

  @Override
  public Projection castAsInt() {
    if (projection != null) projection = projection.castAsInt();

    return this;
  }

  @Override
  public Projection castAsReal() {
    if (projection != null) projection = projection.castAsReal();

    return this;
  }

  @Override
  public Projection castAsString() {
    if (projection != null) projection = projection.castAsString();

    return this;
  }

  public Projection removeAlias() {
    Projection p = projection;

    while (p instanceof AliasedProjection) {
      p = ((AliasedProjection) p).projection;
    }

    return p;
  }

  @Override
  public String build() {
    String ret = (projection != null ? projection.build() : "");
    return ret + " AS " + alias;
  }

  @Override
  public List<String> buildParameters() {
    if (projection != null) return projection.buildParameters();
    else
      return Utils.EMPTY_LIST.map(
          String::valueOf);
  }
}
