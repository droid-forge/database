package promise.db.query.criteria;

import promise.commons.model.List;

public class OrCriteria extends Criteria {
  private Criteria left;
  private Criteria right;

  public OrCriteria(Criteria left, Criteria right) {
    this.left = left;
    this.right = right;
  }

  @Override
  public String build() {
    String ret = " OR ";

    if (left != null) ret = left.build() + ret;

    if (right != null) ret = ret + right.build();

    return "(" + ret.trim() + ")";
  }

  @Override
  public List<String> buildParameters() {
    List<Object> ret = new List<>();

    if (left != null) ret.addAll(left.buildParameters());

    if (right != null) ret.addAll(right.buildParameters());

    return ret.map(
        String::valueOf);
  }
}
