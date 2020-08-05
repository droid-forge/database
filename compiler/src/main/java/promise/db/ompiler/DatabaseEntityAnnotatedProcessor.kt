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
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import promise.db.DatabaseEntity
import promise.db.ompiler.utils.JavaUtils
import promise.db.ompiler.utils.LogUtil
import promise.db.ompiler.utils.asTableClassName
import promise.db.ompiler.utils.getTableClassNameString
import promise.db.ompiler.utils.getTableEntities
import java.util.*
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

class DatabaseEntityAnnotatedProcessor(private val processingEnv: ProcessingEnvironment) : AnnotatedClassProcessor() {

  override fun process(environment: RoundEnvironment?): List<JavaFile.Builder?>? {
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
      return Collections.singletonList(processElement(element))
    }
    return null
  }

  private fun processElement(element: Element): JavaFile.Builder {
    processingEnv.messager.printMessage(Diagnostic.Kind.OTHER, "DatabaseProcessor Processing: ${element.simpleName}")

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
      MethodSpec.methodBuilder("get"+ it.key.simpleName())
          .addModifiers(Modifier.PUBLIC)
          .returns(it.key)
          .addCode(JavaUtils.generateGetRelationDaoCodeBlock(it).build())
          .build()
    })

    classBuilder.superclass(ClassName.get(pack, className))
        .addAnnotation(databaseAnnotationSpec)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addMethod(constructorMethod.build())

    (element as TypeElement).getTableEntities(processingEnv).forEach {
      classBuilder.addMethod(MethodSpec.methodBuilder("get"+it.getTableClassNameString())
          .addModifiers(Modifier.PUBLIC)
          .returns(it.asTableClassName(processingEnv))
          .addCode(JavaUtils.generateGetTableStatement(processingEnv, it))
          .build())
    }

    DatabaseStaticMethodsGenerator(classBuilder, element, processingEnv).generate()

    val abstractFuncsBuilder = DatabaseAbstractMethodsGenerator((element as TypeElement), processingEnv)

    abstractFuncsBuilder.generate()?.forEach {
      classBuilder.addMethod(it)
    }

    DatabaseCrudStubMethodsGenerator(classBuilder, element, processingEnv).generate()

    return JavaFile.builder(pack, classBuilder.build())

  }
}