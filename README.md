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
     implementation 'com.github.android-promise:database:1.0'
     implementation 'com.github.android-promise:commons:1.0'
}
```

### Initialization
Initialize Promise in your main application file, entry point

##### App.java
```java
  ...
  @Override
  public void onCreate() {
    super.onCreate();
    Promise.init(this);
    ...
  }
  ...
```

## Making your Record Class and Table
A Record is a POJO that is stored in a table in the database, A sample [ComplexRecord class](https://github.com/android-promise/database/blob/master/dbapp/src/main/java/promise/dbapp/model/ComplexRecord.kt) referenced below
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
A sample [ComplexRecordTable class](https://github.com/android-promise/database/blob/master/dbapp/src/main/java/promise/dbapp/model/ComplexRecordTable.kt) will manipulate [ComplexRecord](https://github.com/android-promise/database/blob/master/dbapp/src/main/java/promise/dbapp/model/ComplexRecord.kt) within the database.
```kotlin

class ComplexRecordTable : FastTable<ComplexRecord>() {
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
  override fun getColumns(): List<Column<*>> {
   return List.fromArray(intVariableColumn, floatVariableColumn, doubleVariableColumn, stringVariableColumn)
  }

  override fun deserialize(e: Cursor): ComplexModel = ComplexModel().apply {
    intVariable = e.getInt(intVariableColumn.index)
    floatVariable = e.getFloat(floatVariableColumn.index)
    doubleVariable = e.getDouble(doubleVariableColumn.index)
    stringVariable = e.getString(stringVariableColumn.index)
  }

  override fun serialize(t: ComplexModel): ContentValues = ContentValues().apply {
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
 override fun getColumns(): List<Column<*>> {
   return List.fromArray(intVariableColumn, floatVariableColumn, doubleVariableColumn, stringVariableColumn)
  }
```
It also provides a name for it in the database
```kotlin
override fun getName(): String = "name_of_complex_model_table"
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

class Database : ReactiveDatabase(name, version, null, null) {
  /**
   *
   */
  private val compositeDisposable: CompositeDisposable by lazy { CompositeDisposable() }
  /**
   * @return
   */
  override fun onTerminate(): CompositeDisposable {
    return compositeDisposable
  }
  /**
   *
   * @return
   */
  override fun tables(): List<Table<*, in SQLiteDatabase>> = List.fromArray(complexModelTable)

  fun allComplexModels(result: Result<SList<out ComplexModel>, Throwable>) {
    compositeDisposable.add(findAllAsync(complexModelTable)
        .subscribeOn(Schedulers.from(Promise.instance().executor()))
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ list ->
          if (list.isEmpty())
            saveSomeComplexModels(Result<Boolean, Throwable>()
                .withErrorCallBack { allComplexModels(result) }
                .withErrorCallBack {
                  result.error(it) }) else
            result.response(SList(list))

        }, {
          result.error(it)
        }))
  }

  private fun saveSomeComplexModels(result: Result<Boolean, Throwable>) {
    compositeDisposable.add(saveAsync(SList(ComplexModel.someModels()), complexModelTable)
        .subscribeOn(Schedulers.from(Promise.instance().executor()))
        .observeOn(Schedulers.from(Promise.instance().executor()))
        .subscribe({
          result.response(it)
        }, {
          result.error(it)
        }))
  }

  override fun deleteAllAsync(): Maybe<Boolean> =
      super.deleteAllAsync().subscribeOn(Schedulers.from(Promise.instance().executor()))

  companion object {
    @Volatile
    var instance: AppDatabase? = null
    private var LOCK = Any()
    operator fun invoke(): AppDatabase = instance
        ?: synchronized(LOCK) {
          instance ?: AppDatabase()
              .also {
                instance = it
              }
        }
    const val name = "complex_db_name"
    const val version = 1
    val complexModelTable: ComplexRecordTable by lazy { ComplexRecordTable(AppDatabase()) }
  }
}
```
The tables are registered with the following method of the database
```kotlin
override fun tables(): List<Table<*, in SQLiteDatabase>> = List.fromArray(complexModelTable)
```
> For better performance, always initialize your tables lazily as static fields
```kotlin
  ...
 companion object {
    @Volatile
    var instance: AppDatabase? = null
    private var LOCK = Any()
    operator fun invoke(): AppDatabase = instance
        ?: synchronized(LOCK) {
          instance ?: AppDatabase()
              .also {
                instance = it
              }
        }

    const val name = "complex_db_name"
    const val version = 1
    val complexModelTable: ComplexRecordTable by lazy { ComplexRecordTable(AppDatabase()) }
  }
```

## Consuming your database
Consuming you database is as easy as just calling methods from the custom database, An example in our [MainActivity](https://github.com/android-promise/database/blob/master/dbapp/src/main/java/promise/dbapp/MainActivity.kt)
```kotlin
 ...
override fun onPostCreate(savedInstanceState: Bundle?) {
    super.onPostCreate(savedInstanceState)
    ...
    val database = AppDatabase()
    database.allComplexModels(Result<IdentifiableList<out ComplexRecord>, Throwable>()
        .withCallBack {
          if (it.isNotEmpty()) {
            complex_values_textview.text = it.toString()
          } else complex_values_textview.text = "empty list"
        }
        .withErrorCallBack { complex_values_textview.text = it.message })

    clear_button.setOnClickListener {
      database.deleteAllAsync()
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe {
            complex_values_textview.text = ""
          }
    }
  }
...
```
Note, you could also interract with the table directly
```kotlin
...
val complexRecordTable = AppDatabase.complexModelTable
```
> All the crud functionality exist on the table
# Developed By
* Peter Vincent - <dev4vin@gmail.com>
# Donations
If you'd like to support this library development, you could buy me coffee here:
* [![Become a Patreon]("https://c6.patreon.com/becomePatronButton.bundle.js")](https://www.patreon.com/bePatron?u=31165349)

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

