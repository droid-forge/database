package promise.db.query.criteria;


import promise.db.query.QueryBuilder;

public class NotExistsCriteria extends ExistsCriteria {
  public NotExistsCriteria(QueryBuilder subQuery) {
    super(subQuery);
  }

  @Override
  public String build() {
    return "NOT " + super.build();
  }
}
