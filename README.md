# Android Promise Database [![](https://jitpack.io/v/android-promise/database.svg)](https://jitpack.io/#android-promise/database)
 Manage SqLite databases in android with ease
## Setup
##### build.gradle
```

allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}

android {
    ...
    compileOptions {
            sourceCompatibility JavaVersion.VERSION_1_8
            targetCompatibility JavaVersion.VERSION_1_8
        }
}

dependencies {
     ...
     implementation 'com.github.android-promise:database:TAG'
     implementation 'com.github.android-promise:commons:1.1-alpha03'
}
```

### Initialization
Initialize Promise in your main application file, entry point

##### App.java
```java
public class App extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
    // 10 is the number of threads allowed to run in the background
    AndroidPromise.init(this, 10, BuildConfig.DEBUG);
  }

  @Override
  public void onTerminate() {
    super.onTerminate();
    AndroidPromise.instance().terminate();
  }
}
```

## Making your Record Class and Table
A Record is a POJO that is stored in a table in the database, A sample [ComplexRecord class](https://github.com/android-promise/database/blob/master/dbappbase/src/main/java/com/dev4vin/dbappbase/baseapp/model/ComplexRecord.kt) referenced below
### Record Class

```kotlin
class ComplexRecord : TimeAware() {

  var intVariable: Int? = null

  var floatVariable: Float? = null

  var doubleVariable: Double? = null

  var stringVariable: String? = null
  ...
}
```
This class can extend from [TimeAware class](https://github.com/android-promise/database/blob/master/promisedb/src/main/java/promise/model/TimeAware.java) and retrieve createdAt and updatedAt fields

### The Table Class
A table is a functional class that has methods that manipulate records in the database.
A sample [ComplexRecordTable class](https://github.com/android-promise/database/blob/master/dbapp/src/main/java/promise/dbapp/model/ComplexRecordTable.kt) will manipulate [ComplexRecord](https://github.com/android-promise/database/blob/master/dbappbase/src/main/java/com/dev4vin/dbappbase/baseapp/model/ComplexRecord.kt) within the database.
```kotlin

@Table(
    tableName = "name_of_complex_model_table",
    indexes = [
      Table.Index(
          columnName = "int"
      ),
      Table.Index(
          columnName = "double",
          unique = true
      )
    ]
)

class ComplexRecordTable(database: FastDatabase) : FastTable<ComplexRecord>(database) {

  override fun onUpgrade(database: SQLiteDatabase, v1: Int, v2: Int) {
    if (v1 == 1 && v2 == 2) {
      // add when migrating from version 1 to 2
      addColumns(database, flagVariableColumn)
    }
  }

  override val columns: List<out Column<*>>
    get() = List.fromArray(intVariableColumn,
        floatVariableColumn,
        doubleVariableColumn,
        stringVariableColumn
    )

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

  companion object {
    val intVariableColumn: Column<Int> = Column("int", Column.Type.INTEGER.NOT_NULL(), 1)
    val floatVariableColumn: Column<Float> = Column("float", Column.Type.INTEGER.NOT_NULL(), 2)
    val doubleVariableColumn: Column<Double> = Column("double", Column.Type.INTEGER.NOT_NULL(), 3)
    val stringVariableColumn: Column<String> = Column("string", Column.Type.INTEGER.NOT_NULL(), 4)
  }
}
```
Each table should extend [FastTable Class](https://github.com/android-promise/database/blob/master/promisedb/src/main/java/promise/db/FastTable.java) class or implement [Table Interface](https://github.com/android-promise/database/blob/master/promisedb/src/main/java/promise/db/Table.java).
A table also exposes columns that will be created in it in the database,
```kotlin
override val columns: List<out Column<*>>
    get() = List.fromArray(intVariableColumn,
        floatVariableColumn,
        doubleVariableColumn,
        stringVariableColumn
    )
```

It also overrides methods that will serialize and deserialize the model
##### Serialization
converting the model into content values for save and update operations
```kotlin
  override fun serialize(t: ComplexRecord): ContentValues = ContentValues().apply {
    put(intVariableColumn.name, t.intVariable)
    put(floatVariableColumn.name, t.floatVariable)
    put(doubleVariableColumn.name, t.doubleVariable)
    put(stringVariableColumn.name, t.stringVariable)
  }
``` 
##### Deserialization
creating the model from a cursor for read operations
```kotlin
override fun deserialize(e: Cursor): ComplexRecord = ComplexRecord().apply {
    intVariable = e.getInt(intVariableColumn.index)
    floatVariable = e.getFloat(floatVariableColumn.index)
    doubleVariable = e.getDouble(doubleVariableColumn.index)
    stringVariable = e.getString(stringVariableColumn.index)
  }
```
> Note for better performance, the columns should be initialized as static fields
```kotlin
companion object {

    fun id() = id

    val intVariableColumn: Column<Int> = Column("int", Column.Type.INTEGER.NOT_NULL(), 1)
    val floatVariableColumn: Column<Float> = Column("float", Column.Type.INTEGER.NOT_NULL(), 2)
    val doubleVariableColumn: Column<Double> = Column("double", Column.Type.INTEGER.NOT_NULL(), 3)
    val stringVariableColumn: Column<String> = Column("string", Column.Type.INTEGER.NOT_NULL(), 4)

  }
```
> id is a static column for primary key initialized in the base class

## Making your database
The database expects a list of tables to be registered with it, In our example, the ComplexRecordTable.
If you want to utilize rxJava in your queries, extend from [ReactiveFastDatabase](https://github.com/android-promise/database/blob/master/promisedb/src/main/java/promise/db/ReactiveFastDatabase.java) otherwise extend from [FastDatabase](https://github.com/android-promise/database/blob/master/promisedb/src/main/java/promise/db/FastDatabase.java)
##### [AppDatabase](https://github.com/android-promise/database/blob/master/dbapp/src/main/java/promise/dbapp/model/AppDatabase.kt)
```kotlin

@Database(
    tables = [
      ComplexRecordTable::class
    ]
)
object AppDatabase {

  fun allComplexModels(result: Result<IdentifiableList<out ComplexRecord>, Throwable>) {
    val items = complexModelTable.findAll()
    if (items.isEmpty()) {
      saveSomeComplexModels(Result<Boolean, Throwable>()
          .withCallBack {
            allComplexModels(result)
          })
      return
    }
    result.response(items)
   }

  private fun saveSomeComplexModels(result: Result<Boolean, Throwable>) {
    complexModelTable.save(IdentifiableList(ComplexRecord.someModels()))
    result.response(true)
  }

  // can also use in memory database, no name and no migrations
  val inMemoryDatabase = createInMemoryDatabase(AppDatabase::class.java)

  val instance = createDatabase(AppDatabase::class.java, "db_name")

  val complexModelTable: ComplexRecordTable by lazy {
    instance.obtain<ComplexRecordTable>(ComplexRecordTable::class.java)
  }

}
```
The tables are registered in the database annotation
```kotlin
@Database(
    tables = [
      ComplexRecordTable::class
    ]
)
```
> For better performance, always initialize your tables lazily as static fields
```kotlin
  ...
val instance = createDatabase(AppDatabase::class.java)

val complexModelTable: ComplexRecordTable by lazy {
  instance.obtain<ComplexRecordTable>(ComplexRecordTable::class.java)
}
```

## Consuming your database
Consuming you database is as easy as just calling methods from the custom database, An example in our [MainActivity](https://github.com/android-promise/database/blob/master/dbapp/src/main/java/promise/dbapp/MainActivity.kt)
```kotlin
 ...
 override fun onPostCreate(savedInstanceState: Bundle?) {
    super.onPostCreate(savedInstanceState)
    ...
    AppDatabase.allComplexModels(Result<IdentifiableList<out ComplexRecord>, Throwable>()
        .withCallBack {
          if (it.isNotEmpty()) {
            complex_values_textview.text = it.toString()
          } else complex_values_textview.text = "empty list"
        }
        .withErrorCallBack { complex_values_textview.text = it.message })

    clear_button.setOnClickListener {
      AppDatabase.instance.deleteAll()
      complex_values_textview.text = ""
    }

  }
...
```
Note, you could also interract with the table directly
```kotlin
...

    val complexRecordTable = AppDatabase.complexModelTable
    var items = complexRecordTable.findAll()
    if (items.isEmpty()) {
      complexRecordTable.save(IdentifiableList(ComplexRecord.someModels()))
      items = complexRecordTable.findAll()
    }
    complex_values_textview.text = items.toString()
```
> All the crud functionality exist on the table,
> Note avoid querying the database on the UI thread

## Migrations
Assume, on our app, we decided to add another table for another datatype, in this case NewRecord
We'll upgrade our database to version 2 and add the table in our database as this
##### AppDatabase
```kotlin
...
val instance = createDatabase(AppDatabase::class.java, "db_name",
      object : Migration {
        override fun onMigrate(database: FastDatabase,
                               sqLiteDatabase: SQLiteDatabase,
                               oldVersion: Int,
                               newVersion: Int) {
          if (oldVersion == 1 && newVersion == 2) {
            // we added new record table when database version is 1, and therefore need to add it version 2
            database.add(sqLiteDatabase, database.obtain(NewRecordTable::class.java))
          }
        }
      })
...
```
Amd the update our new table in table registry so that new installs get the new table as well
```kotlin
...
@Database(
    version = 2,
    tables = [
      ComplexRecordTable::class,
      // added table
      NewRecordTable::class
    ]
)
...
```
> Finally upgrade the version of the database to 2

A second scenario, in our complex table, we decided to add another column for storing a flag
We'll also update the table
##### ComplexRecord
```kotlin
...
  // added new field
  var flagString: String? = null
...
```
##### ComplexRecordTable
Adding the column in the table
The column added must be nullable, replace Column.Type.INTGER.NOT_NULL with Column.Type.TEXT.NULLABLE
```kotlin
companion object {
   ...
   // new column, this should be nullable and type TEXT
    val flagVariableColumn: Column<String> = Column("flag", Column.Type.INTEGER.NOT_NULL(), 5)
    // notice update on the index of 5
  }
```
Adding this column to the table
```kotlin
...
override fun onUpgrade(database: SQLiteDatabase?, v1: Int, v2: Int) {
    if (v1 == 1 && v2 ==2) {
      // add when migrating from version 1 to 2
      addColumns(database, flagVariableColumn)
    }
  }
...
```
> Do not forget to add this new column to the clumns list so new installs do not have to upgrade
```kotlin
...
 override val columns: List<out Column<*>>
    get() = List.fromArray(intVariableColumn,
        floatVariableColumn,
        doubleVariableColumn,
        stringVariableColumn,
        // add this here to have it on new installs
        flagVariableColumn
    )
...
```
Finally update your serialization and deserialization logic
```kotlin
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
```

## New features on the way
watch this repo to stay updated 

# Developed By
* Peter Vincent - <dev4vin@gmail.com>
# Donations
If you'd like to support this library development, you could buy me coffee here:
* [![Become a Patreon]("https://c6.patreon.com/becomePatronButton.bundle.js")](https://www.patreon.com/bePatron?u=31932751)

Thank you very much in advance!

#### Pull requests / Issues / Improvement requests
Feel free to contribute and ask!<br/>

# License

    Copyright 2018 Peter Vincent

    Licensed under the Apache License, Version 2.0 Android Promise;
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

