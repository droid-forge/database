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
 *
 *
 */

package promise.db.query.criteria;

import promise.commons.model.List;
import promise.db.query.projection.AliasedProjection;
import promise.db.query.projection.Projection;

public class InCriteria extends Criteria {
  private Projection projection;
  private List<Object> valuesList;
  private Object[] valuesArray;

  public InCriteria(Projection projection, List<Object> values) {
    this.projection = projection;
    this.valuesList = values;
    this.valuesArray = null;

    if (this.projection instanceof AliasedProjection)
      this.projection = ((AliasedProjection) this.projection).removeAlias();
  }

  public InCriteria(Projection projection, Object[] values) {
    this.projection = projection;
    this.valuesArray = values;
    this.valuesList = null;

    if (this.projection instanceof AliasedProjection)
      this.projection = ((AliasedProjection) this.projection).removeAlias();
  }

  @Override
  public String build() {
    StringBuilder sb = new StringBuilder();

    if (projection != null) sb.append(projection.build());

    sb.append(" IN (");

    if (valuesList != null) {
      if (valuesList.size() <= 0) return "1=0";

      for (int i = 0; i < valuesList.size(); i++) {
        if (valuesList.get(i) != null) sb.append("?, ");
        else sb.append("NULL, ");
      }
    } else {
      if (valuesArray.length <= 0) return "1=0";

      for (Object o : valuesArray) {
        if (o != null) sb.append("?, ");
        else sb.append("NULL, ");
      }
    }

    sb.setLength(sb.length() - 2); // removes the ", " from the last entry
    sb.append(")");

    return sb.toString();
  }

  @Override
  public List<String> buildParameters() {
    List<String> ret = new List<>();

    if (projection != null) ret.addAll(projection.buildParameters());

    if (valuesList != null)
      for (int i = 0; i < valuesList.size(); i++) {
        if (valuesList.get(i) != null) ret.add(String.valueOf(valuesList.get(i)));
      }
    else
      for (Object aValuesArray : valuesArray) {
        if (aValuesArray != null) ret.add(String.valueOf(aValuesArray));
      }

    return ret;
  }
}
