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

package promise.db

import android.database.sqlite.SQLiteDatabase
import android.text.TextUtils
import androidx.sqlite.db.SupportSQLiteDatabase
import promise.commons.model.Identifiable
import promise.commons.model.List
import promise.model.IdentifiableList
import promise.utils.Visitor

internal class UpdateVisitor<T : Identifiable<Int>>(private val x: SupportSQLiteDatabase,
                                           private val instance: T,
                                           private val column: Column<*>? = null) : Visitor<TableCrud<T, in SupportSQLiteDatabase>, Boolean> {

  override fun visit(t: TableCrud<T, in SupportSQLiteDatabase>): Boolean {
    fun update(instance: T, x: SupportSQLiteDatabase, column: Column<*>): Boolean {
      val whereArg: String = if (column.operand != null && column.value() != null)
        column.name + column.operand + column.value()
      else throw TableError("Cant update the record, missing updating information")
      val values = t.serialize(instance)
      values.put(FastTable.updatedAt.name, System.currentTimeMillis())
      return x.update(t.name, SQLiteDatabase.CONFLICT_ROLLBACK, values, whereArg, null) > 0
    }
    return if (column == null) {
      try {
        update(this.instance, x, FastTable.id.with(this.instance.getId()))
      } catch (tableError: TableError) {
        false
      }
    } else update(this.instance, x, column)
  }
}

internal class DeleteVisitor<T : Identifiable<Int>>(private val x: SupportSQLiteDatabase,
                                           private val instance: T) : Visitor<TableCrud<T, in SupportSQLiteDatabase>, Boolean> {
  override fun visit(t: TableCrud<T, in SupportSQLiteDatabase>): Boolean {
    fun onDelete(x: SupportSQLiteDatabase, column: Column<*>): Boolean {
      val where = column.name + column.operand + column.value()
      return x.delete(t.name, where, null) >= 0
    }
    return onDelete(x, FastTable.id.with(instance.getId()))
  }
}

internal class DeleteErasureVisitor(private val x: SupportSQLiteDatabase,
                           private val column: Column<*>? = null) : Visitor<TableCrud<*, in SupportSQLiteDatabase>, Boolean> {
  override fun visit(t: TableCrud<*, in SupportSQLiteDatabase>): Boolean {
    fun onDelete(x: SupportSQLiteDatabase): Boolean =
        !TextUtils.isEmpty(t.name) && x.delete(t.name, null, null) >= 0

    fun onDelete(x: SupportSQLiteDatabase, column: Column<*>): Boolean {
      val where = column.name + column.operand + column.value()
      return x.delete(t.name, where, null) >= 0
    }
    return if (column != null) onDelete(x, column)
    else onDelete(x)
  }
}

internal class DeleteListErasureVisitor<C>(private val x: SupportSQLiteDatabase,
                                  private val column: Column<C>,
                                  private val list: List<out C>) : Visitor<TableCrud<*, in SupportSQLiteDatabase>, Boolean> {
  override fun visit(t: TableCrud<*, in SupportSQLiteDatabase>): Boolean {
    fun <C> onDelete(x: SupportSQLiteDatabase, column: Column<C>, list: List<out C>): Boolean {
      val deleted: Boolean
      var where = ""
      var i = 0
      val listSize = list.size
      while (i < listSize) {
        val c = list[i]
        where = if (i == listSize - 1) {
          column.name + " " + column.operand + " " + c
        } else column.name + " " + column.operand + " " + c + " OR "
        i++
      }
      deleted = x.delete(t.name, where, null) >= 0
      return deleted
    }
    return onDelete(x, column, list)
  }
}

internal class SaveVisitor<T : Identifiable<Int>>(
    private val updateVisitor: Visitor<TableCrud<T, in SupportSQLiteDatabase>, Boolean>,
    private val x: SupportSQLiteDatabase,
    private val instance: T
) : Visitor<TableCrud<T, in SupportSQLiteDatabase>, Long> {
  override fun visit(t: TableCrud<T, in SupportSQLiteDatabase>): Long {
    fun onSave(instance: T, x: SupportSQLiteDatabase): Long {
      if (instance.getId() != 0 && updateVisitor.visit(t)) return instance.getId().toLong()
      val values = t.serialize(instance)
      values.put(FastTable.createdAt.name, System.currentTimeMillis())
      values.put(FastTable.updatedAt.name, System.currentTimeMillis())
      return x.insert(t.name, SQLiteDatabase.CONFLICT_ROLLBACK, values)
    }
    return onSave(instance, x)
  }
}

internal class SaveListVisitor<T : Identifiable<Int>>(
    private val x: SupportSQLiteDatabase,
    private val list: IdentifiableList<out T>
) : Visitor<TableCrud<T, in SupportSQLiteDatabase>, Boolean> {
  override fun visit(t: TableCrud<T, in SupportSQLiteDatabase>): Boolean {
    var saved = true
    var i = 0
    val listSize = list.size
    while (i < listSize) {
      val instance = list[i]
      val visitor = SaveVisitor<T>(UpdateVisitor(x, instance, null), x, instance)
      saved = saved && visitor.visit(t) > 0
      i++
    }
    /*if (close) database.close();*/return saved
  }
}