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
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.lang.model.util.ElementFilter
import javax.tools.Diagnostic


class DatabaseAbstractFuncsGenerator(
    private val fileSpec: FileSpec.Builder,
    private val element: TypeElement,
    private val processingEnv: ProcessingEnvironment) : CodeBlockGenerator<List<FunSpec>?> {


  override fun generate(): List<FunSpec>? {
    val enclosedElements: List<Element?> = element.enclosedElements
    val methods: MutableList<ExecutableElement> = ArrayList(ElementFilter.methodsIn(enclosedElements))

    val abstractFunctions = methods.filter {
      it.modifiers.contains(Modifier.ABSTRACT)
    }
    //if (abstractFunctions.isNullOrEmpty()) return null
    val funSpecs = ArrayList<FunSpec>()

    val entityClasses = element.getTableEntities(processingEnv)

    abstractFunctions.filter {
      it.parameters.isEmpty() && it.returnType.kind != TypeKind.VOID
    }.forEach { method ->
      val returnType = method.returnType
      val methodName = method.simpleName

      val typeElement = returnType.asTypeElement(processingEnv)
      val returnTypeName = method.returnType.toString()
      val pack = processingEnv.elementUtils.getPackageOf(typeElement).toString()
      //processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "ReturnType: $pack $returnTypeName")
      val className = ClassName(pack, returnTypeName)
      funSpecs.add(FunSpec.builder(methodName.toString())
          .addModifiers(KModifier.OVERRIDE)
          //.returns(className)
          .addCode(CodeBlock.of("return getDatabaseInstance().obtain<${className}>(${className}::class.java)"))
          .build())
      //fileSpec.addImport(ClassName.bestGuess(returnTypeName))
      // val tableClassNameStrings = entityClasses.map { it.getTableName() }

//      if (tableClassNameStrings.contains(returnTypeName)) {
//        """
//          override fun getPersonFastTable(): PersonFastTable {
//            return getDatabaseInstance().obtain<PersonFastTable>(PersonFastTable::class.java)
//          }
//        """.trimIndent()
//        funSpecs.add(FunSpec.builder(methodName.toString())
//            .addModifiers(KModifier.OVERRIDE)
//            .returns(ClassName.bestGuess(returnTypeName))
//            .addCode(CodeBlock.of("return getDatabaseInstance().obtain<${returnTypeName}>(${returnTypeName}::class.java)"))
//            .build())
//      }
    }
    return funSpecs
  }


}