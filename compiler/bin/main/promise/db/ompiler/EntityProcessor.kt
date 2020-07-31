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
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType
import org.jetbrains.annotations.NotNull
import promise.db.DatabaseEntity
import promise.db.Entity
import promise.db.ompiler.annotation.TableAnnotationGenerator
import promise.db.ompiler.migration.MigrationGenerator
import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic


@AutoService(Processor::class) // For registering the service
@IncrementalAnnotationProcessor(IncrementalAnnotationProcessorType.DYNAMIC)
@SupportedSourceVersion(SourceVersion.RELEASE_7) // to support Java 8
@SupportedOptions(EntityProcessor.KAPT_JAVA_GENERATED_OPTION_NAME)
class EntityProcessor : AbstractProcessor() {

  override fun init(processingEnv: ProcessingEnvironment?) {
    super.init(processingEnv)
  }

  override fun process(mutableSet: MutableSet<out TypeElement>?, environment: RoundEnvironment?): Boolean {
    try {
      environment?.getElementsAnnotatedWith(Entity::class.java)
          ?.forEach { element ->
            if (element.kind != ElementKind.CLASS) {
              processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Only classes can be annotated")
              return false
            }

//            val identifiableInterface = processingEnv.elementUtils.getTypeElement("promise.commons.model.Identifiable")
//            processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "Entity class ${element.simpleName}, interfaceType ${identifiableInterface.toString()}")
//            if (JavaUtils.implementsInterface(processingEnv, element as TypeElement, identifiableInterface.asType())) {
//              processAnnotation(element)
//            }
             processAnnotation(element)
//            else {
//              processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "The Entity class ${element.simpleName} must implement Identifiable")
//              return false
//            }
          }
      //return true
      return DatabaseProcessor(processingEnv).process(mutableSet, environment)
    } catch (e: Throwable) {
      //processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "EntityProcessor: ${Utils.getStackTraceString(e)}")
      processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "EntityProcessor Trace: ${Arrays.toString(e.stackTrace)}")
      return false
    }
  }

  private fun processAnnotation(element: Element) {
    processingEnv.messager.printMessage(Diagnostic.Kind.OTHER, "Entity Processing: ${element.simpleName}")

    val className = element.simpleName.toString()
    val pack = processingEnv.elementUtils.getPackageOf(element).toString()

    val fileName = element.getClassName()

    val tableAnnotationSpec = TableAnnotationGenerator(element, processingEnv).generate()

    val classBuilder = TypeSpec.classBuilder(fileName)
        .addModifiers(Modifier.PUBLIC)
        .superclass(ParameterizedTypeName.get(
            ClassName.get("promise.db", "FastTable"),
            ClassName.get(pack, className))
        )
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(
                ClassName.get("promise.db", "FastDatabase")
                    .annotated(AnnotationSpec.builder(NotNull::class.java).build()),
                "database")
            .addStatement("super(database)")
            .build())

    classBuilder.addAnnotation(tableAnnotationSpec)

    // static column block generation
    val tableColumnPropsGenerator = TableColumnPropsGenerator(classBuilder, processingEnv, element.enclosedElements)

    val idColumnSpec = FieldSpec.builder(
        ParameterizedTypeName.get(
            ClassName.get("promise.db", "Column"),
            ClassName.get(Integer::class.java)
        ), "idColumn")
        .addModifiers(Modifier.STATIC, Modifier.PUBLIC, Modifier.FINAL)
        .initializer(CodeBlock.of("FastTable.getId()")).build()

    classBuilder.addField(idColumnSpec)
    /**
     * private static final String TAG = LogUtil.makeTag(PersonFastTable.class);
     */
    val tagSpec = FieldSpec.builder(
        String::class.java, "TAG")
        .addModifiers(Modifier.STATIC, Modifier.PRIVATE, Modifier.FINAL)
        .initializer("promise.commons.data.log.LogUtil.makeTag(${element.getClassName()}.class)")
        .build()

    classBuilder.addField(tagSpec)

    val columnSpecs = tableColumnPropsGenerator.generate()

    columnSpecs.values.forEach {
      classBuilder.addField(it)
    }

    // column register generation
    val columnRegSpecGenerator = RegisterColumnsGenerator(tableColumnPropsGenerator.genColValues.map { it.second })
    classBuilder.addMethod(columnRegSpecGenerator.generate())

    // serializer generator
    val serializerGenerator = SerializerGenerator(pack, className, tableColumnPropsGenerator.genColValues)
    classBuilder.addMethod(serializerGenerator.generate())

    // deserializer generator
    val deserializerGenerator = DeserializerGenerator(pack, className,
        tableColumnPropsGenerator.genColValues)
    classBuilder.addMethod(deserializerGenerator.generate())

    // migrations
    val elemMap = HashMap<Element, String>()
    columnSpecs.forEach {
      elemMap[it.key.first] = it.key.second
    }
    val migrationGenerator = MigrationGenerator(elemMap)
    val migrationFunc = migrationGenerator.generate()
    if (migrationFunc != null) classBuilder.addMethod(migrationFunc)

    val fileBuilder = JavaFile.builder(pack, classBuilder.build())
    //val kaptKotlinGeneratedDir = processingEnv.options[KAPT_JAVA_GENERATED_OPTION_NAME]!!

    fileBuilder
        .indent("    ")
        .skipJavaLangImports(true)
        .addFileComment(
            """
            Copyright 2017, Peter Vincent
            Licensed under the Apache License, Version 2.0, Android Promise.
            you may not use this file except in compliance with the License.
            You may obtain a copy of the License at
            http://www.apache.org/licenses/LICENSE-2.0
            Unless required by applicable law or agreed to in writing,
            software distributed under the License is distributed on an AS IS BASIS,
            WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied
            See the License for the specific language governing permissions and
            limitations under the License
            """.trimIndent()
            )
        .build()
        .writeTo(processingEnv.filer)
  }

  override fun getSupportedAnnotationTypes(): MutableSet<String> = mutableSetOf(
      Entity::class.java.name,
      DatabaseEntity::class.java.name
  )

  companion object {
    const val KAPT_JAVA_GENERATED_OPTION_NAME = "kapt.java.generated"
  }

}