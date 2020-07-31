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

import com.google.auto.common.MoreTypes.asElement
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeName.*
import java.io.PrintWriter
import java.io.StringWriter
import java.net.UnknownHostException
import java.util.*
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Types
import javax.tools.Diagnostic
import kotlin.collections.HashMap
import kotlin.reflect.KClass


fun TypeName.isSameAs(javaClass: Class<*>): Boolean {
  if (this.isPrimitive) this.box()
  return this.toString() == JavaUtils.wrap(javaClass).name
}

fun TypeName.isSameAs2(processingEnvironment: ProcessingEnvironment, javaClass: Class<*>): Boolean {
  processingEnvironment.messager.printMessage(Diagnostic.Kind.OTHER, "comparing \n: elem typeName ${this}, javaClass: ${JavaUtils.wrap(javaClass).name}")
  if (this.isPrimitive)  return this.box().toString() == JavaUtils.wrap(javaClass).name
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

fun getAnnotationMirror(element: Element, annotationClass: Class<out Annotation?>): Optional<out AnnotationMirror?>? {
  val annotationClassName = annotationClass.name
  return element.annotationMirrors.stream()
      .filter { m: AnnotationMirror? -> m!!.annotationType.toString() == annotationClassName }
      .findFirst()
}

fun TypeMirror.asTypeElement(processingEnv: ProcessingEnvironment): TypeElement {
  val typeUtils: Types = processingEnv.typeUtils
  return (try {
    typeUtils.asElement(this)
  } catch (mte: MirroredTypeException) {
    mte.typeMirror
  }) as TypeElement
}

object Utils {

  fun getAnnotationMirror(typeElement: TypeElement, clazz: Class<*>): AnnotationMirror? {
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

  fun classImplementsInterface(classElement: TypeElement, interfaceElement: TypeElement): Boolean {
    for (interfaceType in classElement.interfaces) {
      if ((interfaceType as DeclaredType).asElement() == interfaceElement) return true
    }
//    val classMethods: List<ExecutableElement> = getClassMethods(classElement)
//    var implementsMethod: Boolean
//    for (interfaceMethod in ElementFilter.methodsIn(interfaceElement.enclosedElements)) {
//      implementsMethod = false
//      for (classMethod in classMethods) {
//        if (sameMethod(interfaceMethod, classMethod)) {
//          implementsMethod = true
//          classMethods.remove(classMethod)
//          break
//        }
//      }
//      if (!implementsMethod) {
//        builder.processError(WebserviceapMessages.WEBSERVICEAP_METHOD_NOT_IMPLEMENTED(interfaceElement.simpleName, classElement.simpleName, interfaceMethod), interfaceMethod)
//        return false
//      }
//    }
    return false
  }

  fun isFromInterface(typeElement: TypeElement, interfaceName: String): Boolean {
    for (anInterface in typeElement.interfaces) {
      if (anInterface.toString() == interfaceName) {
        return true
      } else {
        val isInterface = isFromInterface(asElement(anInterface) as TypeElement, interfaceName)
        if (isInterface) {
          return true
        }
      }
    }
    return false
  }

  /**
   * Returns true if a and b are equal, including if they are both null.
   *
   * *Note: In platform versions 1.1 and earlier, this method only worked well if
   * both the arguments were instances of String.*
   *
   * @param a first CharSequence to check
   * @param b second CharSequence to check
   * @return true if a and b are equal
   *
   *
   * NOTE: Logic slightly change due to strict policy on CI -
   * "Inner assignments should be avoided"
   */


  fun notEquals(a: CharSequence?, b: CharSequence?): Boolean {
    if (a === b) return false
    if (a != null && b != null) {
      val length = a.length
      if (length == b.length) return if (a is String && b is String) a != b else {
        for (i in 0 until length) if (a[i] != b[i]) return true
        false
      }
    }
    return true
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
}