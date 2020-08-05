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
import promise.db.ompiler.utils.JavaUtils
import promise.db.ompiler.utils.Utils
import promise.db.ompiler.utils.getTableClassNameString
import promise.db.ompiler.utils.getTableEntities
import promise.db.ompiler.utils.toTypeName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

class DatabaseStaticMethodsGenerator(
    private val typeSpec: TypeSpec.Builder,
    private val element: Element,
    private val processingEnv: ProcessingEnvironment) : CodeGenerator<String> {

  override fun generate(): String {
    val className = element.simpleName.toString()
    val classnameImpl = "${className}Impl"
    val pack = processingEnv.elementUtils.getPackageOf(element).toString()

    val entities = (element as TypeElement).getTableEntities(processingEnv)
        .filter {
          it.getAnnotation(AddedEntity::class.java) != null
        }
    val migrationCodeBlock = CodeBlock.builder()
    if (entities.isEmpty()) migrationCodeBlock.addStatement("return null")
    else {
      migrationCodeBlock.beginControlFlow("return new Migration()")
      JavaUtils.generateDatabaseMigrationOverrideControlBlock(migrationCodeBlock)
      entities.forEach {
        val migrationAnnotation = it.getAnnotation(AddedEntity::class.java)
        migrationCodeBlock.beginControlFlow("if (oldVersion == ${migrationAnnotation.fromVersion} && newVersion == ${migrationAnnotation.toVersion})")
        migrationCodeBlock.addStatement("database.add(sqLiteDatabase, database.obtain(${it.getTableClassNameString()}.class))")
        migrationCodeBlock.endControlFlow()
      }
      migrationCodeBlock.endControlFlow()
      migrationCodeBlock.add("};")
    }

    val migrationPropSpec = MethodSpec.methodBuilder("getMigration")
        .returns(ClassName.get("promise.db", "Migration"))
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
        .addCode(migrationCodeBlock.build())
        .addJavadoc("""
          Migration callback for adding tables into existing database
        """.trimIndent())
        .build()

    typeSpec.addMethod(migrationPropSpec)

    if (TypeConverterAnnotatedProcessor.typeConverter != null) {
      typeSpec.addField(FieldSpec.builder(
          TypeConverterAnnotatedProcessor.typeConverter!!.toTypeName(),
          "typeConverter")
          .addModifiers(Modifier.PRIVATE)
          .addJavadoc("""
            TypeConverter instance
          """.trimIndent())
          .build())
    }

    typeSpec.addField(FieldSpec.builder(Boolean::class.javaPrimitiveType, "initialized")
        .initializer("false")
        .addJavadoc("""
          Check to ensure database instance is not instantiated more than once
        """.trimIndent())
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
        .addParameter(ClassName.get(String::class.java), "name")
        .returns(ClassName.get(pack, classnameImpl))
        .addJavadoc("""
          Creates the simplest database with name specified
          @Param name the name of the database
        """.trimIndent())
        .addCode(CodeBlock.of("""
          if (initialized) throw new IllegalStateException("Database already created");
          initialized = true;
          return new $classnameImpl(FastDatabase.createDatabase($classnameImpl.class, name, getMigration()));
        """.trimIndent()))
        .build())

    // add with creation callback
    typeSpec.addMethod(MethodSpec.methodBuilder("createDatabase")
        .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
        .addParameter(ClassName.get(String::class.java), "name")
        .addParameter(ClassName.get("promise.db", "DatabaseCreationCallback"), "databaseCreationCallback")
        .returns(ClassName.get(pack, classnameImpl))
        .addJavadoc("""
          Creates the simplest database with name specified with callback
          Callback can be used to pre populate database with records or
          set flags like foreign keys
          @Param name the name of the database
          @Param databaseCreationCallback callback
        """.trimIndent())
        .addCode(CodeBlock.of("""
          if (initialized) throw new IllegalStateException("Database already created");
          initialized = true;
          return new $classnameImpl(FastDatabase.createDatabase($classnameImpl.class, name, getMigration(), databaseCreationCallback));
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
        .returns(ClassName.get(pack, classnameImpl))
        .addJavadoc("""
          Creates an in memory database, useful for tests
        """.trimIndent())
        .addCode(CodeBlock.of("""
          if (initialized) throw new IllegalStateException("Database already created");
          initialized = true;
          return new $classnameImpl(FastDatabase.createInMemoryDatabase($classnameImpl.class));
        """.trimIndent()))
        .build())
    // with in memory database
    typeSpec.addMethod(MethodSpec.methodBuilder("createInMemoryDatabase")
        .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
        .addParameter(ClassName.get("promise.db", "DatabaseCreationCallback"), "databaseCreationCallback")
        .returns(ClassName.get(pack, classnameImpl))
        .addJavadoc("""
          Creates an in memory database, useful for tests
          Callback can be used to pre populate database with records or
          set flags like foreign keys
          @Param databaseCreationCallback callback
        """.trimIndent())
        .addCode(CodeBlock.of("""
          if (initialized) throw new IllegalStateException("Database already created");
          initialized = true;
          return new $classnameImpl(FastDatabase.createInMemoryDatabase($classnameImpl.class, databaseCreationCallback));
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
        .returns(ClassName.get(pack, classnameImpl))
        .addJavadoc("""
          Creates an in memory database, enables calling rx DML functions in the tables
        """.trimIndent())
        .addCode(CodeBlock.of("""
          if (initialized) throw new IllegalStateException("Database already created");
          initialized = true;
          return new $classnameImpl(FastDatabase.createInMemoryReactiveDatabase($classnameImpl.class));
        """.trimIndent()))
        .build())
    // with callback
    typeSpec.addMethod(MethodSpec.methodBuilder("createReactiveInMemoryDatabase")
        .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
        .addParameter(ClassName.get("promise.db", "DatabaseCreationCallback"), "databaseCreationCallback")
        .returns(ClassName.get(pack, classnameImpl))
        .addJavadoc("""
          Creates an in memory database, enables calling rx DML functions in the tables
          Callback can be used to pre populate database with records or
          set flags like foreign keys
          @Param databaseCreationCallback callback
        """.trimIndent())
        .addCode(CodeBlock.of("""
          if (initialized) throw new IllegalStateException("Database already created");
          initialized = true;
          return new $classnameImpl(FastDatabase.createInMemoryReactiveDatabase($classnameImpl.class, databaseCreationCallback));
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
        .addParameter(ClassName.get(String::class.java), "name")
        .returns(ClassName.get(pack, classnameImpl))
        .addJavadoc("""
          Creates database, that enables calling rx DML functions in the tables
          @Param name name of the database 
        """.trimIndent())
        .addCode(CodeBlock.of("""
          if (initialized) throw new IllegalStateException("Database already created");
           initialized = true;
          return new $classnameImpl(FastDatabase.createReactiveDatabase($classnameImpl.class, name, getMigration()));
        """.trimIndent()))
        .build())

    // with creation callback
    typeSpec.addMethod(MethodSpec.methodBuilder("createReactiveDatabase")
        .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
        .addParameter(ClassName.get(String::class.java), "name")
        .addParameter(ClassName.get("promise.db", "DatabaseCreationCallback"), "databaseCreationCallback")
        .returns(ClassName.get(pack, classnameImpl))
        .addJavadoc("""
          Creates database, that enables calling rx DML functions in the tables
          Callback can be used to pre populate database with records or
          set flags like foreign keys
          @Param name name of the database 
          @Param databaseCreationCallback callback
        """.trimIndent())
        .addCode(CodeBlock.of("""
          if (initialized) throw new IllegalStateException("Database already created");
           initialized = true;
          return new $classnameImpl(FastDatabase.createReactiveDatabase($classnameImpl.class, name, getMigration(), databaseCreationCallback));
        """.trimIndent()))
        .build())

    return "created"
  }

}