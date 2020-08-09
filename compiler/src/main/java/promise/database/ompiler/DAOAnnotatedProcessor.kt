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

package promise.database.ompiler

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import org.jetbrains.annotations.NotNull
import promise.database.DAO
import promise.database.Parent
import promise.database.Relation
import promise.database.ompiler.utils.JavaUtils
import promise.database.ompiler.utils.LogUtil
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.VariableElement
import javax.lang.model.util.ElementFilter

class DAOAnnotatedProcessor(private val processingEnv: ProcessingEnvironment) : AnnotatedClassProcessor() {
  override fun process(environment: RoundEnvironment?): List<JavaFile.Builder?>? {
    val javaFiles = ArrayList<JavaFile.Builder?>()
    environment?.getElementsAnnotatedWith(DAO::class.java)
        ?.forEach { element ->
//          if (element.kind != ElementKind.CLASS ||
//              element.kind != ElementKind.INTERFACE)
//            LogUtil.e(Exception("Only classes can be annotated"), element)
          if (!element.modifiers.contains(Modifier.ABSTRACT))
            LogUtil.e(Exception("Class must be abstract"), element)
//          if (element.enclosingElement != null)
//            LogUtil.e(Exception("Class must not extend or implement any other class or interfaces"), element)
          else javaFiles.add(processAnnotation(element))
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
            .addParameter(
                ClassName.get("promise.db", "PromiseDatabase")
                    .annotated(AnnotationSpec.builder(NotNull::class.java).build()),
                "promiseDatabase")
            .build())
    if (element.kind == ElementKind.INTERFACE) classBuilder.addSuperinterface(element.asType())
    else if (element.kind == ElementKind.CLASS) classBuilder.superclass(element.asType())
    val gen = """
      //  private PromiseDatabase fastDatabase;
      //  private PostCommentsTable postCommentsTable;
      //
      //  public PostCommentsDaoImpl(PromiseDatabase fastDatabase) {
      //    this.fastDatabase = fastDatabase;
      //    this.postCommentsTable = (PostCommentsTable) fastDatabase.tableOf(PostComment.class);
      //  }
      //
      //  @Override
      //  public List<PostCommentToReplyRelation> getPostComments(Criteria andCriteria) {
      //    Cursor cursor = fastDatabase.getDatabaseInstance().query(
      //        postCommentsTable.queryBuilder()
      //            .whereAnd(andCriteria));
      //    return getPostCommentRepliesImpl(cursor);
      //  }
      //
      //  public List<PostComment> getPostCommentRepliesImpl(PostComment postComment) {
      //    return new ArrayList<>(postCommentsTable.findAll(PostCommentsTable.idColumn.with(postComment.getId())));
      //  }
      //
      //  private List<PostCommentToReplyRelation> getPostCommentRepliesImpl(Cursor cursor) {
      //    IdentifiableList<? extends PostComment> postComments = postCommentsTable.collection(cursor);
      //    return postComments.map(postComment -> new PostCommentToReplyRelation() {{
      //      setPostComment(postComment);
      //      setPostCommentReplies(getPostCommentRepliesImpl(postComment));
      //    }});
      //  }
      //
      //  private PostCommentToReplyRelation getPostCommentReplyImpl(Cursor cursor) {
      //    PostComment postComment = postCommentsTable.single(cursor);
      //    return new PostCommentToReplyRelation() {{
      //      setPostComment(postComment);
      //      setPostCommentReplies(getPostCommentRepliesImpl(postComment));
      //    }};
      //  }
    """.trimIndent()
    ElementFilter.methodsIn(element.enclosedElements).forEach { executableElement ->
      val returnType = processingEnv.typeUtils.asElement(executableElement.returnType)
      val relationType: Element
      if (JavaUtils.isCollectionType(processingEnv, returnType as VariableElement?)) {
        val typeMirrors = JavaUtils.getParameterizedTypeMirrors(returnType)
        if (typeMirrors != null && typeMirrors.isNotEmpty()) {
          relationType = processingEnv.typeUtils.asElement(typeMirrors[0])
          if (relationType.getAnnotation(Relation::class.java) != null) {
            var relationFields = ElementFilter.fieldsIn(relationType.enclosedElements);
            val parentRelations = relationFields.filter {
              it.getAnnotation(Parent::class.java) != null
            }
            if (parentRelations.size > 1) {
              LogUtil.e(Exception("Relation classes can only have one field annotated with Parent"), relationType)
            }
            val parentRelation = parentRelations[0]


          } else LogUtil.e(Exception("Class must be annotated as Relation"), relationType)
        }
      }
      else {

      }
    }
    return JavaFile.builder(pack, classBuilder.build())
  }

}