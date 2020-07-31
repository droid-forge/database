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

import com.github.plural4j.Plural
import com.github.plural4j.Plural.WordForms
import com.github.plural4j.Plural.parse
import com.squareup.javapoet.ClassName
import com.sun.tools.javac.code.Attribute
import com.sun.xml.internal.bind.api.impl.NameConverter
import promise.db.DatabaseEntity
import promise.db.Entity
import promise.db.Index
import java.io.IOException
import java.io.InputStreamReader
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementFilter


fun TypeElement.asTableClassName(processingEnv: ProcessingEnvironment): ClassName {
  val className = this.getClassName()
  val pack = processingEnv.elementUtils.getPackageOf(this).toString()
  return ClassName.get(pack, className)
}


fun Element.getClassName(): String = "${this.simpleName}FastTable"

//@JvmName("PersistableEntityUtils")
fun Element.getTableName(): String {
  val entityTableAnnotation = this.getAnnotation(Entity::class.java)
      ?: throw IllegalArgumentException("Element is not annotated with Entity")
  return if (entityTableAnnotation.tableName.isEmpty()) this.simpleName.toString()
  else {
    entityTableAnnotation.tableName
  }
}

fun String.pluralize(): String {
  val englishWords: Array<WordForms?> = parse("dictionary-en.txt".load())
  val p = Plural(Plural.ENGLISH, englishWords)
  return p.pl(1, this)
}

@Throws(IOException::class)
private fun String.load(): String {
  val reader = InputStreamReader(Utils::class.java.getResourceAsStream("/${this}"), "UTF-8")
  return try {
    val res = StringBuilder()
    var len: Int
    val chr = CharArray(4096)
    while (reader.read(chr).also { len = it } > 0) {
      res.append(chr, 0, len)
    }
    res.toString()
  } finally {
    try {
      reader.close()
    } catch (ignored: IOException) {
    }
  }
}

fun TypeElement.getTableCompoundIndices(): Array<Entity.CompoundIndex> {
  val entityTableAnnotation = this.getAnnotation(Entity::class.java)
      ?: throw IllegalArgumentException("Element is not annotated with Entity")
  return entityTableAnnotation.compoundIndices
}

fun TypeElement.getTableIndices(): Array<String>? {
  val fields = ElementFilter.fieldsIn(this.enclosedElements)
      .filter { it.getAnnotation(Index::class.java) != null }.map {
        it.getNameOfColumn()
      }
  if (fields.isNullOrEmpty()) return null
  return fields.toTypedArray()
}

fun TypeElement.getTableEntities(processingEnv: ProcessingEnvironment): Array<TypeElement> {
  val entityAnnotation = Utils.getAnnotationMirror(this, DatabaseEntity::class.java)
      ?: throw IllegalArgumentException("Element is not annotated with DatabaseEntity")
  val classes = (Utils.getAnnotationValue(entityAnnotation, "persistableEntities")
      ?: throw IllegalArgumentException("DatabaseEntity does not have persistableEntities")).value as Iterable<*>
  return classes.map {
    ((it as Attribute.Class).classType as TypeMirror).asTypeElement(processingEnv)
  }.toTypedArray()
}

fun TypeElement.getDatabaseVersion(): Int {
  val entityAnnotation = this.getAnnotation(DatabaseEntity::class.java)
      ?: throw IllegalArgumentException("Element is not annotated with DatabaseEntity")
  return entityAnnotation.version
}

fun Element.getNameOfColumn(): String {
  //processingEnvironment.messager.printMessage(Diagnostic.Kind.ERROR, "elemtype: "+element.toTypeName().toString()+"\n")
  //processingEnvironment.messager.printMessage(Diagnostic.Kind.ERROR, "class type: "+Int::class.java.name+"\n")
  var name: String? = null
  if ((this.toTypeName().isSameAs(Integer::class.java) ||
          this.toTypeName().isSameAs(Float::class.java) ||
          this.toTypeName().isSameAs(Double::class.java) ||
          this.toTypeName().isSameAs(Boolean::class.java)) &&
      this.getAnnotation(promise.db.Number::class.java) != null) {
    name = this.getAnnotation(promise.db.Number::class.java).columnName
  } else if (this.toTypeName().isSameAs(String::class.java) &&
      this.getAnnotation(promise.db.VarChar::class.java) != null) {
    name = this.getAnnotation(promise.db.VarChar::class.java).columnName
  }
  if (name != null && name.isNotEmpty()) return name
  return this.simpleName.toString()
}