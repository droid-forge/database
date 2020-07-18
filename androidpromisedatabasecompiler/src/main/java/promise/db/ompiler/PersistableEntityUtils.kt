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

import com.squareup.kotlinpoet.ClassName
import com.sun.tools.javac.code.Attribute
import promise.db.DatabaseEntity
import promise.db.Entity
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror

fun TypeElement.asTableClassName(processingEnv: ProcessingEnvironment): ClassName {
  val className = this.getClassName()
  val pack = processingEnv.elementUtils.getPackageOf(this).toString()
  return ClassName(pack, className)
}

fun Element.getClassName(): String = "${this.simpleName}FastTable"

fun Element.getTableName(): String {
  val entityTableAnnotation = this.getAnnotation(Entity::class.java)
      ?: throw IllegalArgumentException("Element is not annotated with Entity")
  return if (entityTableAnnotation.tableName.isEmpty()) "${this.simpleName}"
  else entityTableAnnotation.tableName
}

fun TypeElement.getTableCompoundIndices(): Array<Entity.CompoundIndex> {
  val entityTableAnnotation = this.getAnnotation(Entity::class.java)
      ?: throw IllegalArgumentException("Element is not annotated with Entity")
  return entityTableAnnotation.compoundIndices
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