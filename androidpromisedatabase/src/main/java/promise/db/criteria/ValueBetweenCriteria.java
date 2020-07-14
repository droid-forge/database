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

package promise.db.criteria;

import promise.commons.model.List;
import promise.db.projection.AliasedProjection;
import promise.db.projection.Projection;

public class ValueBetweenCriteria extends Criteria {
  private Object value;
  private Projection projectionStart;
  private Projection projectionEnd;

  public ValueBetweenCriteria(Object value, Projection projectionStart, Projection projectionEnd) {
    this.value = value;
    this.projectionStart = projectionStart;
    this.projectionEnd = projectionEnd;

    if (this.projectionStart instanceof AliasedProjection)
      this.projectionStart = ((AliasedProjection) this.projectionStart).removeAlias();

    if (this.projectionEnd instanceof AliasedProjection)
      this.projectionEnd = ((AliasedProjection) this.projectionEnd).removeAlias();
  }

  @Override
  public String build() {
    StringBuilder sb = new StringBuilder();

    sb.append((value != null ? "?" : "NULL"));
    sb.append(" BETWEEN ");
    sb.append((projectionStart != null ? projectionStart.build() : "NULL"));
    sb.append(" AND ");
    sb.append((projectionEnd != null ? projectionEnd.build() : "NULL"));

    return sb.toString();
  }

  @Override
  public List<String> buildParameters() {
    List<Object> ret = new List<Object>();

    if (value != null) ret.add(value);

    if (projectionStart != null) ret.addAll(projectionStart.buildParameters());

    if (projectionEnd != null) ret.addAll(projectionEnd.buildParameters());

    return ret.map(
        String::valueOf);
  }
}
