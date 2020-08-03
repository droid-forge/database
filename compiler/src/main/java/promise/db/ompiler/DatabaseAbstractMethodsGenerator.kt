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
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeVariableName
import com.squareup.javapoet.WildcardTypeName
import org.jetbrains.annotations.NotNull
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.lang.model.util.ElementFilter
import javax.tools.Diagnostic

class DatabaseAbstractMethodsGenerator(
    private val element: TypeElement,
    private val processingEnv: ProcessingEnvironment) : CodeGenerator<List<MethodSpec>?> {


  override fun generate(): List<MethodSpec>? {
    val enclosedElements: List<Element?> = element.enclosedElements
    val methods: MutableList<ExecutableElement> = ArrayList(ElementFilter.methodsIn(enclosedElements))

    val abstractFunctions = methods.filter {
      it.modifiers.contains(Modifier.ABSTRACT)
    }

    //if (abstractFunctions.isNullOrEmpty()) return null
    val funSpecs = ArrayList<MethodSpec>()

    abstractFunctions.forEach {
      funSpecs.add(MethodSpec.overriding(it)
          .build())
    }

    abstractFunctions.filter {
      it.parameters.isEmpty() && it.returnType.kind != TypeKind.VOID
    }.forEach { method ->
      val returnType = method.returnType
      val methodName = method.simpleName

      val typeElement = returnType.asTypeElement(processingEnv)
      val returnTypeName = method.returnType.toString()
      val pack = processingEnv.elementUtils.getPackageOf(typeElement).toString()
      processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "ReturnType: $pack $returnTypeName")
      val className = ClassName.get(getPackage(element, returnTypeName), returnTypeName)
      funSpecs.add(MethodSpec.methodBuilder(methodName.toString())
          .addAnnotation(Override::class.java)
          .addAnnotation(NotNull::class.java)
          .addModifiers(Modifier.PUBLIC)
          .returns(className)
          .addCode(CodeBlock.builder()
              .addStatement("return getDatabaseInstance().obtain(${className}.class)")
              .build())
          .build())

    }

    val codeBlock = CodeBlock.builder()
    element.getTableEntities(processingEnv).forEach {
      JavaUtils.generateIfStatementObtainClassString(processingEnv, codeBlock, it)
    }
    codeBlock.addStatement("throw new IllegalArgumentException(entityClass.getCanonicalName() + \"not registered with this database\")")

    /**
     * @Override
    @NotNull
    public <T extends Identifiable<Integer>> FastTable<T> getTable(Class<? extends T> entityClass) throws IllegalArgumentException {
    if (entityClass == Person.class) return getDatabaseInstance().obtain(PersonFastTable.class);
    if (entityClass == Exam.class) return getDatabaseInstance().obtain(ExamFastTable.class);
    throw new IllegalArgumentException(entityClass.getCanonicalName() + "not registered with this database");
    }
     */
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
        .addException(IllegalArgumentException::class.java)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .returns(ParameterizedTypeName.get(ClassName.get("promise.db", "FastTable"), TypeVariableName.get("T")))
        .addCode(codeBlock.build())
        .build())

    return funSpecs
  }

  private var entities: Array<TypeElement> = emptyArray()

  private fun getPackage(element: TypeElement, returnTypeName: String): String {
    if (entities.isEmpty()) entities = element.getTableEntities(processingEnv)
    var pack = ""
    entities.forEach {
      if (returnTypeName == it.getClassName()) {
        pack = processingEnv.elementUtils.getPackageOf(it).toString()
      }
    }
    return pack

  }


}