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

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import promise.database.compiler.utils.JavaUtils
import promise.database.compiler.utils.asTableClassName
import promise.database.compiler.utils.getTableClassNameString
import promise.database.compiler.utils.getTableEntities
import java.util.*
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

class DatabaseEntityAnnotatedProcessor(private val processingEnv: ProcessingEnvironment,
                                       private val database: TypeElement) : AnnotatedClassProcessor() {

  override fun process(environment: RoundEnvironment?): List<JavaFile.Builder?>? =
      Collections.singletonList(processElement(database))

  private fun processElement(element: Element): JavaFile.Builder {
    val className = element.simpleName.toString()
    val pack = processingEnv.elementUtils.getPackageOf(element).toString()

    val fileName = "${className}Impl"

    val databaseAnnotationSpec = DatabaseAnnotationGenerator(processingEnv, element).generate()

    val classBuilder = TypeSpec.classBuilder(fileName)

    val constructorMethod = MethodSpec.constructorBuilder()
        .addParameter(ClassName.get("promise.db", "FastDatabase"),
            "fastDatabase")
        .addModifiers(Modifier.PRIVATE)
        .addStatement("super(fastDatabase)")
    if (TypeConverterAnnotatedProcessor.typeConverter != null)
      JavaUtils.generateAddConverterStatementInDatabaseConstructorMethod(processingEnv, constructorMethod, TypeConverterAnnotatedProcessor.typeConverter!!)

    if (RelationsDaoGenerator.relationsMap.isNotEmpty())
      classBuilder.addMethods(RelationsDaoGenerator.relationsMap.map {
        MethodSpec.methodBuilder("get" + it.key.simpleName())
            .addModifiers(Modifier.PUBLIC)
            .returns(it.key)
            .addCode(JavaUtils.generateGetRelationDaoCodeBlock(it).build())
            .build()
      })

    val supressWarningsSpec = AnnotationSpec.builder(SuppressWarnings::class.java)
        .addMember("value", "{\"unchecked\"}")

    classBuilder.superclass(ClassName.get(pack, className))
        .addAnnotation(databaseAnnotationSpec)
        .addAnnotation(supressWarningsSpec.build())
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addMethod(constructorMethod.build())

    (element as TypeElement).getTableEntities(processingEnv).forEach {
      classBuilder.addMethod(MethodSpec.methodBuilder("get" + it.getTableClassNameString())
          .addModifiers(Modifier.PUBLIC)
          .returns(it.asTableClassName(processingEnv))
          .addCode(JavaUtils.generateGetTableStatement(processingEnv, it))
          .addAnnotation(supressWarningsSpec.build())
          .build())
    }

    DatabaseStaticMethodsGenerator(classBuilder, element, processingEnv).generate()

    val abstractFuncsBuilder = DatabaseAbstractMethodsGenerator(element, processingEnv)

    abstractFuncsBuilder.generate()?.forEach {
      classBuilder.addMethod(it)
    }

    DatabaseCrudStubMethodsGenerator(classBuilder, element, processingEnv).generate()

    return JavaFile.builder(pack, classBuilder.build())

  }
}