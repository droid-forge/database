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

  /**
   * gets all the columns for this model from the child class for creation purposes
   * see [.onCreate]
   *
   * @return list of columns
   */
  override fun getColumns(): List<Column<*>> =
      List.fromArray(intVariableColumn,
          floatVariableColumn,
          doubleVariableColumn,
          stringVariableColumn)

  override fun deserialize(e: Cursor): ComplexRecord = ComplexRecord().apply {
    intVariable = e.getInt(intVariableColumn.index)
    floatVariable = e.getFloat(floatVariableColumn.index)
    doubleVariable = e.getDouble(doubleVariableColumn.index)
    stringVariable = e.getString(stringVariableColumn.index)
  }

  override fun serialize(t: ComplexRecord): ContentValues = ContentValues().apply {
    put(intVariableColumn.name, t.intVariable)
    put(floatVariableColumn.name, t.floatVariable)
    put(doubleVariableColumn.name, t.doubleVariable)
    put(stringVariableColumn.name, t.stringVariable)
  }

  override fun findAllAsync(): Maybe<IdentifiableList<out ComplexRecord>> {
    return super.findAllAsync()
  }


  companion object {
    val intVariableColumn: Column<Int> = Column("int", Column.Type.INTEGER.NOT_NULL(), 1)
    val floatVariableColumn: Column<Float> = Column("float", Column.Type.INTEGER.NOT_NULL(), 2)
    val doubleVariableColumn: Column<Double> = Column("double", Column.Type.INTEGER.NOT_NULL(), 3)
    val stringVariableColumn: Column<String> = Column("string", Column.Type.INTEGER.NOT_NULL(), 4)

  }
}