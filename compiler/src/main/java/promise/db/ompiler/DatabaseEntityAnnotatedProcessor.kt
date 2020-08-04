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
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

class DatabaseEntityAnnotatedProcessor(private val processingEnv: ProcessingEnvironment) : AnnotatedClassProcessor() {

  override fun process(environment: RoundEnvironment?): List<JavaFile.Builder?>? {
    return environment?.getElementsAnnotatedWith(DatabaseEntity::class.java)
        ?.map { element ->
          if (element.kind != ElementKind.CLASS) {
            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Only classes can be annotated")
            return null
          }
          if (element.kind == ElementKind.CLASS && !(element as TypeElement).modifiers.contains(Modifier.ABSTRACT)) {
            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Database class must be abstract")
          }
          val promiseDatabaseType = JavaUtils.getDeclaredType(processingEnv,
              processingEnv.elementUtils.getTypeElement("promise.db.PromiseDatabase"))
          if (!JavaUtils.isSubTypeOfDeclaredType(processingEnv, element, promiseDatabaseType)) {
            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Database class must extend from promise.db.PromiseDatabase")
          }
          processElement(element)
        }
  }

  private fun processElement(element: Element): JavaFile.Builder {
    processingEnv.messager.printMessage(Diagnostic.Kind.OTHER, "DatabaseProcessor Processing: ${element.simpleName}")

    val className = element.simpleName.toString()
    val pack = processingEnv.elementUtils.getPackageOf(element).toString()

    val fileName = "${className}_Impl"

    val tableAnnotationSpec = DatabaseAnnotationGenerator(processingEnv, element).generate()

    val classBuilder = TypeSpec.classBuilder(fileName)

    val constructorMethod = MethodSpec.constructorBuilder()
        .addParameter(ClassName.get("promise.db", "FastDatabase"),
            "fastDatabase")
        .addModifiers(Modifier.PRIVATE)
        .addStatement("super()")
        .addStatement("this.instance = fastDatabase")
    if (TypeConverterAnnotatedProcessor.typeConverter != null) {
      constructorMethod.addJavadoc("""
        Initializes the TypeConverter as singleton
      """.trimIndent())
      JavaUtils.generateAddConverterStatementInDatabaseConstructorMethod(processingEnv, constructorMethod, TypeConverterAnnotatedProcessor.typeConverter!!)
    }
    classBuilder.superclass(ClassName.get(pack, className))
        .addAnnotation(tableAnnotationSpec)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addMethod(constructorMethod.build())

    DatabaseStaticMethodsGenerator(classBuilder, element, processingEnv).generate()

    val abstractFuncsBuilder = DatabaseAbstractMethodsGenerator((element as TypeElement), processingEnv)

    abstractFuncsBuilder.generate()?.forEach {
      classBuilder.addMethod(it)
    }

    DatabaseCrudStubMethodsGenerator(classBuilder, element, processingEnv).generate()

    return JavaFile.builder(pack, classBuilder.build())

  }
}