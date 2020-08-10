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

import android.database.DatabaseErrorHandler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;

import promise.commons.AndroidPromise;

public abstract class FastDatabaseOpenHelper extends SupportSQLiteOpenHelper.Callback {

  private SupportSQLiteOpenHelper helper;

  private DatabaseErrorHandler errorHandler;

  public FastDatabaseOpenHelper(@Nullable String name,
                                int version,
                                @Nullable DatabaseErrorHandler errorHandler) {
    super(version);
    SupportSQLiteOpenHelper.Factory factory = new FrameworkSQLiteOpenHelperFactory();
    this.errorHandler = errorHandler;
    SupportSQLiteOpenHelper.Configuration configuration = SupportSQLiteOpenHelper.Configuration
        .builder(AndroidPromise.instance().context())
        .name(name)
        .callback(this)
        .build();

    this.helper = factory.create(configuration);
   // super(AndroidPromise.instance().context(), name, version, errorHandler);
  }

  public void onCorruption(SupportSQLiteDatabase db) {
    //if (this.errorHandler != null) this.errorHandler.onCorruption(db);
  }


  @Override
  public final void onConfigure(SupportSQLiteDatabase db) {
    super.onConfigure(db);
  }

  @Override
  public final void onDowngrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
    super.onDowngrade(db, oldVersion, newVersion);
  }

  @Override
  public final void onOpen(@NonNull SupportSQLiteDatabase db) {
    super.onOpen(db);
    db.execSQL("PRAGMA foreign_keys = ON");
  }

  public final SupportSQLiteDatabase getReadableDatabase() {
    return helper.getReadableDatabase();
  }

  public final SupportSQLiteDatabase getWritableDatabase() {
    return helper.getWritableDatabase();
  }

  public String getDatabaseName() {
    return helper.getDatabaseName();
  }
}
