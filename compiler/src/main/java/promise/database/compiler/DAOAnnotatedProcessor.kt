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

import com.google.auto.common.MoreTypes
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeSpec
import org.jetbrains.annotations.NotNull
import promise.database.Child
import promise.database.DAO
import promise.database.Parent
import promise.database.Relation
import promise.database.compiler.utils.JavaUtils
import promise.database.compiler.utils.LogUtil
import promise.database.compiler.utils.asTableClassName
import promise.database.compiler.utils.camelCase
import promise.database.compiler.utils.toTypeName
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.util.ElementFilter


class DAOAnnotatedProcessor(private val processingEnv: ProcessingEnvironment) : AnnotatedClassProcessor() {
  override fun process(environment: RoundEnvironment?): List<JavaFile.Builder?>? {
    val javaFiles = ArrayList<JavaFile.Builder?>()
    environment?.getElementsAnnotatedWith(DAO::class.java)
        ?.forEach {
          javaFiles.add(processAnnotation(it))
        }
    return javaFiles
  }

  private fun processAnnotation(element: Element): JavaFile.Builder {

    val className = element.simpleName.toString()
    val pack = processingEnv.elementUtils.getPackageOf(element).toString()

    val fileName = className + "Impl"

    val classBuilder = TypeSpec.classBuilder(fileName)
        .addModifiers(Modifier.PUBLIC)
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(ClassName.get("promise.db", "PromiseDatabase")
                .annotated(AnnotationSpec.builder(NotNull::class.java).build()),
                "promiseDatabase")
            .addStatement("this.promiseDatabase = promiseDatabase").build())

    classBuilder.addField(FieldSpec.builder(ClassName.get("promise.db", "PromiseDatabase")
        .annotated(AnnotationSpec.builder(NotNull::class.java).build()),
        "promiseDatabase").build())

    if (element.kind == ElementKind.INTERFACE) classBuilder.addSuperinterface(element.asType())
    else if (element.kind == ElementKind.CLASS) classBuilder.superclass(element.asType())

    val gen = """
      
  private PostCommentToReplyRelation getPostCommentToReplyRelation(PostComment postComment) {
    return new PostCommentToReplyRelation() {{
      setPostComment(postComment);
      setPostCommentReplies(new ArrayList<>(
          promiseDatabase.tableOf(PostComment.class).findAll(
              PostCommentsTable.postCommentIdColumn.with(
                  postComment.getId().toString()))));
    }};
  }

  @Override
  public List<PostCommentToReplyRelation> getPostComments(Criteria andCriteria) {
    Cursor cursor = promiseDatabase.getDatabaseInstance().query(
        promiseDatabase.tableOf(PostComment.class).queryBuilder()
            .whereAnd(andCriteria));
    return getPostCommentRepliesImpl(cursor);
  }

  private List<PostCommentToReplyRelation> getPostCommentRepliesImpl(Cursor cursor) {
    IdentifiableList<? extends PostComment> postComments = promiseDatabase.tableOf(PostComment.class).collection(cursor);
    return postComments.map(this::getPostCommentToReplyRelation);
  }
    """.trimIndent()
    ElementFilter.methodsIn(element.enclosedElements).forEach { executableElement ->
      classBuilder.addMethod(MethodSpec.overriding(executableElement)
          .addCode("return null;").build())
      var relationType: Element?
      if (MoreTypes.isType(executableElement.returnType) &&
          MoreTypes.isTypeOf(Collection::class.java, executableElement.returnType)) {
        val typeMirrors =
            JavaUtils.getParameterizedTypeMirrors(MoreTypes.asDeclared(executableElement.returnType) as VariableElement)

        val typeMirror = typeMirrors[0]
        relationType = processingEnv.typeUtils.asElement(typeMirror)
        LogUtil.n(Exception("Return type is collection: ${relationType}"))
        //generateGetRelationMethod(classBuilder, relationType)
      }

      else {
        relationType = processingEnv.typeUtils.asElement(executableElement.returnType)
        LogUtil.n(Exception("Return type is : ${relationType}"))
        //generateGetRelationMethod(classBuilder, relationType)
      }

      if (relationType == null) throw IllegalStateException("Failed to determine relation class")

//      classBuilder.addMethod(MethodSpec.methodBuilder("get${relationType.simpleName}Impl")
//          .addParameter(ClassName.get("android.database", "Cursor"), "cursor")
//          .addModifiers(Modifier.PRIVATE)
//          .returns(processingEnv.typeUtils.asElement(executableElement.returnType).toTypeName())
//          .addCode("return null;")
//          .build())
    }
    return JavaFile.builder(pack, classBuilder.build())
  }

  private fun generateGetRelationMethod(classBuilder: TypeSpec.Builder,
                                        relationType: Element) {
    val gen = """
  private PostCommentToReplyRelation getPostCommentToReplyRelation(PostComment postComment) {
    return new PostCommentToReplyRelation() {{
      setPostComment(postComment);
      setPostCommentReplies(new ArrayList<>(
          promiseDatabase.tableOf(PostComment.class).findAll(
              PostCommentsTable.postCommentIdColumn.with(
                  postComment.getId().toString()))));
    }};
  }
  }
    """.trimIndent()
    if (relationType.getAnnotation(Relation::class.java) != null) {
      val relationFields = ElementFilter.fieldsIn(relationType.enclosedElements);
      val parentRelations = relationFields.filter {
        it.getAnnotation(Parent::class.java) != null
      }
      if (parentRelations.isEmpty()) LogUtil.e(Exception("Relation classes must have a parent annotated field"), relationType)
      if (parentRelations.size > 1) LogUtil.e(Exception("Relation classes can only have one field annotated with Parent"), relationType)
      val parentRelation = parentRelations[0]

      classBuilder.addMethod(MethodSpec.methodBuilder("get${relationType.simpleName}")
          .addModifiers(Modifier.PRIVATE)
          .returns(relationType.toTypeName())
          .addParameter(ParameterSpec.builder(parentRelation.toTypeName(),
              parentRelation.simpleName.toString().camelCase()).build())
          .addCode(CodeBlock.builder()
              .beginControlFlow("return new ${relationType.simpleName}() {")
              .addStatement("set${parentRelation.simpleName}(${parentRelation.simpleName.toString().camelCase()})")
              .add(getCodeBlockForChildElements(relationType).build())
              .endControlFlow("}")
              .build())
          .build())

      val parentTableClass = (parentRelation as TypeElement).asTableClassName(processingEnv)
      classBuilder.addField(FieldSpec.builder(parentTableClass, parentTableClass.simpleName().camelCase())
          .addModifiers(Modifier.PRIVATE)
          .build())

    } else LogUtil.e(Exception("Class must be annotated as Relation"), relationType)
  }

  private fun getCodeBlockForChildElements(element: Element): CodeBlock.Builder {
    val codeBlock = CodeBlock.builder()
    ElementFilter.fieldsIn(element.enclosedElements)
        .filter {
          it.getAnnotation(Parent::class.java) == null
        }
        .filter {
          if (it.getAnnotation(Child::class.java) != null) true
          else {
            LogUtil.e(Exception("Field ${it.simpleName} must be annotated with Child"))
            false
          }
        }.forEach {
          val childAnnotation = it.getAnnotation(Child::class.java)
          if (JavaUtils.isCollectionType(processingEnv, it)) {
            val typeMirrors = JavaUtils.getParameterizedTypeMirrors(it)
            if (typeMirrors != null && typeMirrors.isNotEmpty()) {
              val relationType = processingEnv.typeUtils.asElement(typeMirrors[0])
              val fieldToRelatedKey = childAnnotation.fieldRelatedToParent
              val parentRelationKey = childAnnotation.parentRelatedField

            }
          } else {
          }
        }
    return codeBlock
  }

}