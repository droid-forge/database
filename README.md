
[![-----------------------------------------------------](https://raw.githubusercontent.com/andreasbm/readme/master/assets/lines/colored.png)](#android-promise-database-httpsjitpackiovandroid-promisedatabasesvghttpsjitpackioandroid-promisedatabase)

# ➤ Android Promise Database [![](https://jitpack.io/v/android-promise/database.svg)](https://jitpack.io/#android-promise/database)
 Manage SqLite databases in android with ease by It mapping Pojo and data classes to the SQlite tables, In most of database interactions, most queries are generated at runtime.


[![-----------------------------------------------------](https://raw.githubusercontent.com/andreasbm/readme/master/assets/lines/colored.png)](#table-of-contents)

# ➤ Table of Contents

***[Entity](***Entity)***<br>
***[Database](###Database)***<br>
***[TypeConverter](###TypeConverter)***<br>
***[Relations](###Relations)***<br>
***[Initialization](###Initialization)***<br>
***[Migrations](###Migrations)***<br>
**[Setup](##Setup)**<br>
**[Next Steps, Credits, Feedback, License](#next-steps)**<br>


[![-----------------------------------------------------](https://raw.githubusercontent.com/andreasbm/readme/master/assets/lines/colored.png)](#sample)

### Entity
Pojo classes to be persisted are annotated with `@Entity`

The classes can either implement `Identifiable<Integer>` or extend from `ActiveRecord`

**This classes must hava a no args constructor or no constructors at all**

> Optionally add the table name in the annotation

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
```

> Include correct getter and setter signatures for Java file or generated code will not compile, including fields convertable via type converter and fields marked as relation

> BOOLEAN fields MUST NOT start with `is`

> Kotlin data classes are not supported yet 

### Database
Database classe is annotated with `@DatabaseEntity` and contain a list
of persistable entity classes

*The class must be abstract and extend from `PromiseDatabase`*
> **There can only be one database class in a module**
```kotlin
@DatabaseEntity(
    persistableEntities = [
      PostComment::class,
      Photo::class,
      Post::class,
      Todo::class,
      Like::class
    ]
)
abstract class AppDatabase(fastDatabase: FastDatabase)
  : PromiseDatabase(fastDatabase) {

  init {
    fastDatabase.accept(object : Visitor<FastDatabase, Unit> {
      override fun visit(t: FastDatabase) {
        t.setErrorHandler {
          LogUtil.e(TAG, "database error: ${it.path}")
        }
        t.fallBackToDestructiveMigration()
      }
    })
  }

  companion object {
    val TAG: String = LogUtil.makeTag(AppDatabase::class.java)
  }
}
```
*Rebuild your project to generate database implementation*

### TypeConverter
A type converter is a utility that helps to convert fields not directly persistable to be persistable by converting to and from String,
For instance the Post entity has named uId with a type
```kotlin
data class ID(val id: String)
```
To make the uId field persistable, provide a type converter
```kotlin
@TypeConverter
class AppTypeConverter {
  fun toUniqueId(data: String): ID = ID(data)
  fun toString(data: ID?): String = data?.id ?: ""
}
```
A type converter must be annotated with `@TypeConverter` and contain *non static* methods that converts between the non persistable type to a string and vice verser
Methods in a type converter should have only one parameter and must return

*Type converter is not for fields that are relations*

**There can only be one type converter in one module**

> Without a type converter provision, the compiler will not generate columns for non persistable fields


### Relations

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
To utilize the relation, the compiler will generate a class called `TodoRelationsDao` with convenient methods, a snippet of generated code below
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

> Instances of RelationDaos are retrieved from the generated database class


#### HasMany Relation
Denotes one entity has many entities of same type and the other entity has one entity of this type, a sample a post has many post comments and a post comment has one post
```java
@Entity
public class Post extends ActiveRecord<Post> {
  // other fields
	...
	@HasMany
  private List<PostComment> comments;
}
```
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
To utilize the relation, the compiler will generate a class called `PostRelationsDao` with convenient methods, a snippet of generated code below
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
  public IdentifiableList<? extends Post> listWithComments() {
    IdentifiableList<? extends Post> posts = postsTable.findAll();
    posts.forEach(new Consumer<Post>() {
      @Override
      public void accept(Post post) {
        post.setComments(new IdentifiableList<>(getComments(post)));
      }
    });
    return posts;
  }
  ...
  // more convenience methods
}
```

> Instances of RelationDaos are retrieved from the generated database class

### Initialization
The compiler generates a database class, *In this case the case the generated java file will be named `AppDatabaseIimpl`* ,

for creating your database implementation and accesing tables
and relation daos, sampled code from generated database file below


### Migrations

#### Migrations For Entities
Adding an entity to be persisted to the database from version 1 to version 2 of the database
Annotate it with AddedEntity
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

#### Migrations For Columns
The compiler generates this migrations on the fly otherwise you may still do it manually by
annotating the added field with `@Migrate` and include fromVersion and toVersion
If a field is to pass multiple migrations, annotate it with `@Migrations` and add the migrate annotations in the 
migrations annotation
```java
...
  // added new field
  @Migrate(fromVersion = 2, toVersion = 3, action = MigrationOptions.CREATE)
  private String status;
...
```

[![-----------------------------------------------------](https://raw.githubusercontent.com/andreasbm/readme/master/assets/lines/colored.png)](#setup)

## ➤ Setup
##### Project level build.gradle
Add jitpack repository
```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}

```
##### Module level build.gradle
Compile with java 8, add rxJava dependency of using rx DML functions in the tables
```gradle
android {
    ...
    defaultConfig {
        ...
        javaCompileOptions {
            annotationProcessorOptions {
                arguments = [
                        "promise.database.projectDir": "$projectDir".toString()
                ]
            }
        }
    }
    compileOptions {
            sourceCompatibility JavaVersion.VERSION_1_8
            targetCompatibility JavaVersion.VERSION_1_8
        }
}

dependencies {
     ...
     implementation 'com.github.android-promise.database:database:1.0.3'
     kapt 'com.github.android-promise.database:compiler:1.0.3'
     implementation 'com.github.android-promise:commons:1.1-alpha03'
}
```

> The projectDir specifies where object tree file will be stored

##### Initialization
Initialize Promise in your main application class
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

[![-----------------------------------------------------](https://raw.githubusercontent.com/andreasbm/readme/master/assets/lines/colored.png)](#new-features-on-the-way)

## ➤ New features on the way
watch this repo to stay updated 

[![-----------------------------------------------------](https://raw.githubusercontent.com/andreasbm/readme/master/assets/lines/colored.png)](#developed-by)

# ➤ Developed By
* Peter Vincent - [Portfolio](https://dev4vin.github.io/info)

[![-----------------------------------------------------](https://raw.githubusercontent.com/andreasbm/readme/master/assets/lines/colored.png)](#donations)

# ➤ Donations
If you'd like to support this library development, you could buy me coffee here:
* [![Become a Patreon]("https://c6.patreon.com/becomePatronButton.bundle.js")](https://www.patreon.com/bePatron?u=31932751)

#### Pull requests / Issues / Improvement requests
Feel free to contribute and ask!<br/>

[![-----------------------------------------------------](https://raw.githubusercontent.com/andreasbm/readme/master/assets/lines/colored.png)](#license)

# ➤ License

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

