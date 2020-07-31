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

## Making your Record Class
A Record is a POJO that is stored in a table in the database, the class can either implement Identifiable<Integer> or extend from ActiveRecord and override parcelable functionality , A sample
### Person Class

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


## Making your database
The database expects a list of entities to be registered with it, In our example and must implement PromiseDatabase and not final
```kotlin

@DatabaseEntity(
    persistableEntities = [
      Person::class,
      Dog::class,
      Sales::class
    ]
)
/**
 *
 */
abstract class AppDatabase : PromiseDatabase {


  /**
   *
   */
  private val personsTable: FastTable<Person> by lazy { tableOf(Person::class.java) }

  /**
   *
   */
  private val dogsTable: FastTable<Dog> by lazy { tableOf(Dog::class.java) }

  /**
   *
   */
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

  /**
   *
   */
  fun listAllDogs() = dogsTable.findAll()

//  /**
//   *
//   */
//  @OneToMany(parent = Person::class, child = Dog::class)
//  @FindAll
//  abstract fun getPersonsWithDogs(array: Array<Column<*>>?): List<Person>
//
//  /**
//   *
//   */
//  @OneToOne(parent = Dog::class, child = Person::class)
//  @FindAll
//  abstract fun getDogsWithPersons(array: Array<Column<*>>?): List<Dog>

}
```

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
##### Sales Entity
```java

@Entity
@AddedEntity(fromVersion = 1, toVersion = 2)
public class Sales extends ActiveRecord<Sales> {

  private String name;
  @Migrate(fromVersion = 2, toVersion = 3, action = MigrationOptions.DROP)
  // age column to be dropped when migrating 2 to 3
  private int age;

  private String descrip;
  @Index
  private String pickupDate;
  // other fields follow

  public Sales() {
  }
}
  // getters and setters and parcelable implementation
```
And then add the new entity to the database 
> Finally upgrade the version of the database to 2
```kotlin
...

@DatabaseEntity(
    /**
     *
     */
    persistableEntities = [
      Person::class,
      Sales::class
    ],
    version = 2
)
/**
 *
 */
abstract class AppDatabase : PromiseDatabase {
  ...
  // 
  ...
}

...
```

Adding columns to an existing entity, annotate the new field with Migrate, a migration will be generated by the compiler >
##### Sales Entity
```java
...
  // added new field
  @Migrate(fromVersion = 2, toVersion = 3, action = MigrationOptions.CREATE)
  private String descrip;
...
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

