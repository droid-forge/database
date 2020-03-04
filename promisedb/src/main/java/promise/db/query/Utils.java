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

package promise.db.query;

import java.math.BigDecimal;

import promise.commons.model.List;
import promise.db.Column;
import promise.db.query.projection.Projection;

public class Utils {
  public static final List<Object> EMPTY_LIST = new List<Object>();

  public static String toString(Object value) {
    if (value == null)
      return null;

    if (value instanceof String)
      return (String) value;
    else if (value instanceof Float)
      return new BigDecimal((Float) value).stripTrailingZeros().toPlainString();
    else if (value instanceof Double)
      return new BigDecimal((Double) value).stripTrailingZeros().toPlainString();
    else
      return String.valueOf(value);
  }
	
	/*public static String dateToString(LocalDate date, DateTimeFormatter format) {
		if (date == null)
			return null;
		
		if(format == null)
			format = QueryBuildConfiguration.current().getDateFormat();

		try {
			return date.toString(format);
		} catch (Exception e) {
			return null;
		}
	}

	public static String dateToString(LocalDateTime date, DateTimeFormatter format) {
		if (date == null)
			return null;

		if(format == null)
			format = QueryBuildConfiguration.current().getDateTimeFormat();
		
		try {
			return date.toString(format);
		} catch (Exception e) {
			return null;
		}
	}*/

  public static boolean isNullOrEmpty(String string) {
    return (string == null || string.length() <= 0);
  }

  public static boolean isNullOrWhiteSpace(String string) {
    return (string == null || string.trim().length() <= 0);
  }

  public static Projection[] buildColumnProjections(Column... columns) {
    Projection[] projections = new Projection[columns.length];

    for (int i = 0; i < columns.length; i++) {
      projections[i] = Projection.column(columns[i]);
    }
    return projections;
  }
}
