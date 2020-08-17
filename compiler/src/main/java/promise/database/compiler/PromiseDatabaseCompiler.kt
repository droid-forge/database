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

package promise.database.compiler

import com.google.auto.service.AutoService
import com.squareup.javapoet.JavaFile
import promise.database.DAO
import promise.database.DatabaseEntity
import promise.database.Entity
import promise.database.Relation
import promise.database.TypeConverter
import promise.database.compiler.utils.JavaUtils
import promise.database.compiler.utils.LogUtil
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(PromiseDatabaseCompiler.KAPT_JAVA_GENERATED_OPTION_NAME)
class PromiseDatabaseCompiler : AbstractProcessor() {

  override fun init(processingEnv: ProcessingEnvironment?) {
    super.init(processingEnv)
    LogUtil.initLogger(processingEnv)
  }

  private fun getDatabaseElement(environment: RoundEnvironment?, processingEnv: ProcessingEnvironment): TypeElement? {
    val databases = environment?.getElementsAnnotatedWith(DatabaseEntity::class.java) ?: emptySet()
    if (databases.size > 1)
      LogUtil.e(Exception("There can only be one database in the module"))
    else if (databases.size == 1) {
      val element = databases.first()
      if (element.kind != ElementKind.CLASS) {
        LogUtil.e(Exception("Only classes can be annotated"), element)
        return null
      }
      if (element.kind == ElementKind.CLASS && !(element as TypeElement).modifiers.contains(Modifier.ABSTRACT))
        LogUtil.e(Exception("Database class must be abstract"), element)
      val promiseDatabaseType = JavaUtils.getDeclaredType(processingEnv,
          processingEnv.elementUtils.getTypeElement("promise.db.PromiseDatabase"))
      if (!JavaUtils.isSubTypeOfDeclaredType(processingEnv, element, promiseDatabaseType))
        LogUtil.e(Exception("Database class must extend from promise.db.PromiseDatabase"), element)
      return element as TypeElement
    }
    return null
  }

  override fun process(mutableSet: MutableSet<out TypeElement>?, environment: RoundEnvironment?): Boolean {
    if (mutableSet == null || mutableSet.isEmpty()) return false
    try {
      val database = getDatabaseElement(environment, processingEnv)
      if (database != null) {
        var schelocation = ""
        val options = processingEnv?.options ?: emptyMap()
        if (options.containsKey("promise.database.projectDir")) schelocation = options["promise.database.projectDir"]!!
        else LogUtil.e(Exception("you must provide schema location as argument"))
        val finalLocation = "${schelocation}${File.separator}schemas${File.separator}${database.asType()}"
        //LogUtil.e(Exception("Schema loc $finalLocation"))
        databaseMetaDataWriter = DatabaseMetaDataWriter(database, finalLocation)
      } else LogUtil.e(Exception("No database class found in module"))
      val javaFiles: ArrayList<JavaFile.Builder> = ArrayList()
      val processors: ArrayList<AnnotatedClassProcessor> = ArrayList()
      processors.add(TypeConverterAnnotatedProcessor(processingEnv))
      processors.add(EntityAnnotatedProcessor(processingEnv, database!!))
      processors.add(RelationsDaoProcessor(processingEnv))
      processors.add(DAOAnnotatedProcessor(processingEnv))
      processors.add(DatabaseEntityAnnotatedProcessor(processingEnv, database))
      processors.forEach {
        val builders = it.process(environment)
        if (builders != null) javaFiles.addAll(builders.filterNotNull())
      }
      javaFiles.forEach {
        it
            .indent("\t")
            .skipJavaLangImports(true)
            .addFileComment(
                """
                Copyright 2017, Android Promise Database
                Licensed under the Apache License, Version 2.0, Android Promise.
                you may not use this file except in compliance with the License.
                You may obtain a copy of the License at
                http://www.apache.org/licenses/LICENSE-2.0
                Unless required by applicable law or agreed to in writing,
                software distributed under the License is distributed on an AS IS BASIS,
                WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied
                See the License for the specific language governing permissions and
                limitations under the License
                
                Resource https://github.com/android-promise/database
                
                Generated by Android Promise Database Compiler, do not modify
              
            """.trimIndent()
            )
            .build()
            .writeTo(processingEnv.filer)
      }
      return true
    } catch (e: Throwable) {
      LogUtil.e(e)
      return false
    }
  }

  override fun getSupportedAnnotationTypes(): MutableSet<String> = mutableSetOf(
      Entity::class.java.name,
      DatabaseEntity::class.java.name,
      TypeConverter::class.java.name,
      DAO::class.java.name
  )

  companion object {
    const val KAPT_JAVA_GENERATED_OPTION_NAME = "kapt.java.generated"
    lateinit var databaseMetaDataWriter: DatabaseMetaDataWriter
  }

}