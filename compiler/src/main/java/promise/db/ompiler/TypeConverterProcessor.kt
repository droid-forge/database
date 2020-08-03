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
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import promise.db.TypeConverter
import java.util.*
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

fun TypeElement.getTypeConverterClassName(): String {
  val className = this.simpleName.toString()
  return "${className}InstanceProvider"
}

fun TypeElement.getVariableName(): String {
  val className = this.simpleName.toString()
  return className.camelCase();
}

class TypeConverterProcessor(private val processingEnv: ProcessingEnvironment) : ClassProcessor() {

  override fun process(environment: RoundEnvironment?): List<JavaFile.Builder?>? {
    val typeConverters = environment?.getElementsAnnotatedWith(TypeConverter::class.java)
    if (typeConverters != null) {
      if (typeConverters.size > 1) processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "There can only be one typeConverter in the module")
      else try {
        typeConverter = typeConverters.firstOrNull() as TypeElement
        return Collections.singletonList(processElement(typeConverter!!))
      } catch (e: Throwable) {

      }
    }
    return null
  }

  private fun processElement(element: TypeElement): JavaFile.Builder {
    val className = element.simpleName.toString()
    val pack = processingEnv.elementUtils.getPackageOf(element).toString()

    val fileName = element.getTypeConverterClassName()

    val varName = element.getVariableName()

    val classBuilder = TypeSpec.classBuilder(fileName)
        .addJavadoc("""
          Class holder for instantiation of TypeConverter
        """.trimIndent())
        .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
        .addSuperinterface(ParameterizedTypeName.get(
            ClassName.get("promise.commons", "InstanceProvider"),
            ClassName.get(pack, className)
        ))
        // field instance for the holder
        .addField(FieldSpec.builder(
            ClassName.get(pack, fileName),
            "instance"
        ).addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .build())
        // holder for type converter
        .addField(FieldSpec.builder(element.toTypeName(), varName,
            Modifier.FINAL, Modifier.PRIVATE)
            .addJavadoc("""
              holder instance for the TypeConverter
            """.trimIndent())
            .build())
        // constructor for the holder
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PRIVATE)
            .addParameter(ClassName.get(pack, className), varName)
            .addStatement("this.$varName = $varName")
            .build())
        .addMethod(MethodSpec.methodBuilder("create")
            .returns(ClassName.get(pack, fileName))
            .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
            .addParameter(
                ClassName.get(pack, className),
                varName)
            .addCode(CodeBlock.builder()
                .addStatement("if (instance == null) instance = new $fileName($varName)")
                .addStatement("return instance")
                .build())
            .build())
        .addMethod(MethodSpec.methodBuilder("get")
            .addAnnotation(Override::class.java)
            .returns(ClassName.get(pack, className))
            .addModifiers(Modifier.PUBLIC)
            .addCode("return $varName;")
            .build())


    return JavaFile.builder(pack, classBuilder.build())
  }

  companion object {
    var typeConverter: TypeElement? = null

  }
}