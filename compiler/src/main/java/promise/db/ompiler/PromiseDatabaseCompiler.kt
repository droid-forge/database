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

import com.google.auto.service.AutoService
import com.squareup.javapoet.JavaFile
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType
import promise.db.DatabaseEntity
import promise.db.Entity
import promise.db.TypeConverter
import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import kotlin.collections.ArrayList

@AutoService(Processor::class) // For registering the service
@IncrementalAnnotationProcessor(IncrementalAnnotationProcessorType.DYNAMIC)
@SupportedSourceVersion(SourceVersion.RELEASE_7) // to support Java 8
@SupportedOptions(PromiseDatabaseCompiler.KAPT_JAVA_GENERATED_OPTION_NAME)
class PromiseDatabaseCompiler : AbstractProcessor() {

  override fun process(mutableSet: MutableSet<out TypeElement>?, environment: RoundEnvironment?): Boolean {
    try {
      val javaFiles: ArrayList<JavaFile.Builder> = ArrayList()
      val processors: ArrayList<ClassProcessor> = ArrayList()

      processors.add(TypeConverterProcessor(processingEnv))
      processors.add(EntityProcessor(processingEnv))
      processors.add(DatabaseEntityProcessor(processingEnv))

      processors.forEach {
        val builder = it.process(environment)
        if (builder != null) {
          javaFiles.addAll(builder.filterNotNull())
        }
      }
      javaFiles.forEach {
        val file = it
            .indent("  ")
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
                
                Code Resource https://github.com/android-promise
              
            """.trimIndent()
            )
            .build()
        file.writeTo(processingEnv.filer)
      }
      return true
    } catch (e: Throwable) {
      processingEnv.messager.printMessage(Diagnostic.Kind.ERROR,
          //"EntityProcessor: ${Utils.getStackTraceString(e)} " +
          "EntityProcessor Trace: ${Arrays.toString(e.stackTrace)}")
      return false
    }
  }

  override fun getSupportedAnnotationTypes(): MutableSet<String> = mutableSetOf(
      Entity::class.java.name,
      DatabaseEntity::class.java.name,
      TypeConverter::class.java.name
  )

  companion object {
    const val KAPT_JAVA_GENERATED_OPTION_NAME = "kapt.java.generated"
  }

}