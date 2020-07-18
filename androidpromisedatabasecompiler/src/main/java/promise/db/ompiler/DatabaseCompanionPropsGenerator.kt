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

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.jvm.jvmStatic
import com.squareup.kotlinpoet.jvm.volatile
import promise.db.AddedEntity
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

class DatabaseCompanionPropsGenerator(
    private val fileSpec: FileSpec.Builder,
    private val element: Element,
    private val processingEnv: ProcessingEnvironment) : CodeBlockGenerator<TypeSpec> {


  override fun generate(): TypeSpec {
    val className = element.simpleName.toString()
    val classnameImpl = "${className}_Impl"
    val pack = processingEnv.elementUtils.getPackageOf(element).toString()
    val typeSpec = TypeSpec.companionObjectBuilder()

    try {
      fileSpec.addImport("android.database.sqlite", "SQLiteDatabase")
    } catch (e: Throwable) {
      processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Add Import: ${Utils.getStackTraceString(e)}")
    }

    val entities = (element as TypeElement).getTableEntities(processingEnv)

    var migrationInitialzer = """
      return object: Migration {
        override fun onMigrate(database: FastDatabase, sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    """.plus("\n").trimIndent()
    entities.forEach {
      val migrationAnnoTation = it.getAnnotation(AddedEntity::class.java)
      if (migrationAnnoTation != null) {
        migrationInitialzer += """
          if (oldVersion == ${migrationAnnoTation.fromVersion} && newVersion == ${migrationAnnoTation.toVersion}) {
            database.add(sqLiteDatabase, database.obtain(${it.getClassName()}::class.java))
          } 
          
    """.trimIndent()
      }
    }


    migrationInitialzer += """
        }
      }
    """.trimIndent()

    val migrationPropSpec = FunSpec.builder("getMigration")
        .returns(ClassName("promise.db", "Migration"))
        .addModifiers(KModifier.PRIVATE)
        .addCode(CodeBlock.of(migrationInitialzer))
        .build()

    typeSpec.addFunction(migrationPropSpec)
    // adding db instance var
    // var instance: FastDatabase? = null
    typeSpec.addProperty(PropertySpec.builder("instance", ClassName("promise.db", "FastDatabase").copy(nullable = true))
        .initializer("null")
        .mutable(true)
        .jvmStatic()
        .volatile()
        .build())


    // @JvmStatic
    //    fun createDatabase(name: String): GeneratedDatabase {
    //      if (instance != null) throw IllegalStateException("Database already created")
    //      instance = FastDatabase.createDatabase(GeneratedDatabaseImpl::class.java,
    //          name, migration)
    //      return GeneratedDatabaseImpl()
    //    }
    typeSpec.addFunction(FunSpec.builder("createDatabase")
        .addAnnotation(JvmStatic::class.java)
        .addParameter("name", STRING)
        .returns(ClassName(pack, className))
        .addCode(CodeBlock.of("""
          if (instance != null) throw IllegalStateException("Database already created")
          instance = FastDatabase.createDatabase($classnameImpl::class.java, name, getMigration())
          return $classnameImpl()
        """.trimIndent()))
        .build())

    //    @JvmStatic
    //    fun createInMemoryDatabase(): GeneratedDatabase {
    //      if (instance != null) throw IllegalStateException("Database already created")
    //      instance = FastDatabase.createInMemoryDatabase(GeneratedDatabaseImpl::class.java)
    //      return GeneratedDatabaseImpl()
    //    }
    typeSpec.addFunction(FunSpec.builder("createInMemoryDatabase")
        .addAnnotation(JvmStatic::class.java)
        .returns(ClassName(pack, className))
        .addCode(CodeBlock.of("""
          if (instance != null) throw IllegalStateException("Database already created")
          instance = FastDatabase.createInMemoryDatabase($classnameImpl::class.java)
          return $classnameImpl()
        """.trimIndent()))
        .build())

    //    @JvmStatic
    //    fun createReactiveInMemoryDatabase(): GeneratedDatabase {
    //      if (instance != null) throw IllegalStateException("Database already created")
    //      instance = FastDatabase.createInMemoryReactiveDatabase(GeneratedDatabaseImpl::class.java)
    //      return GeneratedDatabaseImpl()
    //    }
    typeSpec.addFunction(FunSpec.builder("createReactiveInMemoryDatabase")
        .addAnnotation(JvmStatic::class.java)
        .returns(ClassName(pack, className))
        .addCode(CodeBlock.of("""
          if (instance != null) throw IllegalStateException("Database already created")
          instance = FastDatabase.createInMemoryReactiveDatabase($classnameImpl::class.java)
          return $classnameImpl()
        """.trimIndent()))
        .build())

    //    @JvmStatic
    //    fun createReactiveDatabase(name: String): GeneratedDatabase {
    //      if (instance != null) throw IllegalStateException("Database already created")
    //      instance = FastDatabase.createReactiveDatabase(GeneratedDatabaseImpl::class.java,
    //          name, migration)
    //      return GeneratedDatabaseImpl()
    //    }
    typeSpec.addFunction(FunSpec.builder("createReactiveDatabase")
        .addAnnotation(JvmStatic::class.java)
        .addParameter("name", STRING)
        .returns(ClassName(pack, className))
        .addCode(CodeBlock.of("""
          if (instance != null) throw IllegalStateException("Database already created")
           instance = FastDatabase.createReactiveDatabase($classnameImpl::class.java, name, getMigration())
          return $classnameImpl()
        """.trimIndent()))
        .build())
    //    @JvmStatic
    //    fun getDatabaseInstance(): FastDatabase {
    //      if (instance == null) throw IllegalStateException("Database not initialized or created yet")
    //      return instance!!
    //    }
    typeSpec.addFunction(FunSpec.builder("getDatabaseInstance")
        .addAnnotation(JvmStatic::class.java)
        .returns(ClassName("promise.db", "FastDatabase"))
        .addCode(CodeBlock.of("""
          if (instance == null) throw IllegalStateException("Database not initialized or created yet")
          return instance!!
        """.trimIndent()))
        .build())

    return typeSpec.build()
  }


}