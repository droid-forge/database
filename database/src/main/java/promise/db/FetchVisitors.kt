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

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteException
import androidx.sqlite.db.SupportSQLiteDatabase
import promise.commons.data.log.LogUtil
import promise.commons.model.Identifiable
import promise.commons.util.DoubleConverter
import promise.db.criteria.Criteria
import promise.db.projection.Projection
import promise.model.ITimeStamped
import promise.model.IdentifiableList
import promise.utils.Visitor

internal class FetchLastIdVisitor(private val x: SupportSQLiteDatabase) : Visitor<TableCrud<*, in SupportSQLiteDatabase>, Int> {
  override fun visit(t: TableCrud<*, in SupportSQLiteDatabase>): Int {
    val builder: QueryBuilder = t.queryBuilder().select(Projection.count(FastTable.id).`as`("num"))
    @SuppressLint("Recycle") val cursor = x.query(builder.build(), builder.buildParameters())
    return cursor.getInt(FastTable.id.index)
  }
}

internal class FetchAllVisitor<T : Identifiable<Int>>(private val x: SupportSQLiteDatabase,
                                             private val columns: Array<out Column<*>>? = null) : Visitor<TableCrud<T, in SupportSQLiteDatabase>, IdentifiableList<out T>> {

  override fun visit(t: TableCrud<T, in SupportSQLiteDatabase>): IdentifiableList<out T> {
    val builder: QueryBuilder = t.queryBuilder()
    columns?.forEach {
      if (it.value() != null) builder.whereAnd(Criteria.equals(it, it.value()))
      if (it.order() != null) {
        if (it.order() == Column.DESCENDING) {
          builder.orderByDescending(it)
        } else builder.orderByAscending(it)
      }
    }
    val cursor: Cursor
    return try {
      cursor = x.query(builder.build(), builder.buildParameters())
      t.collection(cursor)
    } catch (e: SQLiteException) {
      IdentifiableList()
    }
  }
}

@Suppress("KDocUnresolvedReference")
internal class FetchExtrasVisitor<T : Identifiable<Int>>(private val x: SupportSQLiteDatabase) : Visitor<TableCrud<T, in SupportSQLiteDatabase>, TableCrud.Extras<T>> {

  override fun visit(t: TableCrud<T, in SupportSQLiteDatabase>): TableCrud.Extras<T> =
      object : QueryExtras<T>(x, t) {
        override fun serialize(instance: T): ContentValues = t.serialize(instance)
        override fun deserialize(e: Cursor): T = t.deserialize(e)
      }

  /**
   * This class contains special queries for reading from the table
   * see [TimeAware] for encapsulating id and timestamps
   * see [DoubleConverter] for serializing and de-serializing
   *
   * @param <Q> The type of the items in the table
  </Q> */
  @Suppress("UNCHECKED_CAST")
  private abstract inner class QueryExtras<Q : Identifiable<Int>> constructor(
      /**
       * the database instance to readAsync fromm
       */
      private val database: SupportSQLiteDatabase,
      private val fastTable: TableCrud<T, in SupportSQLiteDatabase>) : TableCrud.Extras<Q>, DoubleConverter<Q, Cursor, ContentValues> {

    fun database(): SupportSQLiteDatabase = database

    /**
     * get a record pre populated with id and timestamps from each readAsync
     *
     * @param cursor serialized version of Q
     * @return Q the de-serialized output of reading the cursor
     */
    fun getWithId(cursor: Cursor): Q {
      var t = deserialize(cursor)
      t.setId(cursor.getInt(FastTable.id.getIndex(cursor)))
      if (t is ITimeStamped) {
        val iTimeAware = t as ITimeStamped
        iTimeAware.setCreatedAt(cursor.getInt(FastTable.createdAt.getIndex(cursor)).toLong())
        iTimeAware.setUpdatedAt(cursor.getInt(FastTable.updatedAt.getIndex(cursor)).toLong())
        t = iTimeAware as Q
      }
      if (t is ActiveRecord<*>) (t as ActiveRecord<T>).table = fastTable as FastTable<T>
      return t
    }

    /**
     * get the first record in the table
     *
     * @return the first records or null if theirs none in the table
     */
    override fun first(): Q? {
      val cursor: Cursor
      return try {
        val builder: QueryBuilder = fastTable.queryBuilder().take(1)
        cursor = database.query(builder.build(), builder.buildParameters())
        cursor.moveToFirst()
        getWithId(cursor)
      } catch (e: SQLiteException) {
        LogUtil.e(fastTable.TAG, e)
        null
      }
    }

    /**
     * get the last record in the table
     *
     * @return an item or null if theirs none stored in the table
     */
    override fun last(): Q? {
      val cursor: Cursor
      return try {
        val builder: QueryBuilder = fastTable.queryBuilder().orderByDescending(FastTable.id).take(1)
        cursor = database.query(builder.build(), builder.buildParameters())
        cursor.moveToFirst()
        getWithId(cursor)
      } catch (e: SQLiteException) {
        LogUtil.e(fastTable.TAG, e)
        null
      }
    }

    /**
     * get all the items in the table
     *
     * @return the items or an empty list if theirs none
     */
    override fun all(): IdentifiableList<out Q> {
      val cursor: Cursor
      return try {
        val builder: QueryBuilder = fastTable.queryBuilder().takeAll()
        cursor = database.query(builder.build(), builder.buildParameters())
        val ts = IdentifiableList<Q>()
        while (cursor.moveToNext() && !cursor.isClosed) ts.add(getWithId(cursor))
        ts
      } catch (e: SQLiteException) {
        IdentifiableList()
      }
    }

    /**
     * readAsync the top items in the table
     *
     * @param limit the number of records to readAsync
     * @return a list of the items
     */
    override fun limit(limit: Int): IdentifiableList<out Q> {
      val cursor: Cursor
      return try {
        val builder: QueryBuilder = fastTable.queryBuilder().take(limit)
        cursor = database.query(builder.build(), builder.buildParameters())
        val ts = IdentifiableList<Q>()
        while (cursor.moveToNext() && !cursor.isClosed) ts.add(getWithId(cursor))
        ts
      } catch (e: SQLiteException) {
        LogUtil.e(fastTable.TAG, e)
        IdentifiableList()
      }
    }

    /**
     * reads the records between the skip and limit in the table
     *
     * @param skip  of set from the top to not readAsync
     * @param limit items to load after skip
     * @return a list of records
     */
    override fun paginate(skip: Int, limit: Int): IdentifiableList<out Q> {
      val cursor: Cursor
      return try {
        val builder: QueryBuilder = fastTable.queryBuilder().take(limit).skip(skip)
        cursor = database.query(builder.build(), builder.buildParameters())
        val ts = IdentifiableList<Q>()
        while (cursor.moveToNext() && !cursor.isClosed) ts.add(getWithId(cursor))
        ts
      } catch (e: SQLiteException) {
        LogUtil.e(fastTable.TAG, e)
        IdentifiableList()
      }
    }

    override fun paginateDescending(skip: Int, limit: Int): IdentifiableList<out Q> {
      val cursor: Cursor
      return try {
        val builder: QueryBuilder = fastTable.queryBuilder().orderByDescending(FastTable.id).take(limit).skip(skip)
        cursor = database.query(builder.build(), builder.buildParameters())
        val ts = IdentifiableList<Q>()
        while (cursor.moveToNext() && !cursor.isClosed) ts.add(getWithId(cursor))
        ts
      } catch (e: SQLiteException) {
        LogUtil.e(fastTable.TAG, e)
        IdentifiableList()
      }
    }

    /**
     * gets all items that match in between the int left and right
     *
     * @param column column to match between
     * @param a      lower between bound
     * @param b      upper between bound
     * @return a list of items
     */
    override fun between(column: Column<Number>, a: Number, b: Number): IdentifiableList<out Q> {
      val cursor: Cursor
      return try {
        val builder: QueryBuilder = fastTable.queryBuilder().takeAll().whereAnd(Criteria.between(column, a, b))
        cursor = database.query(builder.build(), builder.buildParameters())
        val ts = IdentifiableList<Q>()
        while (cursor.moveToNext() && !cursor.isClosed) ts.add(getWithId(cursor))
        ts
      } catch (e: SQLiteException) {
        LogUtil.e(fastTable.TAG, e)
        IdentifiableList()
      }
    }

    /**
     * gets all items matching the multiple columns
     *
     * @param column fields to match their values
     * @return a list of items
     */
    override fun where(vararg column: Column<*>): IdentifiableList<out Q> {
      val cursor: Cursor
      return try {
        val builder: QueryBuilder = fastTable.queryBuilder().takeAll()
        for (column1 in column) if (column1.value() != null) builder.whereAnd(Criteria.equals(column1, column1.value()))
        cursor = database.query(builder.build(), builder.buildParameters())
        val ts = IdentifiableList<Q>()
        while (cursor.moveToNext() && !cursor.isClosed) ts.add(getWithId(cursor))
        ts
      } catch (e: SQLiteException) {
        LogUtil.e(fastTable.TAG, e)
        IdentifiableList()
      }
    }

    /**
     * gets all the items matching not in any of the columns
     *
     * @param column field to match
     * @param bounds not in bounds
     * @return a list of items
     */
    @SafeVarargs
    override fun notIn(column: Column<Number>, vararg bounds: Number): IdentifiableList<out Q> {
      val cursor: Cursor
      val items = arrayOfNulls<Any>(bounds.size)
      System.arraycopy(bounds, 0, items, 0, bounds.size)
      val builder: QueryBuilder = fastTable.queryBuilder().takeAll()
          .whereAnd(Criteria.notIn(column, items))
      return try {
        cursor = database.query(builder.build(), builder.buildParameters())
        val ts = IdentifiableList<Q>()
        while (cursor.moveToNext() && !cursor.isClosed) {
          val t = getWithId(cursor)
          ts.add(t)
        }
        cursor.close()
        /*database.close();*/ts
      } catch (e: SQLiteException) {
        LogUtil.e(fastTable.TAG, e)
        IdentifiableList()
      }
    }

    /**
     * get all the rows where the column is like the columns values
     *
     * @param column the fields to compute like from
     * @return a list of columns
     */
    override fun like(vararg column: Column<*>): IdentifiableList<out Q> {
      val cursor: Cursor
      val builder: QueryBuilder = fastTable.queryBuilder().takeAll()
      for (column1 in column) builder.whereAnd(Criteria.contains(column1, column1.value().toString()))
      return try {
        cursor = database.query(builder.build(), builder.buildParameters())
        val ts = IdentifiableList<Q>()
        while (cursor.moveToNext() && !cursor.isClosed) {
          val t = getWithId(cursor)
          ts.add(t)
        }
        cursor.close()
        /*database.close();*/ts
      } catch (e: SQLiteException) {
        LogUtil.e(fastTable.TAG, e)
        IdentifiableList()
      }
    }

    /**
     * get all the rows in the oder specified by the column
     *
     * @param column field to order by
     * @return a list of ordered items
     */
    override fun orderBy(column: Column<*>): IdentifiableList<out Q> {
      val cursor: Cursor
      val builder: QueryBuilder = fastTable.queryBuilder().takeAll()
      if (column.order() == Column.DESCENDING) {
        builder.orderByDescending(column)
      } else builder.orderByAscending(column)
      return try {
        cursor = database.query(builder.build(), builder.buildParameters())
        val ts = IdentifiableList<Q>()
        while (cursor.moveToNext() && !cursor.isClosed) {
          val t = getWithId(cursor)
          ts.add(t)
        }
        cursor.close()
        /*database.close();*/ts
      } catch (e: SQLiteException) {
        LogUtil.e(fastTable.TAG, e)
        IdentifiableList()
      }
    }

    /**
     * gets all the items grouped by the column
     *
     * @param column field to group by
     * @return a list of grouped items
     */
    override fun groupBy(column: Column<*>): IdentifiableList<out Q> {
      val cursor: Cursor
      val builder: QueryBuilder = fastTable.queryBuilder().takeAll().groupBy(column)
      return try {
        cursor = database.query(builder.build(), builder.buildParameters())
        val ts = IdentifiableList<Q>()
        while (cursor.moveToNext() && !cursor.isClosed) {
          val t = getWithId(cursor)
          ts.add(t)
        }
        cursor.close()
        /*database.close();*/ts
      } catch (e: SQLiteException) {
        LogUtil.e(fastTable.TAG, e)
        IdentifiableList()
      }
    }

    /**
     * gets all the items grouped and ordered by the two columns
     *
     * @param column  group by field
     * @param column1 order by fields
     * @return a list of items
     */
    override fun groupAndOrderBy(column: Column<*>, column1: Column<*>): IdentifiableList<out Q> {
      val cursor: Cursor
      val builder: QueryBuilder = fastTable.queryBuilder().takeAll().groupBy(column)
      if (column1.order() == Column.DESCENDING) {
        builder.orderByDescending(column1)
      } else builder.orderByAscending(column1)
      return try {
        cursor = database.query(builder.build(), builder.buildParameters())
        val ts = IdentifiableList<Q>()
        while (cursor.moveToNext() && !cursor.isClosed) {
          val t = getWithId(cursor)
          ts.add(t)
        }
        cursor.close()
        /*database.close();*/ts
      } catch (e: SQLiteException) {
        LogUtil.e(fastTable.TAG, e)
        IdentifiableList()
      }
    }
  }
}