# Android Promise Database [![](https://jitpack.io/v/android-promise/database.svg)](https://jitpack.io/#android-promise/database)
 Manage SqLite databases in android with ease
# Setup
#### build.gradle
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

# Initialization
Initialize Promise in your main application file, entry point

#### App.java
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
#### Stay Updated

# Wiki!
Go through the **[Wiki](https://github.com/android-promise/database/wiki) pages**,to learn how to set up your database<br/>

## Developed By

* Peter Vincent - <dev4vin@gmail.com>

## Donations
If you'd like to support this library development, you could buy me coffee here:

* [![Become a Patreon]("https://c6.patreon.com/becomePatronButton.bundle.js")](https://www.patreon.com/bePatron?u=31165349)

Thank you very much in advance!

### Pull requests / Issues / Improvement requests
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

