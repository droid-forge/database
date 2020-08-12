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

package promise.database.compiler

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeVariableName
import com.squareup.javapoet.WildcardTypeName
import org.jetbrains.annotations.NotNull
import promise.database.DAO
import promise.database.compiler.utils.JavaUtils
import promise.database.compiler.utils.asTypeElement
import promise.database.compiler.utils.getTableEntities
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.lang.model.util.ElementFilter

class DatabaseAbstractMethodsGenerator(
    private val element: TypeElement,
    private val processingEnv: ProcessingEnvironment) : CodeGenerator<List<MethodSpec>?> {

  override fun generate(): List<MethodSpec>? {
    val enclosedElements: List<Element?> = element.enclosedElements
    val methods: MutableList<ExecutableElement> = ArrayList(ElementFilter.methodsIn(enclosedElements))

    val abstractFunctions = methods.filter {
      it.modifiers.contains(Modifier.ABSTRACT)
    }

    val funSpecs = ArrayList<MethodSpec>()

    abstractFunctions.filter {
      it.parameters.isEmpty() && it.returnType.kind != TypeKind.VOID
    }.forEach { method ->
      val returnType = method.returnType
      val typeElement = returnType.asTypeElement(processingEnv)
      if (typeElement.getAnnotation(DAO::class.java) != null) {
        val returnTypeName = typeElement.simpleName.toString()
        val pack = processingEnv.elementUtils.getPackageOf(typeElement).toString()
        val className = "${returnTypeName}Impl"
        funSpecs.add(MethodSpec.overriding(method)
            .addCode(JavaUtils.generateReturnDaoImplInstance(ClassName.get(pack, className)))
            .build())
      }
    }

    val codeBlock = CodeBlock.builder()
    element.getTableEntities(processingEnv).forEach {
      JavaUtils.generateIfStatementObtainClassString(processingEnv, codeBlock, it)
    }
    codeBlock.addStatement("throw new IllegalArgumentException(entityClass.getCanonicalName() + \"not registered with this database\")")

    val typeVariable: TypeVariableName? = TypeVariableName.get("T").withBounds(
        ParameterizedTypeName.get(
            ClassName.get("promise.commons.model", "Identifiable"),
            ClassName.get(Integer::class.java)))
    funSpecs.add(MethodSpec.methodBuilder("tableOf")
        .addTypeVariable(typeVariable)
        .addParameter(ParameterizedTypeName.get(ClassName.get(Class::class.java),
            WildcardTypeName.subtypeOf(TypeVariableName.get("T"))
        ), "entityClass")
        .addAnnotation(Override::class.java)
        .addAnnotation(NotNull::class.java)
        .addJavadoc("""
          Returns the table for the specified entity
          @Param entityClass Class of the entity persisted
          @Returns the table instance
        """.trimIndent())
        .addException(IllegalArgumentException::class.java)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .returns(ParameterizedTypeName.get(
            ClassName.get("promise.db", "FastTable"),
            TypeVariableName.get("T")))
        .addCode(codeBlock.build())
        .build())

    return funSpecs
  }
//
//  private fun getPackage(element: TypeElement, returnTypeName: String): String {
//    if (entities.isEmpty()) entities = element.getTableEntities(processingEnv)
//    var pack = ""
//    entities.forEach {
//      if (returnTypeName == it.getTableClassNameString()) {
//        pack = processingEnv.elementUtils.getPackageOf(it).toString()
//      }
//    }
//    return pack
//
//  }


}