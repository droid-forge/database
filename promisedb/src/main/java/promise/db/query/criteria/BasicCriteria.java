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

package promise.db.query.criteria;

import promise.commons.model.List;
import promise.db.query.projection.AliasedProjection;
import promise.db.query.projection.Projection;

public class BasicCriteria extends Criteria {
  private Projection projection;
  private String operator;
  private Object value;

  public BasicCriteria(Projection projection, String operator, Object value) {
    this.projection = projection;
    this.operator = operator;
    this.value = value;

    if (this.projection instanceof AliasedProjection)
      this.projection = ((AliasedProjection) this.projection).removeAlias();

    if (value == null) {
      if (Operators.IS_NULL.equals(operator)
          || Operators.EQUALS.equals(operator)
          || Operators.LIKE.equals(operator)) this.operator = Operators.IS_NULL;
      else this.operator = Operators.IS_NOT_NULL;
    }
  }

  @Override
  public String build() {
    StringBuilder sb = new StringBuilder();

    if (projection != null) {
      /*if(value instanceof LocalDateTime)
      	sb.append(projection.castAsDateTime().build());
      else if(value instanceof LocalDate)
      	sb.append(projection.castAsDate().build());
      else*/
      sb.append(projection.build());
    }

    sb.append(" ");
    sb.append(operator);
    sb.append(" ");

    if (value != null) {
      if (value instanceof AliasedProjection)
        sb.append(((AliasedProjection) value).removeAlias().build());
      else if (value instanceof Projection) sb.append(((Projection) value).build());
      else sb.append("?");
    }

    return sb.toString();
  }

  @Override
  public List<String> buildParameters() {
    List<String> ret = new List<>();

    if (projection != null) ret.addAll(projection.buildParameters());

    if (value != null) ret.add(String.valueOf(value));

    return ret;
  }

  public static class Operators {
    public static final String IS_NULL = "IS NULL";
    public static final String IS_NOT_NULL = "IS NOT NULL";
    public static final String EQUALS = "=";
    public static final String NOT_EQUALS = "<>";
    public static final String GREATER = ">";
    public static final String LESSER = "<";
    public static final String GREATER_OR_EQUALS = ">=";
    public static final String LESSER_OR_EQUALS = "<=";
    public static final String LIKE = "LIKE";
    public static final String NOT_LIKE = "NOT LIKE";
  }
}
