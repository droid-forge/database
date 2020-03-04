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

package promise.dbapp.model

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import io.reactivex.Maybe
import promise.commons.model.List
import promise.db.Column
import promise.db.FastTable
import promise.model.IdentifiableList

class ComplexRecordTable(database: AppDatabase) : FastTable<ComplexRecord>(database) {
  /**
   * @return
   */
  override fun getName(): String = "name_of_complex_model_table"

  override fun onUpgrade(database: SQLiteDatabase?, v1: Int, v2: Int) {
    if (v1 == 1 && v2 ==2) {
      // add when migrating from version 1 to 2
      addColumns(database, flagVariableColumn)
    }
    super.onUpgrade(database, v1, v2)
  }

  override fun getColumns(): List<Column<*>> =
      List.fromArray(intVariableColumn,
          floatVariableColumn,
          doubleVariableColumn,
          stringVariableColumn,
          // add this here to have it on new installs
          flagVariableColumn
          )

  override fun deserialize(e: Cursor): ComplexRecord = ComplexRecord().apply {
    intVariable = e.getInt(intVariableColumn.index)
    floatVariable = e.getFloat(floatVariableColumn.index)
    doubleVariable = e.getDouble(doubleVariableColumn.index)
    stringVariable = e.getString(stringVariableColumn.index)
    // remember to update it to your model
    flagString = e.getString(flagVariableColumn.getIndex(e))
  }

  override fun serialize(t: ComplexRecord): ContentValues = ContentValues().apply {
    put(intVariableColumn.name, t.intVariable)
    put(floatVariableColumn.name, t.floatVariable)
    put(doubleVariableColumn.name, t.doubleVariable)
    put(stringVariableColumn.name, t.stringVariable)
    // remember to store the new variable
    put(flagVariableColumn.name, t.flagString)
  }

  companion object {
    val intVariableColumn: Column<Int> = Column("int", Column.Type.INTEGER.NOT_NULL(), 1)
    val floatVariableColumn: Column<Float> = Column("float", Column.Type.INTEGER.NOT_NULL(), 2)
    val doubleVariableColumn: Column<Double> = Column("double", Column.Type.INTEGER.NOT_NULL(), 3)
    val stringVariableColumn: Column<String> = Column("string", Column.Type.INTEGER.NOT_NULL(), 4)

    val flagVariableColumn: Column<String> = Column("flag", Column.Type.INTEGER.NOT_NULL(), 5)

  }
}