//
///*
// * Copyright 2017, Peter Vincent
// * Licensed under the Apache License, Version 2.0, Android Promise.
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// * http://www.apache.org/licenses/LICENSE-2.0
// * Unless required by applicable law or agreed to in writing,
// * software distributed under the License is distributed on an "AS IS" BASIS,
// *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package promise.dbapp.test
//
//import android.database.sqlite.SQLiteDatabase
//import promise.db.Database
//import promise.db.FastDatabase
//import promise.db.Migration
//import promise.dbapp.PersonFastTable
//import promise.dbapp.model.CatFastTable
//import promise.dbapp.model.DogFastTable
//import promise.dbapp.model.GeneratedDatabase
//
//@Database(
//    tables = [
//      PersonFastTable::class
//    ],
//    version = 2
//)
//class GeneratedDatabaseImpl : GeneratedDatabase() {
//
//  companion object {
//
//
//
//
//    private fun getMigration(): Migration {
//      return object: Migration {
//        override fun onMigrate(database: FastDatabase, sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
//          if (oldVersion == 2 && newVersion == 3) {
//            // we added new record table when database version is 1, and therefore need to add it version 2
//            database.add(sqLiteDatabase, database.obtain(PersonFastTable::class.java))
//          }
//        }
//      }
//    }
//
//    var instance: FastDatabase? = null
//
//    private val LOCK = Any()
//
//    @JvmStatic
//    fun createDatabase(name: String): GeneratedDatabase {
//      if (instance != null) throw IllegalStateException("Database already created")
//      instance = synchronized(LOCK) {
//        instance ?: FastDatabase.createDatabase(GeneratedDatabaseImpl::class.java,
//            name, getMigration()).also {
//          instance = it
//        }
//      }
//      return GeneratedDatabaseImpl()
//    }
//
//    @JvmStatic
//    fun createInMemoryDatabase(): GeneratedDatabase {
//      if (instance != null) throw IllegalStateException("Database already created")
//      instance = synchronized(LOCK) {
//        instance ?: FastDatabase.createInMemoryDatabase(GeneratedDatabaseImpl::class.java).also {
//          instance = it
//        }
//      }
//      return GeneratedDatabaseImpl()
//    }
//
//    @JvmStatic
//    fun createReactiveInMemoryDatabase(): GeneratedDatabase {
//      if (instance != null) throw IllegalStateException("Database already created")
//      instance = FastDatabase.createInMemoryReactiveDatabase(GeneratedDatabaseImpl::class.java)
//      return GeneratedDatabaseImpl()
//    }
//
//    @JvmStatic
//    fun createReactiveDatabase(name: String): GeneratedDatabase {
//      if (instance != null) throw IllegalStateException("Database already created")
//      instance = FastDatabase.createReactiveDatabase(GeneratedDatabaseImpl::class.java,
//          name, getMigration())
//      return GeneratedDatabaseImpl()
//    }
//
//    val personFastTable: PersonFastTable by lazy {
//      getDatabaseInstance().obtain<PersonFastTable>(PersonFastTable::class.java)
//    }
//
//    @JvmStatic
//    fun getDatabaseInstance(): FastDatabase {
//      if (instance == null) throw IllegalStateException("Database not initialized or created yet")
//      return instance!!
//    }
//  }
//
//  override fun getPersonFastTable(): PersonFastTable {
//    return personFastTable
//  }
//
//  override fun getCatFastTable(): CatFastTable {
//    TODO("Not yet implemented")
//  }
//
//  override fun getDogFastTable(): DogFastTable {
//    TODO("Not yet implemented")
//  }
//
//}