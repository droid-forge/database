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

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import org.jetbrains.annotations.NotNull
import promise.db.Entity
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

class EntityProcessor(private val processingEnv: ProcessingEnvironment) : ClassProcessor() {
  override fun process(environment: RoundEnvironment?): List<JavaFile.Builder?>? {
    return environment?.getElementsAnnotatedWith(Entity::class.java)
        ?.map { element ->
          if (element.kind != ElementKind.CLASS) {
            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Only classes can be annotated")
            return null
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
  }


  private fun processAnnotation(element: Element): JavaFile.Builder {
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

    val activeRecordType = processingEnv.typeUtils.erasure(
        processingEnv.elementUtils.getTypeElement("promise.db.ActiveRecord").asType())

    if (JavaUtils.isSubtypeOfType((element as TypeElement).asType(), TypeName.get(activeRecordType)))
      classBuilder.addMethod(MethodSpec.methodBuilder("createEntityInstance")
          .addAnnotation(Override::class.java)
          .addAnnotation(NotNull::class.java)
          .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
          .returns(ClassName.get(pack, className))
          .addCode("return new $className();")
          .build())

    // static column block generation
    val tableColumnPropsGenerator = TableColumnFieldsGenerator(processingEnv, element.enclosedElements)

    if (element.checkIfAnyElementNeedsTypeConverter()) {
      classBuilder.addField(FieldSpec.builder(TypeName.get(TypeConverterProcessor
          .typeConverter!!.asType()), "typeConverter")
          .addModifiers(Modifier.PRIVATE)
          .build())
      classBuilder.addMethod(MethodSpec.methodBuilder("setTypeConverter")
          .addModifiers(Modifier.PUBLIC)
          .addParameter(TypeName.get(TypeConverterProcessor
              .typeConverter!!.asType()), "typeConverter")
          .addStatement("this.typeConverter = typeConverter")
          .build())
    }

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
    val tagSpec = JavaUtils.generateEntityTableLogField(element)

    classBuilder.addField(tagSpec)

    val columnSpecs = tableColumnPropsGenerator.generate()

    columnSpecs.values.forEach {
      classBuilder.addField(it)
    }

    // column register generation
    val columnRegSpecGenerator = TableRegisteredColumnsMethodGenerator(tableColumnPropsGenerator.genColValues.map { it.second })
    classBuilder.addMethod(columnRegSpecGenerator.generate())

    // serializer generator
    val serializerGenerator = TableSerializerMethodGenerator(processingEnv, pack, className, tableColumnPropsGenerator.genColValues)
    classBuilder.addMethod(serializerGenerator.generate())

    // deserializer generator
    val deserializerGenerator = TableDeserializerMethodGenerator(processingEnv, pack, className,
        tableColumnPropsGenerator.genColValues)
    classBuilder.addMethod(deserializerGenerator.generate())

    // migrations
    val elemMap = HashMap<Element, String>()
    columnSpecs.forEach {
      elemMap[it.key.first] = it.key.second
    }
    val migrationGenerator = TableMigrationFieldGenerator(elemMap)
    val migrationFunc = migrationGenerator.generate()
    if (migrationFunc != null) classBuilder.addMethod(migrationFunc)

    return JavaFile.builder(pack, classBuilder.build())
  }


}