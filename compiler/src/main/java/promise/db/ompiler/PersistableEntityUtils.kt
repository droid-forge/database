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
import com.squareup.javapoet.TypeName
import com.sun.tools.javac.code.Attribute
import org.atteo.evo.inflector.English
import promise.db.DatabaseEntity
import promise.db.Entity
import promise.db.ForeignKey
import promise.db.HasMany
import promise.db.HasOne
import promise.db.Index
import java.io.IOException
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementFilter
import javax.tools.Diagnostic

/**
 * gets the className for type element
 */
fun TypeElement.asTableClassName(processingEnv: ProcessingEnvironment): ClassName {
  val className = this.getClassName()
  val pack = processingEnv.elementUtils.getPackageOf(this).toString()
  return ClassName.get(pack, className)
}

/**
 * gets the class name for the table class of this element
 */
fun Element.getClassName(): String = "${this.simpleName.toString().pluralize().capitalizeFirst()}Table"

//@JvmName("PersistableEntityUtils")
/**
 * get the name of table for this entity
 */
fun Element.getTableName(): String {
  val entityTableAnnotation = this.getAnnotation(Entity::class.java)
      ?: throw IllegalArgumentException("Element is not annotated with Entity")
  return if (entityTableAnnotation.tableName.isEmpty()) this.simpleName.toString().pluralize().decapitalize()
  else {
    entityTableAnnotation.tableName
  }
}

/**
 * pluralize a string for table names
 */
fun String.pluralize(): String = English.plural(this)

/**
 * gets compound indices for a table
 */
@Throws(IOException::class)
fun TypeElement.getTableCompoundIndices(): Array<Entity.CompoundIndex> {
  val entityTableAnnotation = this.getAnnotation(Entity::class.java)
      ?: throw IllegalArgumentException("Element is not annotated with Entity")
  return entityTableAnnotation.compoundIndices
}

/**
 * gets table indices for this entity
 */
fun TypeElement.getTableIndices(): Array<String>? {
  val fields = ElementFilter.fieldsIn(this.enclosedElements)
      .filter { it.getAnnotation(Index::class.java) != null }.map {
        it.getNameOfColumn()
      }
  if (fields.isNullOrEmpty()) return null
  return fields.toTypedArray()
}

/**
 * gets foreign keys for this entity
 */
fun TypeElement.getTableForeignKeys(processingEnv: ProcessingEnvironment): Array<Map<String, String>>? {
  val fields = ElementFilter.fieldsIn(this.enclosedElements)
      .filter { it.getAnnotation(ForeignKey::class.java) != null }.map {
        val foreignKeyAnnotationMirror = Utils.getAnnotationMirror(it, ForeignKey::class.java)
        val annotationValue = Utils.getAnnotationValue(foreignKeyAnnotationMirror!!, "referencedEntity")
        val entityClass = ((annotationValue as Attribute.Class).classType as TypeMirror).asTypeElement(processingEnv)
        val refColName = Utils.getAnnotationValue(foreignKeyAnnotationMirror, "referencedEntityColumnName")
        mapOf(Pair("columnName", it.getNameOfColumn()),
            Pair("referencedColumnName", if (refColName != null ) refColName.value as String else "id"),
            Pair("referencedTableName", entityClass.getTableName()))
      }
  if (fields.isNullOrEmpty()) return null
  return fields.toTypedArray()
}

/**
 * gets table entities for this database entity
 */
fun TypeElement.getTableEntities(processingEnv: ProcessingEnvironment): Array<TypeElement> {
  val entityAnnotation = Utils.getAnnotationMirror(this, DatabaseEntity::class.java)
      ?: throw IllegalArgumentException("Element is not annotated with DatabaseEntity")
  val classes = (Utils.getAnnotationValue(entityAnnotation, "persistableEntities")
      ?: throw IllegalArgumentException("DatabaseEntity does not have persistableEntities")).value as Iterable<*>
  return classes.map {
    ((it as Attribute.Class).classType as TypeMirror).asTypeElement(processingEnv)
  }.toTypedArray()
}

/**
 * gets database version
 */
fun TypeElement.getDatabaseVersion(): Int {
  val entityAnnotation = this.getAnnotation(DatabaseEntity::class.java)
      ?: throw IllegalArgumentException("Element is not annotated with DatabaseEntity")
  return entityAnnotation.version
}

/**
 * gets name of column for a field in entity class
 */
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

/**
 * capitalize the first char of string
 */

fun String.capitalizeFirst(): String = this.replaceFirst(this.first(), this.first().toUpperCase())

fun String.camelCase(): String = this.replaceFirst(this.first(), this.first().toLowerCase())

fun Element.checkIfHasTypeConverter(): Boolean {
  //return true
  if (TypeConverterProcessor.typeConverter != null)
    return this.getConverterCompatibleMethod(ConverterTypes.SERIALIZER) != null &&
      this.getConverterCompatibleMethod(ConverterTypes.DESERIALIZER) != null
  return false
}

fun Element.checkIfHasTypeConverter(processingEnv: ProcessingEnvironment): Boolean {
  //return true
  if (TypeConverterProcessor.typeConverter != null)
    return this.getConverterCompatibleMethod(ConverterTypes.SERIALIZER, processingEnv) != null &&
        this.getConverterCompatibleMethod(ConverterTypes.DESERIALIZER, processingEnv) != null
  return false
}

enum class ConverterTypes {
  DESERIALIZER,
  SERIALIZER
}

fun Element.getConverterCompatibleMethod(converterTypes: ConverterTypes): ExecutableElement? {
  if (TypeConverterProcessor.typeConverter != null) {
    val methods =
        ElementFilter.methodsIn(TypeConverterProcessor.typeConverter!!.enclosedElements)
            .filter { it.parameters.size == 1 }
    return methods.find {
      when(converterTypes) {
        ConverterTypes.DESERIALIZER -> {
          JavaUtils.isTypeEqual(it.parameters[0].asType(), TypeName.get(String::class.java)) &&
              JavaUtils.isTypeEqual(it.returnType, TypeName.get(this.asType()))
        }
        ConverterTypes.SERIALIZER -> {
          JavaUtils.isTypeEqual(it.parameters[0].asType(), TypeName.get(this.asType())) &&
              JavaUtils.isTypeEqual(it.returnType, TypeName.get(String::class.java))
        }
      }
    }
  }
    else return null
}


fun Element.getConverterCompatibleMethod(converterTypes: ConverterTypes, processingEnv: ProcessingEnvironment): ExecutableElement? {
  if (TypeConverterProcessor.typeConverter != null) {
    val methods =
        ElementFilter.methodsIn(TypeConverterProcessor.typeConverter!!.enclosedElements)
            .filter {
              it.parameters.size == 1 }
    return methods.find {
//      processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "Type converter: " +
//          " method: ${it}, " +
//          " returnType: ${TypeName.get(it.returnType)}" +
//          " parameter: ${TypeName.get(it.parameters[0].asType())}" +
//          " elementType ${TypeName.get(this.asType())}")
      when(converterTypes) {
        ConverterTypes.DESERIALIZER -> {

          JavaUtils.isTypeEqual(it.parameters[0].asType(), TypeName.get(String::class.java)) &&
              JavaUtils.isTypeEqual(it.returnType, TypeName.get(this.asType()))
        }
        ConverterTypes.SERIALIZER -> {
          JavaUtils.isTypeEqual(it.parameters[0].asType(), TypeName.get(this.asType())) &&
              JavaUtils.isTypeEqual(it.returnType, TypeName.get(String::class.java))
        }
      }
    }
  }
  else return null
}
fun Element.isElementAnnotatedAsRelation(): Boolean {
  //return false
  return this.getAnnotation(HasMany::class.java) != null ||
     this.getAnnotation(HasOne::class.java) != null
}