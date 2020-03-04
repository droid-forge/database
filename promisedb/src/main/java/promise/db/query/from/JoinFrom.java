/*
 * Copyright 2017, Peter Vincent
 * Licensed under the Apache License, Version 2.0, Android Promise.
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package promise.db.query.from;

import promise.commons.model.List;
import promise.db.Column;
import promise.db.query.criteria.Criteria;
import promise.db.query.projection.Projection;

public class JoinFrom extends From {
  private From left;
  private From right;
  private String joinType;
  private Criteria criteria;

  public JoinFrom(From left, From right, String joinType, Criteria criteria) {
    this.left = left;
    this.right = right;
    this.joinType = joinType;
    this.criteria = criteria;
  }

  public JoinFrom onOr(Column leftColumn, Column rightColumn) {
    return onOr(Criteria.equals(Projection.column(leftColumn), Projection.column(rightColumn)));
  }

  public JoinFrom onAnd(Column leftColumn, Column rightColumn) {
    return onAnd(Criteria.equals(Projection.column(leftColumn), Projection.column(rightColumn)));
  }

  public JoinFrom onAnd(Criteria criteria) {
    this.criteria = (this.criteria != null ? this.criteria.and(criteria) : criteria);
    return this;
  }

  public JoinFrom onOr(Criteria criteria) {
    this.criteria = (this.criteria != null ? this.criteria.or(criteria) : criteria);
    return this;
  }

  @Override
  public String build() {
    String leftSide = (left != null ? left.build() : "");
    String rightSide = (right != null ? right.build() : "");
    String joinCriteria = (criteria != null ? criteria.build() : "");

    return "(" + leftSide + " " + joinType + " " + rightSide + " ON " + joinCriteria + ")";
  }

  @Override
  public List<String> buildParameters() {
    List<Object> ret = new List<Object>();

    if (left != null) ret.addAll(left.buildParameters());

    if (right != null) ret.addAll(right.buildParameters());

    if (criteria != null) ret.addAll(criteria.buildParameters());

    return ret.map(
        String::valueOf);
  }
}
