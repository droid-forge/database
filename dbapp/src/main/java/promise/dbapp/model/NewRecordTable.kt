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
import promise.commons.model.List
import promise.db.Column
import promise.db.FastDatabase
import promise.db.FastTable
import promise.db.Table

@Table(tableName = "new_record_table")
class NewRecordTable(appDatabase: FastDatabase) : FastTable<NewRecord>(appDatabase) {

  override val columns: List<out Column<*>>
    get() = List.fromArray(nameColumn, descColumn)

  override fun deserialize(e: Cursor): NewRecord = NewRecord().apply {
    toDoName = e.getString(nameColumn.index)
    todoDesc = e.getString(descColumn.index)
  }

  override fun serialize(t: NewRecord): ContentValues = ContentValues().apply {
    put(nameColumn.name, t.toDoName)
    put(descColumn.name, t.todoDesc)
  }

  companion object {
    val nameColumn: Column<String> = Column("tableName", Column.Type.TEXT.NULLABLE(), 1)
    val descColumn: Column<Float> = Column("desc", Column.Type.INTEGER.NOT_NULL(), 2)
  }
}