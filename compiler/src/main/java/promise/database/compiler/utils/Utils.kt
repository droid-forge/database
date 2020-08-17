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

package promise.database.compiler.utils

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import java.io.PrintWriter
import java.io.StringWriter
import java.net.UnknownHostException
import java.util.*
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.*
import javax.lang.model.util.SimpleTypeVisitor6
import javax.lang.model.util.SimpleTypeVisitor7
import javax.lang.model.util.Types
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.reflect.KClass


fun TypeName.isSameAs(javaClass: Class<*>): Boolean {
  if (this.isPrimitive) return this.box().toString() == JavaUtils.wrap(javaClass).name
  return this.toString() == JavaUtils.wrap(javaClass).name
}

fun Element.toTypeName(): TypeName {
  return TypeName.get(this.asType())
}

fun VariableElement.getClass(): KClass<*> {
  val type = this.asType()
  return when (type.kind) {
    TypeKind.DECLARED -> Class.forName(type.toString()).kotlin
    TypeKind.BOOLEAN -> Boolean::class
    TypeKind.BYTE -> Byte::class
    TypeKind.SHORT -> Short::class
    TypeKind.INT -> Int::class
    TypeKind.LONG -> Long::class
    TypeKind.CHAR -> Char::class
    TypeKind.FLOAT -> Float::class
    TypeKind.DOUBLE -> Double::class
    else -> throw Exception("Unknown type: $type, kind: ${type.kind}")
  }
}

fun TypeMirror.asTypeElement(processingEnv: ProcessingEnvironment): TypeElement {
  val typeUtils: Types = processingEnv.typeUtils
  return (try {
    typeUtils.asElement(this)
  } catch (mte: MirroredTypeException) {
    mte.typeMirror
  }) as TypeElement
}

fun TypeMirror.asVariableElement(processingEnv: ProcessingEnvironment): VariableElement? {
  val typeUtils: Types = processingEnv.typeUtils
  val typeMirror = typeUtils.erasure(this)
  if (typeMirror is VariableElement) return typeMirror
  return null
}

fun String.getInstanceProviderClassName(): String = "${this}InstanceProvider"

object Utils {

  fun getAnnotationMirror(typeElement: ExecutableElement, clazz: Class<*>): AnnotationMirror? {
    val clazzName = clazz.name
    for (m in typeElement.annotationMirrors) {
      if (m.annotationType.toString() == clazzName) {
        return m
      }
    }
    return null
  }

  fun getAnnotationMirror(typeElement: TypeElement, clazz: Class<*>): AnnotationMirror? {
    val clazzName = clazz.name
    for (m in typeElement.annotationMirrors) {
      if (m.annotationType.toString() == clazzName) {
        return m
      }
    }
    return null
  }

  fun getAnnotationMirror(typeElement: VariableElement, clazz: Class<*>): AnnotationMirror? {
    val clazzName = clazz.name
    for (m in typeElement.annotationMirrors) {
      if (m.annotationType.toString() == clazzName) {
        return m
      }
    }
    return null
  }

  fun getAnnotationValue(annotationMirror: AnnotationMirror, key: String): AnnotationValue? {
    for ((key1, value) in annotationMirror.elementValues) {
      if (key1!!.simpleName.toString() == key) {
        return value
      }
    }
    return null
  }

  /**
   * Copied from "android.util.Log.getStackTraceString()" in order to avoid usage of Android stack
   * in unit tests.
   *
   * @return Stack trace in form of String
   */
  fun getStackTraceString(tr: Throwable?): String? {
    if (tr == null) return ""

    // This is to reduce the amount of log spew that apps do in the non-error
    // condition of the network being unavailable.
    var t = tr
    while (t != null) {
      if (t is UnknownHostException) return ""
      t = t.cause
    }
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    tr.printStackTrace(pw)
    pw.flush()
    return sw.toString()
  }

  fun toString(`object`: Any?): String? {
    if (`object` == null) return "null"
    if (!`object`.javaClass.isArray) return `object`.toString()
    if (`object` is BooleanArray) return Arrays.toString(`object` as BooleanArray?)
    if (`object` is ByteArray) return Arrays.toString(`object` as ByteArray?)
    if (`object` is CharArray) return Arrays.toString(`object` as CharArray?)
    if (`object` is ShortArray) return Arrays.toString(`object` as ShortArray?)
    if (`object` is IntArray) return Arrays.toString(`object` as IntArray?)
    if (`object` is LongArray) return Arrays.toString(`object` as LongArray?)
    if (`object` is FloatArray) return Arrays.toString(`object` as FloatArray?)
    if (`object` is DoubleArray) return Arrays.toString(`object` as DoubleArray?)
    return if (`object` is Array<*>) Arrays.deepToString(`object` as Array<Any?>?) else "Couldn't find a correct type for the object"
  }

  /**
   * generates provider for element Typename
   */
  fun generateInstanceProviderHolder(
      /**
       * Typename for this provider
       */
      element: ClassName
      ): JavaFile.Builder {

    val className = element.simpleName()

    val pack = element.packageName()

    val varName = className.camelCase()

    val fileName = className.getInstanceProviderClassName()

    val classBuilder = TypeSpec.classBuilder(fileName)
        .addJavadoc("""
          Class holder for instantiation of TypeConverter
        """.trimIndent())
        .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
        .addSuperinterface(ParameterizedTypeName.get(
            ClassName.get("promise.commons", "InstanceProvider"),
            element
        ))
        // field instance for the holder
        .addField(FieldSpec.builder(
            ClassName.get(pack, fileName),
            "instance"
        ).addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .build())
        // holder for type converter
        .addField(FieldSpec.builder(element, varName,
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
            .addJavadoc("""
              Creates an instance of the holder with the converter
            """.trimIndent())
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
}