package promise.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQuery;

public class DatabaseCursorFactory implements SQLiteDatabase.CursorFactory {

  private Listener listener;

  DatabaseCursorFactory(Listener listener) {
    this.listener = listener;
  }

  @Override
  public Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery, String editTable, SQLiteQuery query) {
    listener.onLog(query.toString());
    return new SQLiteCursor(db, masterQuery, editTable, query);
  }

  public interface Listener {
    void onLog(String query);
  }
}
