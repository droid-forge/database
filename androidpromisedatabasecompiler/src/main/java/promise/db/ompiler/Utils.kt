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

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.PrintWriter
import java.io.StringWriter
import java.net.UnknownHostException
import java.util.*
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.tools.Diagnostic
import kotlin.collections.HashMap
import kotlin.reflect.jvm.internal.impl.name.FqName
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap

fun  wrap(c: Class<*>): ClassName? {
  if (c == String::class.java) return STRING
  return if (c.isPrimitive) PRIMITIVES_TO_WRAPPERS[c] else c.asClassName()
}

private val PRIMITIVES_TO_WRAPPERS: Map<Class<*>, ClassName> = HashMap<Class<*>, ClassName>().apply {
  put(Boolean::class.java, BOOLEAN)
  put(Byte::class.java, BYTE)
  put(Char::class.java, CHAR)
  put(Double::class.java, DOUBLE)
  put(Float::class.java, FLOAT)
  put(Int::class.java, INT)
  put(Integer::class.java, INT)
  put(Long::class.java, LONG)
  put(Short::class.java, SHORT)
}

fun TypeName.isSameAs(javaClass: Class<*>): Boolean {
  return this.toString() == wrap(javaClass).toString()
}

fun TypeName.isSameAs2(processingEnvironment: ProcessingEnvironment, javaClass: Class<*>): Boolean {
  processingEnvironment.messager.printMessage(Diagnostic.Kind.OTHER, "comparing \n: elem typeName ${this}, javaClass: ${javaClass.name} wrapClassName: ${wrap(javaClass)} ")
  return this.toString() == wrap(javaClass).toString()
}

fun Element.toTypeName(): TypeName {
  return this.asType().asTypeName().javaToKotlinType()
}

fun Element.javaToKotlinType(): TypeName =
    asType().asTypeName().javaToKotlinType()

fun TypeName.javaToKotlinType(): TypeName {
  return if (this is ParameterizedTypeName) {
    (rawType.javaToKotlinType() as ClassName).parameterizedBy(*typeArguments.map { it.javaToKotlinType() }.toTypedArray())
  } else {
    val className =
        JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(toString()))
            ?.asSingleFqName()?.asString()

    return if (className == null) {
      this
    } else {
      ClassName.bestGuess(className)
    }
  }
}

object Utils {


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