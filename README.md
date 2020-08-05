# Android Promise Database [![](https://jitpack.io/v/android-promise/database.svg)](https://jitpack.io/#android-promise/database)
 Manage SqLite databases in android with ease

# Table of Contents

**[Sample Application](##SampleApplication)**<br>
***[Entity Classes](***EntityClasses)***<br>
***[Database Class](###DatabaseClass)***<br>
***[TypeConverter Class](###DatabaseClass)***<br>
***[Entity Relations](###EntityRelations)***<br>
***[Database Initialization](###DatabaseInitialization)***<br>
***[Migrations](###Migrations)***<br>
**[Setup](##Setup)**<br>
**[Initialization](##Initialization)**<br>
**[Next Steps, Credits, Feedback, License](#next-steps)**<br>

## Sample Application
### Entity Classes
Pojo classes to be persisted are annotated with @Entity
The classes can either implement Identifiable<Integer> or extend from ActiveRecord
This classes must hava a no args constructor or no constructors at all

> Optionally add the table name in the annotation

#### Photo Class
```kotlin

@SuppressLint("ParcelCreator")
@Entity
class Photo: ActiveRecord<Photo>() {
	var albumId: Int? = null
	var title: String? = null
	var url: String? = null
	var thumbnailUrl: String? = null

  // denotes a photo has one post
	@HasOne var post: Post? = null

	override fun getEntity(): Photo {
		return this
	}

}
  
}
```
#### Todo Class
```java

@SuppressLint("ParcelCreator")
@Entity
public class Todo extends ActiveRecord<Todo> {
	private int uid;
	private boolean completed;
	private String title;
  private int userId;
  
  // denotes a todo item has one photo record
	@HasOne
  private Photo photo;
  // include parcelable implementation if extending from ActiveRecord
  
}
```
#### Post Class
```java

@SuppressLint("ParcelCreator")
@Entity
public class Post extends ActiveRecord<Post> {
	private ID uId;
	private String title;
	private String body;
	private int userId;

  // denotes a post has many post comments
	@HasMany
	private List<PostComment> comments;

  // denotes a post has many photos
	@HasMany
	private List<Photo> photos;
}
```

> Include correct getter and setter signatures for Java file or generated code will not compile, including fields convertable via type converter and fields marked as relation

> BOOLEAN fields MUST NOT start with `is`

### Database Class
Database classe is annotated with @DatabaseEntity and contain a list
of persistable entity classes
The class must be abstract and extend from PromiseDatabase
> **There can only be one database class in a module**
```kotlin
@DatabaseEntity(
    persistableEntities = [
      PostComment::class,
      Photo::class,
      Post::class,
      Todo::class
    ]
)
abstract class AppDatabase(fastDatabase: FastDatabase)
  : PromiseDatabase(fastDatabase)
```

Rebuild your project to generate database implementation

### TypeConverter Class
A type converter is a utility that helps to convert fields not directly persistable to persistable,
For instance the Post entity has named uId with a type
```kotlin
data class ID(val id: String)
```
To make the uId field persistable, we provide a type converter
```kotlin
@TypeConverter
class AppTypeConverter {

  fun toUniqueId(data: String): ID = ID(data)

  fun toString(data: ID?): String = data?.id ?: ""

}
```
A type converter must be annotated with @TypeConverter and contain non static methods that converts between the non persistable type to a string and vice verser
Conversion methods in a type converter should have only one parameter and must return
` Type converter is not for fields that are relations`
**There can only be one type converter in one module**

> Without a type converter provision, the compiler will not generate columns for non persistable fields

### Entity Relations

#### HasOne Relation
Denotes one entity has one other entity, a sample
```java
@Entity
public class Todo extends ActiveRecord<Todo> {
  // other fields
	...

  // denotes this todo has one photo
  @HasOne
  private Photo photo;
}
```
To utilize the relation, the compiler will generate a class called TodoRelationsDao with convenient methods, a snippet of generated code below
```java

public final class TodoRelationsDao {
  private TodoesTable todoesTable;

  private PhotosTable photosTable;

  private TodoRelationsDao(TodoesTable todoesTable, PhotosTable photosTable) {
    this.todoesTable = todoesTable;
    this.photosTable = photosTable;
  }

  public IdentifiableList<? extends Todo> paginateWithPhotos(int skip, int limit) {
    IdentifiableList<? extends Todo> todoes = todoesTable.find().paginateDescending(skip, limit);
    return populateWithPhotos(todoes);
  }

  ...
  // other convenient methods
  ...
  
  public Photo getPhoto(Todo todo) {
    return photosTable.findById(todo.getPhoto().getId());
  }

}
```
Instances of RelationDaos are retrieved from the generated database class


#### HasMany Relation
Denotes one entity has many entities of same type and the other entity has one entity of this type, a sample a post has many post comments and a post comment has one post

Post Entity
```java

@Entity
public class Post extends ActiveRecord<Post> {
  // other fields
	...
	@HasMany
  private List<PostComment> comments;
}

```
PostComment Entity
```java
@SuppressLint("ParcelCreator")
@Entity
public class PostComment extends ActiveRecord<PostComment> {
  // other fields
	...

	@HasOne
  private Post post;
}
```
**Both relations must exist at the same time**
To utilize the relation, the compiler will generate a class called PostRelationsDao with convenient methods, a snippet of generated code below
```java

public final class PostRelationsDao {
  private PostsTable postsTable;

  private PostCommentsTable postCommentsTable;

  private PhotosTable photosTable;

  private PostRelationsDao(PostsTable postsTable, PostCommentsTable postCommentsTable,
      PhotosTable photosTable) {
    this.postsTable = postsTable;
    this.postCommentsTable = postCommentsTable;
    this.photosTable = photosTable;
  }

  public IdentifiableList<? extends Post> paginateWithComments(int skip, int limit) {
    IdentifiableList<? extends Post> posts = postsTable.find().paginateDescending(skip, limit);
    posts.forEach(new Consumer<Post>() {
      @Override
      public void accept(Post post) {
        post.setComments(new IdentifiableList<>(getComments(post)));
      }
    } );
    return posts;
  }

  public IdentifiableList<? extends Post> listWithComments() {
    IdentifiableList<? extends Post> posts = postsTable.findAll();
    posts.forEach(new Consumer<Post>() {
      @Override
      public void accept(Post post) {
        post.setComments(new IdentifiableList<>(getComments(post)));
      }
    } );
    return posts;
  }
  ...
  // more convenience methods
}
```
Instances of RelationDaos are retrieved from the generated database class

Snippet of RelationDao usage in main activity
```kotlin
...
// generate 5 posts and each post 4 comments
val posts = IdentifiableList(List.generate(5) {
      Post().apply {
        uId = ID(it.toString())
        title = "post".plus(it)
        body = "body".plus(it)
        userId = it
        comments = List.generate(4) {
          PostComment().apply {
            name = "nm".repeat(it)
            uId = ID((it + 1).toString())
            body = "hbytcvbcrxgfvbtrxt"
            email = "ejmail;jgfccghcfcvhbhcgvb"
          }
        }
      }
    })

  // save the posts
   postRelationsDao.saveWithComments(posts)
  // reading the posts
   val posts1 = postRelationsDao.listWithComments()
  // display the posts
   complex_values_textview.text = posts1.toString()
  // delete comments for each post and then delete the post
   posts1.forEach {
     postRelationsDao.deleteComments(it)
     it.delete()
   }
 ...
```


### Database Initialization
The compiler generates a database class for creating your database implementation and accesing tables
and relation daos, sampled code from generated database file below

```java
 /**
   * Creates the simplest database with name specified
   * @Param name the name of the database
   */
  public static AppDatabaseImpl createDatabase(String name) {
    if (initialized) throw new IllegalStateException("Database already created");
    initialized = true;
    return new AppDatabaseImpl(FastDatabase.createDatabase(AppDatabaseImpl.class, name, getMigration()));
  }

  /**
   * Creates the simplest database with name specified with callback
   * Callback can be used to pre populate database with records or
   * set flags like foreign keys
   * @Param name the name of the database
   * @Param databaseCreationCallback callback
   */
  public static AppDatabaseImpl createDatabase(String name,
      DatabaseCreationCallback databaseCreationCallback) {
    if (initialized) throw new IllegalStateException("Database already created");
    initialized = true;
    return new AppDatabaseImpl(FastDatabase.createDatabase(AppDatabaseImpl.class, name, getMigration(), databaseCreationCallback));
  }

  /**
   * Creates an in memory database, useful for tests
   */
  public static AppDatabaseImpl createInMemoryDatabase() {
    if (initialized) throw new IllegalStateException("Database already created");
    initialized = true;
    return new AppDatabaseImpl(FastDatabase.createInMemoryDatabase(AppDatabaseImpl.class));
  }

  /**
   * Creates an in memory database, useful for tests
   * Callback can be used to pre populate database with records or
   * set flags like foreign keys
   * @Param databaseCreationCallback callback
   */
  public static AppDatabaseImpl createInMemoryDatabase(
      DatabaseCreationCallback databaseCreationCallback) {
    if (initialized) throw new IllegalStateException("Database already created");
    initialized = true;
    return new AppDatabaseImpl(FastDatabase.createInMemoryDatabase(AppDatabaseImpl.class, databaseCreationCallback));
  }

  /**
   * Creates an in memory database, enables calling rx DML functions in the tables
   */
  public static AppDatabaseImpl createReactiveInMemoryDatabase() {
    if (initialized) throw new IllegalStateException("Database already created");
    initialized = true;
    return new AppDatabaseImpl(FastDatabase.createInMemoryReactiveDatabase(AppDatabaseImpl.class));
  }

  /**
   * Creates an in memory database, enables calling rx DML functions in the tables
   * Callback can be used to pre populate database with records or
   * set flags like foreign keys
   * @Param databaseCreationCallback callback
   */
  public static AppDatabaseImpl createReactiveInMemoryDatabase(
      DatabaseCreationCallback databaseCreationCallback) {
    if (initialized) throw new IllegalStateException("Database already created");
    initialized = true;
    return new AppDatabaseImpl(FastDatabase.createInMemoryReactiveDatabase(AppDatabaseImpl.class, databaseCreationCallback));
  }

  /**
   * Creates database, that enables calling rx DML functions in the tables
   * @Param name name of the database 
   */
  public static AppDatabaseImpl createReactiveDatabase(String name) {
    if (initialized) throw new IllegalStateException("Database already created");
     initialized = true;
    return new AppDatabaseImpl(FastDatabase.createReactiveDatabase(AppDatabaseImpl.class, name, getMigration()));
  }

  /**
   * Creates database, that enables calling rx DML functions in the tables
   * Callback can be used to pre populate database with records or
   * set flags like foreign keys
   * @Param name name of the database 
   * @Param databaseCreationCallback callback
   */
  public static AppDatabaseImpl createReactiveDatabase(String name,
      DatabaseCreationCallback databaseCreationCallback) {
    if (initialized) throw new IllegalStateException("Database already created");
     initialized = true;
    return new AppDatabaseImpl(FastDatabase.createReactiveDatabase(AppDatabaseImpl.class, name, getMigration(), databaseCreationCallback));
  }
```


### Migrations

#### Migrations For Entities
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
And then add the new entity to the database and upgrade database version to 2

```kotlin

@DatabaseEntity(
   
    persistableEntities = [
      /// other entities
      Sales::class
    ],
    version = 2
)

abstract class AppDatabase(fastDatabase: FastDatabase) : PromiseDatabase(fastDatabas) 
```

Adding columns to an existing entity, annotate the new field with Migrate, a migration will be generated by the compiler >

#### Migrations For Columns
Annotate the added field with @Migrate and include fromVersion and toVersion
If a field is to pass multiple migrations, annotate it with @Migrations and add the migrate annotations in the 
migrations annotation
##### Sales Entity
```java
...
  // added new field
  @Migrate(fromVersion = 2, toVersion = 3, action = MigrationOptions.CREATE)
  private String status;
...
```

## Setup
##### Project level build.gradle
Add jitpack repository
```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}

```
##### Module level build.gradle
Compile with java 8, add rxJava dependency of using rx DML functions in the tables
```
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
##### Initialization
Initialize Promise in your main application class

##### App.java
```java
public class App extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
    AndroidPromise.init(this, BuildConfig.DEBUG);
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
* Peter Vincent - [Portfolio](https://dev4vin.github.io/info)

# Donations
If you'd like to support this library development, you could buy me coffee here:
* [![Become a Patreon]("https://c6.patreon.com/becomePatronButton.bundle.js")](https://www.patreon.com/bePatron?u=31932751)

#### Pull requests / Issues / Improvement requests
Feel free to contribute and ask!<br/>

# License

    Copyright 2018 Android Promise Database

    Licensed under the Apache License, Version 2.0 Android Promise;
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

