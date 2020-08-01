# Android Promise Database [![](https://jitpack.io/v/android-promise/database.svg)](https://jitpack.io/#android-promise/database)
 Manage SqLite databases in android with ease


## Making your Record Class
Annotate the entity class with Entity, the class can either implement Identifiable<Integer> or extend from ActiveRecord and override parcelable functionality.
A sample:
### Person Class
> Optionally add the table name in the annotation

```java

@Entity
public class Person extends ActiveRecord<Person> {
  // Indexed column
  @Index
  private String name;
  private int age;
  private boolean aged;
  private float height;
  private double marks;
  private String email;
  private String phoneNumber;
  // denote a person has many dogs
  @HasMany(entity = Dog.class)
  private List<Dog> dogs;

  public Person() {
  }

  @NotNull
  @Override
  public Person getEntity() {
    return this;
  }
  // include parcelable implementation if extending from ActiveRecord
  // include getters and setters for persistable fields
}
```
> A MUST include correct getter and setter signatures for Java file or generated code will not compile

> if entity is a kt file, DO NOT include any getter and setters, BOOLEAN fields MUST NOT start with `is`


The compiler generates a table class for the entity with the below DML functions


| DML FUNCTION                                               | ILLUSTRATION                                                                 |
|------------------------------------------------------------|------------------------------------------------------------------------------|
| querySql(sql: String): Cursor                              | Executing raw queries                                                  |
| save(t: T): Long                                           | Saves one entity to the table                                                |
| save(list: IdentifiableList<out T>): Boolean               | Saves many entities to the table                                             |
| update(t: T): Boolean                                      | Update an entity to the table                                                |
| update(t: T, column: Column<*>): Boolean                   | Update with condition on a specified column                                  |
| queryBuilder(): QueryBuilder                               | Returns a queryBuilder for more complex queries                              |
| query(queryBuilder: QueryBuilder): Cursor                  | Executes the query returned from the query builder                           |
| find(): TableCrud.Extras<T>                                | ![](findoptions.PNG?raw=true)                                                                          |
| findById(idLong: Long): T?                                 | Finds one entity with the ID specified                                       |
| findOne(vararg columns: Column<*>): T?                     | Finds one entity with the condition specified in the columns                 |
| findAll(): IdentifiableList<out T>                         | Returns all records from the table                                           |
| findAll(vararg column: Column<*>): IdentifiableList<out T> | Returns all records from the table specified by the condition in the columns |
| delete(column: Column<*>): Boolean                         | Delete an entity specified by the condition in the columns                   |
| delete(t: T): Boolean                                      | Delete an entity                                                             |
| <N> delete(column: Column<N>, list: List<out N>): Boolean  | Delete a list of records whose column values are in the provided list        |
| clear(): Boolean                                           | Clear all records from the table                                             |
| lastId: Int                                                | Returns the id of the last record                                            |


## Making your database
The database expects a list of entities to be registered with it
```kotlin

@DatabaseEntity(
    persistableEntities = [
      Person::class,
      Dog::class,
      Sales::class
    ]
)
abstract class AppDatabase : PromiseDatabase {
  private val personsTable: FastTable<Person> by lazy { tableOf(Person::class.java) }
  private val dogsTable: FastTable<Dog> by lazy { tableOf(Dog::class.java) }
  fun listAllPersons() = personsTable.findAll()

  fun listAllPersonsWithEmail(): List<Person> {
    val cursor = personsTable.query(personsTable.queryBuilder().whereAnd(Criteria.notIsNull(PersonsTable.emailColumn)))
    val persons = ArrayList<Person>()
    while (cursor.moveToNext()) {
      persons.add(personsTable.deserialize(cursor))
    }
    cursor.close()

    return persons
  }
  fun listAllDogs() = dogsTable.findAll()
//  @OneToMany(parent = Person::class, child = Dog::class)
//  @FindAll
//  abstract fun getPersonsWithDogs(array: Array<Column<*>>?): List<Person>

//  @OneToOne(parent = Dog::class, child = Person::class)
//  @FindAll
//  abstract fun getDogsWithPersons(array: Array<Column<*>>?): List<Dog>

}
```
> Database class MUST be abstract even if it has no abstract methods to override

> Database class MUST implement PromiseDatabase


## Consuming your database
The compiler generates a database class for creating your database implementation and accesing tables.
If using Dagger, initialize it from a module
```kotlin
 ...
 
@Module
object DatabaseDependencies {

  @Provides
  @Singleton
  @JvmStatic
  fun providesAppDatabase(): AppDatabase {
    return AppDatabase_Impl.createDatabase("name")
  }

  // tables obtained are Singleton, no need to annotate with Singleton
  @Provides
  @JvmStatic
  fun providesPersonsTable(appDatabase: AppDatabase): PersonsTable {
    return appDatabase.databaseInstance.obtain<PersonsTable>(PersonsTable::class.java)
  }
}
...
```
> All the crud functionality exist on the table

## Migrations
Adding an entity to be persisted to the database from version 1 to version 2 of the database

Annotate it with AddedEntity
##### Sales Entity
```java

@Entity
// register it for migration from version 1 of database to version 2
@AddedEntity(fromVersion = 1, toVersion = 2)
public class Sales extends ActiveRecord<Sales> {

  private String salesId;
  private double salePrice;

  @Text(columnName = "descrip", nullable = false)
  private String description;

  @Index
  private String pickupDate;
  // other fields follow

  public Sales() {
  }
}
  // getters and setters and parcelable implementation
```
And then add the new entity to the database 

```kotlin
...

@DatabaseEntity(
    persistableEntities = [
      Person::class,
      Sales::class
    ],
    version = 2
)
abstract class AppDatabase : PromiseDatabase {
  ...

  ...
}

...
```
> Finally upgrade the version of the database to 2

Adding columns to an existing entity, annotate the new field with Migrate, a migration will be generated by the compiler >
##### Sales Entity
```java
...
  // added new field
  @Migrate(fromVersion = 2, toVersion = 3, action = MigrationOptions.CREATE)
  private String status;
...
```

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
     implementation 'com.github.android-promise.database:androidpromisedatabase:1.0.2-beta6'
     kapt 'com.github.android-promise.database:androidpromisedatabasecompiler:1.0.2-beta6'
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

