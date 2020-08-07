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

package promise.db;

import android.database.Cursor;

import java.util.Comparator;

public final class Column<T> {
  public static final String ASCENDING = "ASC", DESCENDING = "DESC";
  static Comparator<Column> ascending =
      (o1, o2) -> o1.getIndex() > o2.getIndex() ? 1 :
          o1.getIndex() < o2.getIndex() ? -1 : 0;
  private String order;
  private String name;
  private String description;
  private String operand = Op.EQUALS().s;
  private T value;
  private int index;

  public Column(String name, Type type) {
    this(name, type, 0);
  }

  public Column(String name, Type type, int index) {
    this(name, type.s, index);
  }

  private Column(String name, String description, int index) {
    this.name = name;
    this.description = description;
    this.index = index;
  }

  public Column<T> descending() {
    this.order = DESCENDING;
    return this;
  }

  public Column<T> ascending() {
    this.order = ASCENDING;
    return this;
  }

  public String order() {
    return order;
  }

  public int getIndex() {
    return index;
  }

  protected T value() {
    return value;
  }

  public int getIndex(Cursor cursor) {
    return cursor.getColumnIndex(name);
  }

  public Column<T> with(T t) {
    Column<T> column = this;
    column.set(t);
    return column;
  }

  public Column<T> withOut(T t) {
    Column<T> column = this;
    column.set(Op.NOT_EQUALS(), t);
    return column;
  }

  public void set(T value) {
    this.value = value;
  }

  public void set(Op op, T value) {
    this.value = value;
    this.operand = op.s;
  }

  public String getOperand() {
    return operand;
  }

  public String getName() {
    return name;
  }

  protected String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    return "`" + name + "`" + " " + description;
  }

  private enum NULL {
    YES, NO
  }

  public static class Op {
    private String s;

    Op(String s) {
      this.s = s;
    }

    public static Op EQUALS() {
      return new Op(" = ");
    }

    public static Op NOT_EQUALS() {
      return new Op(" != ");
    }

    public static Op LESS() {
      return new Op(" < ");
    }

    public static Op MORE() {
      return new Op(" > ");
    }

    public static Op LESS_EQUALS() {
      return new Op(" <= ");
    }

    public static Op MORE_EQUALS() {
      return new Op(" >= ");
    }
  }

  public static class Type {
    private String s;

    Type(String s) {
      this.s = s;
    }

    private static Type newType(String prefix, NULL n, String s) {
      String g = n != NULL.NO ? "" : " NOT NULL";
      return new Type(prefix + g + " " + s);
    }

    private static Type newType(String prefix, String s) {
      return newType(prefix, NULL.YES, s);
    }

    private static Type newType(String prefix, NULL n) {
      return newType(prefix, n, "");
    }

    private static Type newType(String prefix) {
      return newType(prefix, NULL.NO, "UNIQUE");
    }

    public static class INTEGER {
      private static final String prefix =
          "INTEGER";

      public static Type NOT_NULL() {
        return newType(prefix, NULL.NO);
      }

      public static Type UNIQUE() {
        return newType(prefix);
      }

      public static Type NULLABLE() {
        return newType(prefix, NULL.YES);
      }

      public static Type PRIMARY_KEY() {
        return newType(prefix, "PRIMARY KEY");
      }

      protected static Type PRIMARY_KEY_AUTOINCREMENT() {
        return newType(prefix, "PRIMARY KEY AUTOINCREMENT");
      }

      public static Type DEFAULT(int d) {
        return newType(prefix, "DEFAULT " + d);
      }
    }

    public static class TEXT {
      private static final String prefix =
          "TEXT";

      public static Type NULLABLE() {
        return newType(prefix, NULL.YES);
      }

      public static Type UNIQUE() {
        return newType(prefix);
      }

      public static Type NOT_NULL() {
        return newType(prefix, NULL.NO);
      }
    }

    public static class VARCHAR {
      private static final String prefix =
          "VARCHAR";

      public static Type NULLABLE(int len) {
        return newType(prefix.concat(" (" + len + ")"), NULL.YES);
      }

      public static Type UNIQUE(int len) {
        return newType(prefix.concat(" (" + len + ")"));
      }

      public static Type NOT_NULL(int len) {
        return newType(prefix.concat(" (" + len + ")"), NULL.NO);
      }
    }

    public static class REAL {
      private static final String prefix =
          "REAL";

      public static Type NULLABLE() {
        return newType(prefix, NULL.YES);
      }

      public static Type UNIQUE() {
        return newType(prefix);
      }

      public static Type NOT_NULL() {
        return newType(prefix, NULL.NO);
      }
    }
  }
}
