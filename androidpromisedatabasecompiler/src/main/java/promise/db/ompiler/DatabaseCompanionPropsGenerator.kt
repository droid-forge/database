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

package promise.db.ompiler

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import promise.db.AddedEntity
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

class DatabaseCompanionPropsGenerator(
    private val typeSpec: TypeSpec.Builder,
    private val element: Element,
    private val processingEnv: ProcessingEnvironment) : CodeBlockGenerator<String> {

  override fun generate(): String {
    val className = element.simpleName.toString()
    val classnameImpl = "${className}_Impl"
    val pack = processingEnv.elementUtils.getPackageOf(element).toString()

    try {
      //fileSpec.addImport("android.database.sqlite", "SQLiteDatabase")
    } catch (e: Throwable) {
      processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Add Import: ${Utils.getStackTraceString(e)}")
    }

    val entities = (element as TypeElement).getTableEntities(processingEnv)

    /**
     * private static Migration getMigration() {
            return new Migration() {
              @Override
              public void onMigrate(@NotNull FastDatabase database, @NotNull SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
                if (oldVersion == 2 && newVersion == 3) {
                  database.add(sqLiteDatabase, database.obtain(PersonFastTable.class));
                }
              }
            };
          }
     */
    var migrationInitialzer = """
      return new Migration() {
              @Override
              public void onMigrate(FastDatabase database, android.database.sqlite.SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
    """.plus("\n").trimIndent()
    entities.forEach {
      val migrationAnnoTation = it.getAnnotation(AddedEntity::class.java)
      if (migrationAnnoTation != null) {
        migrationInitialzer += """
          if (oldVersion == ${migrationAnnoTation.fromVersion} && newVersion == ${migrationAnnoTation.toVersion}) {
            database.add(sqLiteDatabase, database.obtain(${it.getClassName()}.class));
          } 
          
    """.trimIndent()
      }
    }


    migrationInitialzer += """
        }
      };
    """.trimIndent()

    val migrationPropSpec = MethodSpec.methodBuilder("getMigration")
        .returns(ClassName.get("promise.db", "Migration"))
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
        .addCode(CodeBlock.of(migrationInitialzer))
        .build()

    typeSpec.addMethod(migrationPropSpec)
    // adding db instance var
    // private static volatile FastDatabase instance = null;
    typeSpec.addField(FieldSpec.builder(ClassName.get("promise.db", "FastDatabase"),"instance")
        .initializer("null")
        .addModifiers(Modifier.PRIVATE, Modifier.VOLATILE, Modifier.STATIC)
        .build())


    // @JvmStatic
    //    fun createDatabase(name: String): GeneratedDatabase {
    //      if (instance != null) throw IllegalStateException("Database already created")
    //      instance = FastDatabase.createDatabase(GeneratedDatabaseImpl::class.java,
    //          name, migration)
    //      return GeneratedDatabaseImpl()
    //    }
    typeSpec.addMethod(MethodSpec.methodBuilder("createDatabase")
        .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
        .addParameter(ClassName.get(String::class.java),"name")
        .returns(ClassName.get(pack, className))
        .addCode(CodeBlock.of("""
          if (instance != null) throw new IllegalStateException("Database already created");
          instance = FastDatabase.createDatabase($classnameImpl.class, name, getMigration());
          return new $classnameImpl();
        """.trimIndent()))
        .build())

    // add with creation callback
    typeSpec.addMethod(MethodSpec.methodBuilder("createDatabase")
        .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
        .addParameter(ClassName.get(String::class.java),"name")
        .addParameter(ClassName.get("promise.db", "DatabaseCreationCallback"),"databaseCreationCallback")
        .returns(ClassName.get(pack, className))
        .addCode(CodeBlock.of("""
          if (instance != null) throw new IllegalStateException("Database already created");
          instance = FastDatabase.createDatabase($classnameImpl.class, name, getMigration()).setDatabaseCreationCallback(databaseCreationCallback);
          return new $classnameImpl();
        """.trimIndent()))
        .build())

    //    @JvmStatic
    //    fun createInMemoryDatabase(): GeneratedDatabase {
    //      if (instance != null) throw IllegalStateException("Database already created")
    //      instance = FastDatabase.createInMemoryDatabase(GeneratedDatabaseImpl::class.java)
    //      return GeneratedDatabaseImpl()
    //    }
    typeSpec.addMethod(MethodSpec.methodBuilder("createInMemoryDatabase")
        .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
        .returns(ClassName.get(pack, className))
        .addCode(CodeBlock.of("""
          if (instance != null) throw new IllegalStateException("Database already created");
          instance = FastDatabase.createInMemoryDatabase($classnameImpl.class);
          return new $classnameImpl();
        """.trimIndent()))
        .build())
    // with in memory database
    typeSpec.addMethod(MethodSpec.methodBuilder("createInMemoryDatabase")
        .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
        .addParameter(ClassName.get("promise.db", "DatabaseCreationCallback"),"databaseCreationCallback")
        .returns(ClassName.get(pack, className))
        .addCode(CodeBlock.of("""
          if (instance != null) throw new IllegalStateException("Database already created");
          instance = FastDatabase.createInMemoryDatabase($classnameImpl.class).setDatabaseCreationCallback(databaseCreationCallback);
          return new $classnameImpl();
        """.trimIndent()))
        .build())

    //    @JvmStatic
    //    fun createReactiveInMemoryDatabase(): GeneratedDatabase {
    //      if (instance != null) throw IllegalStateException("Database already created")
    //      instance = FastDatabase.createInMemoryReactiveDatabase(GeneratedDatabaseImpl::class.java)
    //      return GeneratedDatabaseImpl()
    //    }
    typeSpec.addMethod(MethodSpec.methodBuilder("createReactiveInMemoryDatabase")
        .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
        .returns(ClassName.get(pack, className))
        .addCode(CodeBlock.of("""
          if (instance != null) throw new IllegalStateException("Database already created");
          instance = FastDatabase.createInMemoryReactiveDatabase($classnameImpl.class);
          return new $classnameImpl();
        """.trimIndent()))
        .build())
    // with callback
    typeSpec.addMethod(MethodSpec.methodBuilder("createReactiveInMemoryDatabase")
        .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
        .addParameter(ClassName.get("promise.db", "DatabaseCreationCallback"),"databaseCreationCallback")
        .returns(ClassName.get(pack, className))
        .addCode(CodeBlock.of("""
          if (instance != null) throw new IllegalStateException("Database already created");
          instance = FastDatabase.createInMemoryReactiveDatabase($classnameImpl.class, databaseCreationCallback);
          return new $classnameImpl();
        """.trimIndent()))
        .build())

    //    @JvmStatic
    //    fun createReactiveDatabase(name: String): GeneratedDatabase {
    //      if (instance != null) throw IllegalStateException("Database already created")
    //      instance = FastDatabase.createReactiveDatabase(GeneratedDatabaseImpl::class.java,
    //          name, migration)
    //      return GeneratedDatabaseImpl()
    //    }
    typeSpec.addMethod(MethodSpec.methodBuilder("createReactiveDatabase")
        .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
        .addParameter(ClassName.get(String::class.java),"name")
        .returns(ClassName.get(pack, className))
        .addCode(CodeBlock.of("""
          if (instance != null) throw new IllegalStateException("Database already created");
           instance = FastDatabase.createReactiveDatabase($classnameImpl.class, name, getMigration());
          return new $classnameImpl();
        """.trimIndent()))
        .build())

    // with creation callback
    typeSpec.addMethod(MethodSpec.methodBuilder("createReactiveDatabase")
        .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
        .addParameter(ClassName.get(String::class.java),"name")
        .addParameter(ClassName.get("promise.db", "DatabaseCreationCallback"),"databaseCreationCallback")
        .returns(ClassName.get(pack, className))
        .addCode(CodeBlock.of("""
          if (instance != null) throw new IllegalStateException("Database already created");
           instance = FastDatabase.createReactiveDatabase($classnameImpl.class, name, getMigration(), databaseCreationCallback);
          return new $classnameImpl();
        """.trimIndent()))
        .build())
    //    @JvmStatic
    //    fun getDatabaseInstance(): FastDatabase {
    //      if (instance == null) throw IllegalStateException("Database not initialized or created yet")
    //      return instance!!
    //    }
    typeSpec.addMethod(MethodSpec.methodBuilder("getDatabaseInstance")
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addAnnotation(Override::class.java)
        .returns(ClassName.get("promise.db", "FastDatabase"))
        .addCode(CodeBlock.of("""
          if (instance == null) throw new IllegalStateException("Database not initialized or created yet");
          return instance;
        """.trimIndent()))
        .build())

    return "created"
  }

}