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

package promise.db.ompiler;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import kotlin.Pair;
import promise.db.Entity;
import promise.db.HasMany;
import promise.db.HasOne;
import promise.db.ompiler.utils.JavaUtils;
import promise.db.ompiler.utils.PersistableEntityUtilsKt;
import promise.db.ompiler.utils.Utils;
import promise.db.ompiler.utils.UtilsKt;

import static promise.db.ompiler.utils.PersistableEntityUtilsKt.asTableClassName;
import static promise.db.ompiler.utils.PersistableEntityUtilsKt.camelCase;
import static promise.db.ompiler.utils.PersistableEntityUtilsKt.capitalizeFirst;
import static promise.db.ompiler.utils.PersistableEntityUtilsKt.getClassName;
import static promise.db.ompiler.utils.PersistableEntityUtilsKt.pluralize;

public class TableRelationsGenerator implements CodeGenerator<List<JavaFile.Builder>> {

  private ProcessingEnvironment processingEnvironment;
  private TypeElement element;
  private List<VariableElement> relationElements;

  private TypeSpec.Builder classBuilder;

  /*
   * Variable name for element
   */
  private String parentElementVarName;
  /*
   * pluralized version of parentElementVarName
   */
  private String parentElementVarNames;

  private String getParentElementTableVarName;

  public TableRelationsGenerator(ProcessingEnvironment processingEnvironment,
                                 TypeElement element,
                                 List<VariableElement> relationElements) {
    this.processingEnvironment = processingEnvironment;
    this.element = element;
    this.relationElements = relationElements;
    parentElementVarName = camelCase(element.getSimpleName().toString());
    parentElementVarNames = pluralize(parentElementVarName);
    getParentElementTableVarName = camelCase(getClassName(element));
  }

  @Override
  public List<JavaFile.Builder> generate() throws Exception {
    ArrayList<JavaFile.Builder> javaFiles = new ArrayList<>();
    String className = element.getSimpleName().toString();
    String pack = processingEnvironment.getElementUtils().getPackageOf(element).toString();
    String fileName = className + "RelationsDao";
    classBuilder = TypeSpec.classBuilder(fileName)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
    classBuilder.addField(FieldSpec.builder(
        asTableClassName(element, processingEnvironment),
        getParentElementTableVarName)
        .addModifiers(Modifier.PRIVATE)
        .build());
    ArrayList<ParameterSpec> constructorParameters = new ArrayList<>();
    ArrayList<Pair<String, String>> constructorInits = new ArrayList<>();
    constructorParameters.add(ParameterSpec.builder(PersistableEntityUtilsKt.asTableClassName(element, processingEnvironment), getParentElementTableVarName).build());
    relationElements.forEach(relationElement -> {
      if (relationElement.getAnnotation(HasMany.class) != null) {
        try {
          Pair<String, String> pair = generateForHasManyRelation(relationElement, constructorParameters);
          if (pair != null) constructorInits.add(pair);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      else if (relationElement.getAnnotation(HasOne.class) != null) {
        generateForHasOneRelation(relationElement);
      }
    });
    CodeBlock.Builder constructorBlock = CodeBlock.builder();
    constructorBlock.addStatement("this." + getParentElementTableVarName + " = " + getParentElementTableVarName);
    constructorInits.forEach(stringStringPair ->
        constructorBlock.addStatement("this." + stringStringPair.getFirst() + " = " + stringStringPair.getSecond()));
    classBuilder.addMethod(MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameters(constructorParameters)
        .addCode(constructorBlock.build())
        .build());

    javaFiles.add(JavaFile.builder(pack, classBuilder.build()));
    javaFiles.add(Utils.INSTANCE.generateInstanceProviderHolder(ClassName.get(pack, fileName)));
    return javaFiles;
  }

  /**
   * generates fields and methods for HasMany relation of the given element
   *
   * @param hasManyRelationFieldElement element with HasMany relation
   * @param constructorParameters
   */
  private Pair<String, String> generateForHasManyRelation(Element hasManyRelationFieldElement, ArrayList<ParameterSpec> constructorParameters) throws Exception {
    if (JavaUtils.isCollectionType(processingEnvironment, (VariableElement) hasManyRelationFieldElement)) {
      List<? extends TypeMirror> typeMirrors = JavaUtils.getParameterizedTypeMirrors((VariableElement) hasManyRelationFieldElement);
      if (typeMirrors != null) {
        /*
         * Field name for the relation
         */
        String hasManyRelationFieldName = hasManyRelationFieldElement.getSimpleName().toString();
        /*
         * Element that this element is related to by HasMany relation
         */
        TypeElement relationElement = UtilsKt.asTypeElement(typeMirrors.get(0), processingEnvironment);
        if (relationElement.getAnnotation(Entity.class) == null)
          throw new Exception("Element related to " + element.getSimpleName() + " as HasMany in " + hasManyRelationFieldElement.getSimpleName() + " field is not marked as an Entity");
        /*
         * Table ClassName for relationElement
         */
        ClassName relationElementTableClassName = asTableClassName(relationElement, processingEnvironment);
        /*
         * Variable name for relationElementTableClassName
         */
        String relationElementTableVarName = camelCase(getClassName(relationElement));
        /*
         * Name of relationElement
         */
        String relationElementVarName = relationElement.getSimpleName().toString();
        /*
         * name of relation element for collection types
         */
        String relationElementVarNames = pluralize(relationElementVarName);

        /*
         * adding field for Table of child relation element
         */

        constructorParameters.add(ParameterSpec.builder(relationElementTableClassName, relationElementTableVarName).build());
        classBuilder.addField(FieldSpec.builder(relationElementTableClassName, relationElementTableVarName)
            .addModifiers(Modifier.PRIVATE)
            .build());

        classBuilder.addMethod(MethodSpec.methodBuilder("listWith" + capitalizeFirst(hasManyRelationFieldName))
            .addModifiers(Modifier.PUBLIC)
            .returns(ParameterizedTypeName.get(ClassName.get("promise.model", "IdentifiableList"),
                WildcardTypeName.subtypeOf(
                    ClassName.get(element.asType())
                )
            )).addCode(CodeBlock.builder()
                .addStatement("IdentifiableList<? extends $T> " + parentElementVarNames + " = " + getParentElementTableVarName + ".findAll()", ClassName.get(element.asType()))
                .beginControlFlow(parentElementVarNames + ".forEach(new $T<$T>()",
                    ClassName.get("java.util.function", "Consumer"),
                    ClassName.get(element.asType()))
                .add("@Override\n")
                .beginControlFlow("public void accept($T " + parentElementVarName + ")",
                    ClassName.get(element.asType()))
                .addStatement(parentElementVarName + ".set" + capitalizeFirst(hasManyRelationFieldName) + "(new $T<>(get" + capitalizeFirst(hasManyRelationFieldName) + "(" + parentElementVarName + ")))",
                    ClassName.get("promise.model", "IdentifiableList"))
                .endControlFlow()
                .endControlFlow(")")
                .addStatement("return " + parentElementVarNames)
                .build())
            .build());

        classBuilder.addMethod(MethodSpec.methodBuilder("listWith" + capitalizeFirst(hasManyRelationFieldName))
            .addParameter(ArrayTypeName.of(
                ParameterizedTypeName.get(ClassName.get("promise.db", "Column"),
                    WildcardTypeName.subtypeOf(TypeName.OBJECT))
            ), "columns")
            .addModifiers(Modifier.PUBLIC)
            .returns(ParameterizedTypeName.get(ClassName.get("promise.model", "IdentifiableList"),
                WildcardTypeName.subtypeOf(
                    ClassName.get(element.asType())
                )
            )).addCode(CodeBlock.builder()
                .addStatement("IdentifiableList<? extends $T> " + parentElementVarNames + " = " + getParentElementTableVarName + ".findAll(columns)", ClassName.get(element.asType()))
                .beginControlFlow(parentElementVarNames + ".forEach(new $T<$T>()",
                    ClassName.get("java.util.function", "Consumer"),
                    ClassName.get(element.asType()))
                .add("@Override\n")
                .beginControlFlow("public void accept($T " + parentElementVarName + ")",
                    ClassName.get(element.asType()))
                .addStatement(parentElementVarName + ".set" + capitalizeFirst(hasManyRelationFieldName) + "(new $T<>(get" + capitalizeFirst(hasManyRelationFieldName) + "(" + parentElementVarName + ")))",
                    ClassName.get("promise.model", "IdentifiableList"))
                .endControlFlow()
                .endControlFlow(")")
                .addStatement("return " + parentElementVarNames)
                .build())
            .build());

        VariableElement relationHasOneRelatedElement = null;
        for (VariableElement elem : ElementFilter.fieldsIn(relationElement.getEnclosedElements()))
          if (elem.getAnnotation(HasOne.class) != null && JavaUtils.isTypeEqual(elem.asType(), TypeName.get(element.asType())))
            relationHasOneRelatedElement = elem;
        if (relationHasOneRelatedElement == null)
          throw new Exception(relationElement.getSimpleName() + "is marked as HasMany in  " + hasManyRelationFieldElement.getSimpleName() + " for " + element.getSimpleName() + " but it has no as HasOne relation field for " + element.getSimpleName());

        classBuilder.addMethod(MethodSpec.methodBuilder("get" + capitalizeFirst(hasManyRelationFieldName))
            .addParameter(TypeName.get(element.asType()), parentElementVarName)
            .addModifiers(Modifier.PUBLIC)
            .returns(ParameterizedTypeName.get(ClassName.get("promise.model", "IdentifiableList"),
                WildcardTypeName.subtypeOf(
                    ClassName.get(relationElement.asType())
                )
            ))
            .addCode(CodeBlock.builder()
                .addStatement("return " + relationElementTableVarName + ".findAll($T." + relationHasOneRelatedElement.getSimpleName() + "Column.with(" + parentElementVarName + ".getId()))", relationElementTableClassName)
                .build())
            .build());

        classBuilder.addMethod(MethodSpec.methodBuilder("delete" + capitalizeFirst(hasManyRelationFieldName))
            .returns(boolean.class)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(TypeName.get(element.asType()), parentElementVarName)
            .addCode(CodeBlock.builder()
                .addStatement("return " + relationElementTableVarName + ".delete($T." + relationHasOneRelatedElement.getSimpleName() + "Column.with(" + parentElementVarName + ".getId()))", relationElementTableClassName)
                .build())
            .build());

        classBuilder.addMethod(MethodSpec.methodBuilder("saveWith" + capitalizeFirst(hasManyRelationFieldName))
            .addParameter(TypeName.get(element.asType()), parentElementVarName)
            .addModifiers(Modifier.PUBLIC)
            .returns(long.class)
            .addCode(CodeBlock.builder()
                .addStatement("long id = " + getParentElementTableVarName + ".save(" + parentElementVarName + ")")
                .beginControlFlow(parentElementVarName + ".get" + capitalizeFirst(hasManyRelationFieldName) + "().forEach(new $T<$T>()",
                    ClassName.get("java.util.function", "Consumer"),
                    TypeName.get(relationElement.asType()))
                .add("@Override\n")
                .beginControlFlow("public void accept($T " + camelCase(relationElementVarName) + ")", TypeName.get(relationElement.asType()))
                .addStatement("$T " + parentElementVarName + "1 = new $T()",
                    TypeName.get(element.asType()),
                    TypeName.get(element.asType()))
                .addStatement(parentElementVarName + "1.setId((int) id)")
                .addStatement(camelCase(relationElementVarName) + ".set" + capitalizeFirst(relationHasOneRelatedElement.getSimpleName().toString()) + "(" + parentElementVarName + "1)")
                .endControlFlow()
                .endControlFlow(")")
                .addStatement(relationElementTableVarName + ".save(new $T<>(" + parentElementVarName + ".get" + capitalizeFirst(hasManyRelationFieldName) + "()))",
                    ClassName.get("promise.model", "IdentifiableList"))
                .addStatement("return id")
                .build())
            .build());

        classBuilder.addMethod(MethodSpec.methodBuilder("saveWith" + capitalizeFirst(hasManyRelationFieldName))
            .addParameter(ParameterizedTypeName.get(
                ClassName.get("java.util", "List"),
                WildcardTypeName.subtypeOf(TypeName.get(element.asType()))),
                parentElementVarNames)
            .addModifiers(Modifier.PUBLIC)
            .returns(boolean.class)
            .addCode(CodeBlock.builder()
                .beginControlFlow(parentElementVarNames + ".forEach(new $T<$T>()",
                    ClassName.get("java.util.function", "Consumer"),
                    TypeName.get(element.asType()))
                .add("@Override\n")
                .beginControlFlow("public void accept($T " + parentElementVarName + ")", TypeName.get(element.asType()))
                .addStatement("saveWith" + capitalizeFirst(hasManyRelationFieldName) + "(" + parentElementVarName + ")")
                .endControlFlow()
                .endControlFlow(")")
                .addStatement("return true")
                .build())
            .build());

        classBuilder.addMethod(MethodSpec.methodBuilder("updateWith" + capitalizeFirst(hasManyRelationFieldName))
            .addModifiers(Modifier.PUBLIC)
            .addParameter(TypeName.get(element.asType()), parentElementVarName)
            .returns(boolean.class)
            .addStatement("return (delete" + capitalizeFirst(hasManyRelationFieldName) + "(" + parentElementVarName + ") && saveWith" + capitalizeFirst(hasManyRelationFieldName) + "(" + parentElementVarName + ") > 0)")
            .build());

        return new Pair<>(relationElementTableVarName, relationElementTableVarName);
      }
    }
    return null;
  }

  private void generateForHasOneRelation(Element hasOneRelationFieldElement) {

  }
}
