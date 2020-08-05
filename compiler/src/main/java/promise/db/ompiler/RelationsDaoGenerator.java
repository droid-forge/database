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
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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
import promise.db.ompiler.utils.LogUtil;
import promise.db.ompiler.utils.PersistableEntityUtilsKt;
import promise.db.ompiler.utils.Utils;
import promise.db.ompiler.utils.UtilsKt;

import static promise.db.ompiler.utils.PersistableEntityUtilsKt.asTableClassName;
import static promise.db.ompiler.utils.PersistableEntityUtilsKt.camelCase;
import static promise.db.ompiler.utils.PersistableEntityUtilsKt.capitalizeFirst;
import static promise.db.ompiler.utils.PersistableEntityUtilsKt.getTableClassNameString;
import static promise.db.ompiler.utils.PersistableEntityUtilsKt.pluralize;

public class RelationsDaoGenerator implements CodeGenerator<List<JavaFile.Builder>> {

  public static HashMap<ClassName, HashMap<ClassName, Element>> relationsMap = new HashMap<>();
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

  public RelationsDaoGenerator(ProcessingEnvironment processingEnvironment,
                               TypeElement element,
                               List<VariableElement> relationElements) {
    this.processingEnvironment = processingEnvironment;
    this.element = element;
    this.relationElements = relationElements;
    parentElementVarName = camelCase(element.getSimpleName().toString());
    parentElementVarNames = pluralize(parentElementVarName);
    getParentElementTableVarName = camelCase(getTableClassNameString(element));
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
    HashMap<ClassName, Element> tableAndRelationElements = new HashMap<>();
    ClassName parentRelationTableClassName = PersistableEntityUtilsKt.asTableClassName(element, processingEnvironment);
    constructorParameters.add(ParameterSpec.builder(parentRelationTableClassName, getParentElementTableVarName).build());
    tableAndRelationElements.put(parentRelationTableClassName, element);
    relationElements.forEach(relationElement -> {
      if (relationElement.getAnnotation(HasMany.class) != null) try {
        Pair<ClassName, Element> pair = generateForHasManyRelation(relationElement, constructorParameters);
        if (pair != null) tableAndRelationElements.put(pair.getFirst(), pair.getSecond());
      } catch (Exception e) {
        LogUtil.e(e, relationElement);
      }
      else if (relationElement.getAnnotation(HasOne.class) != null) try {
        Pair<ClassName, Element> pair = generateForHasOneRelation(relationElement, constructorParameters);
        if (pair != null) tableAndRelationElements.put(pair.getFirst(), pair.getSecond());
      } catch (Exception e) {
        LogUtil.e(e, relationElement);
      }
    });
    CodeBlock.Builder constructorBlock = CodeBlock.builder();
    constructorParameters.forEach(parameterSpec ->
        constructorBlock.addStatement("this." + parameterSpec.name + " = " + parameterSpec.name));
    classBuilder.addMethod(MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PRIVATE)
        .addParameters(constructorParameters)
        .addCode(constructorBlock.build())
        .build());

    generateRelationsDaoBuilder(constructorParameters, pack, fileName);
    javaFiles.add(JavaFile.builder(pack, classBuilder.build()));
    javaFiles.add(Utils.INSTANCE.generateInstanceProviderHolder(ClassName.get(pack, fileName)));
    relationsMap.put(ClassName.get(pack, fileName), tableAndRelationElements);
    return javaFiles;
  }

  private Pair<ClassName, Element> generateForHasManyRelation(Element hasManyRelationFieldElement, ArrayList<ParameterSpec> constructorParameters) throws Exception {
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

        VariableElement relationHasOneRelatedElement = null;
        for (VariableElement elem : ElementFilter.fieldsIn(relationElement.getEnclosedElements()))
          if (elem.getAnnotation(HasOne.class) != null && JavaUtils.isTypeEqual(elem.asType(), TypeName.get(element.asType())))
            relationHasOneRelatedElement = elem;
        if (relationHasOneRelatedElement == null)
          throw new Exception(relationElement.getSimpleName() + " is marked as HasMany in  " + hasManyRelationFieldElement.getSimpleName() + " field for " + element.getSimpleName() + " but it is not marked as HasOne relation field in " + element.getSimpleName());
        /*
         * Table ClassName for relationElement
         */
        ClassName relationElementTableClassName = asTableClassName(relationElement, processingEnvironment);
        /*
         * Variable name for relationElementTableClassName
         */
        String relationElementTableVarName = camelCase(getTableClassNameString(relationElement));
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

        classBuilder.addMethod(MethodSpec.methodBuilder("paginateWith" + capitalizeFirst(hasManyRelationFieldName))
            .addModifiers(Modifier.PUBLIC)
            .addParameter(int.class, "skip")
            .addParameter(int.class, "limit")
            .returns(ParameterizedTypeName.get(ClassName.get("promise.model", "IdentifiableList"),
                WildcardTypeName.subtypeOf(
                    ClassName.get(element.asType())
                )
            )).addCode(CodeBlock.builder()
                .addStatement("IdentifiableList<? extends $T> " + parentElementVarNames + " = " + getParentElementTableVarName + ".find().paginateDescending(skip, limit)", ClassName.get(element.asType()))
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

        return new Pair<>(relationElementTableClassName, relationElement);
      }
    } else
      throw new Exception(hasManyRelationFieldElement.getSimpleName() + " in " + element.getSimpleName() + " is marked as HasMany but is not a collection type");
    return null;
  }

  private Pair<ClassName, Element> generateForHasOneRelation(Element hasOneRelationFieldElement, ArrayList<ParameterSpec> constructorParameters) throws Exception {
    if (!JavaUtils.isCollectionType(processingEnvironment, (VariableElement) hasOneRelationFieldElement)) {
      TypeMirror typeMirror = JavaUtils.getTypeMirror((VariableElement) hasOneRelationFieldElement);
      if (typeMirror != null) {
        /*
         * Field name for the relation
         */
        String hasOneRelationFieldName = hasOneRelationFieldElement.getSimpleName().toString();
        /*
         * Element that this element is related to by HasOne relation
         */
        TypeElement relationElement = UtilsKt.asTypeElement(typeMirror, processingEnvironment);

        if (relationElement.getAnnotation(Entity.class) == null)
          throw new Exception("Element related to " + element.getSimpleName() + " as HasOne in " + hasOneRelationFieldName + " field is not marked as an Entity");
        /*
         * Table ClassName for relationElement
         */
        ClassName relationElementTableClassName = asTableClassName(relationElement, processingEnvironment);
        /*
         * Variable name for relationElementTableClassName
         */
        String relationElementTableVarName = camelCase(getTableClassNameString(relationElement));
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


        classBuilder.addMethod(MethodSpec.methodBuilder("paginateWith" + relationElementVarNames)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(int.class, "skip")
            .addParameter(int.class, "limit")
            .returns(ParameterizedTypeName.get(ClassName.get("promise.model", "IdentifiableList"),
                WildcardTypeName.subtypeOf(
                    ClassName.get(element.asType())
                )
            ))
            .addCode(CodeBlock.builder()
                .addStatement("$T " + parentElementVarNames + " = " + getParentElementTableVarName + ".find().paginateDescending(skip, limit)",
                    ParameterizedTypeName.get(ClassName.get("promise.model", "IdentifiableList"),
                        WildcardTypeName.subtypeOf(
                            ClassName.get(element.asType())
                        )
                    ))
                .addStatement("return populateWith" + capitalizeFirst(relationElementVarNames) + "(" + parentElementVarNames + ")")
                .build())
            .build());


        classBuilder.addMethod(MethodSpec.methodBuilder("listWith" + relationElementVarNames)
            .addModifiers(Modifier.PUBLIC)
            .returns(ParameterizedTypeName.get(ClassName.get("promise.model", "IdentifiableList"),
                WildcardTypeName.subtypeOf(
                    ClassName.get(element.asType())
                )
            ))
            .addCode(CodeBlock.builder()
                .addStatement("$T " + parentElementVarNames + " = " + getParentElementTableVarName + ".findAll()",
                    ParameterizedTypeName.get(ClassName.get("promise.model", "IdentifiableList"),
                        WildcardTypeName.subtypeOf(
                            ClassName.get(element.asType())
                        )
                    ))
                .addStatement("return populateWith" + capitalizeFirst(relationElementVarNames) + "(" + parentElementVarNames + ")")
                .build())
            .build());

        classBuilder.addMethod(MethodSpec.methodBuilder("listWith" + relationElementVarNames)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(ArrayTypeName.of(
                ParameterizedTypeName.get(ClassName.get("promise.db", "Column"),
                    WildcardTypeName.subtypeOf(TypeName.OBJECT))
            ), "columns")
            .returns(ParameterizedTypeName.get(ClassName.get("promise.model", "IdentifiableList"),
                WildcardTypeName.subtypeOf(
                    ClassName.get(element.asType())
                )
            ))
            .addCode(CodeBlock.builder()
                .addStatement("$T " + parentElementVarNames + " = " + getParentElementTableVarName + ".findAll(columns)",
                    ParameterizedTypeName.get(ClassName.get("promise.model", "IdentifiableList"),
                        WildcardTypeName.subtypeOf(
                            ClassName.get(element.asType())
                        )
                    ))
                .addStatement("return populateWith" + capitalizeFirst(relationElementVarNames) + "(" + parentElementVarNames + ")")
                .build())
            .build());

        classBuilder.addMethod(MethodSpec.methodBuilder("populateWith" + relationElementVarNames)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(ParameterizedTypeName.get(ClassName.get("promise.model", "IdentifiableList"),
                WildcardTypeName.subtypeOf(
                    ClassName.get(element.asType())
                )
            ), parentElementVarNames)
            .returns(ParameterizedTypeName.get(ClassName.get("promise.model", "IdentifiableList"),
                WildcardTypeName.subtypeOf(
                    ClassName.get(element.asType())
                )
            ))
            .addCode(CodeBlock.builder()
                .beginControlFlow(parentElementVarNames + ".forEach(new $T<$T>()",
                    ClassName.get("java.util.function", "Consumer"),
                    TypeName.get(element.asType()))
                .add("@Override\n")
                .beginControlFlow("public void accept($T " + parentElementVarName + ")",
                    TypeName.get(element.asType()))
                .addStatement(parentElementVarName + ".set" + capitalizeFirst(hasOneRelationFieldName) + "(get" + capitalizeFirst(hasOneRelationFieldName) + "(" + parentElementVarName + "))")
                .endControlFlow()
                .endControlFlow(")")
                .addStatement("return " + parentElementVarNames + "")
                .build())
            .build());

        classBuilder.addMethod(MethodSpec.methodBuilder("saveWith" + capitalizeFirst(hasOneRelationFieldName))
            .returns(long.class)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(TypeName.get(element.asType()), parentElementVarName)
            .addCode(CodeBlock.builder()
                .addStatement("long " + camelCase(relationElementVarName) + "Id = " + relationElementTableVarName + ".save(" + parentElementVarName + ".get" + capitalizeFirst(hasOneRelationFieldName) + "())")
                .addStatement("$T " + camelCase(relationElementVarName) + " = new $T()", relationElement, relationElement)
                .addStatement(camelCase(relationElementVarName) + ".setId((int) " + camelCase(relationElementVarName) + "Id)")
                .addStatement(parentElementVarName + ".set" + capitalizeFirst(hasOneRelationFieldName) + "(" + camelCase(relationElementVarName) + ")")
                .addStatement("return " + getParentElementTableVarName + ".save(" + parentElementVarName + ")")
                .build())
            .build());


        classBuilder.addMethod(MethodSpec.methodBuilder("get" + relationElementVarName)
            .addParameter(TypeName.get(element.asType()), parentElementVarName)
            .addModifiers(Modifier.PUBLIC)
            .returns(TypeName.get(relationElement.asType()))
            .addCode(CodeBlock.builder()
                .addStatement("return " + relationElementTableVarName + ".findById(" + parentElementVarName + ".get" + capitalizeFirst(hasOneRelationFieldName) + "().getId())")
                .build())
            .build());


        classBuilder.addMethod(MethodSpec.methodBuilder("saveWith" + relationElementVarNames)
            .addModifiers(Modifier.PUBLIC)
            .returns(boolean.class)
            .addParameter(ParameterizedTypeName.get(ClassName.get("promise.model", "IdentifiableList"),
                WildcardTypeName.subtypeOf(
                    ClassName.get(element.asType())
                )
            ), parentElementVarNames)
            .addCode(CodeBlock.builder()
                .beginControlFlow(parentElementVarNames + ".forEach(new $T<$T>()",
                    ClassName.get("java.util.function", "Consumer"),
                    TypeName.get(element.asType()))
                .add("@Override\n")
                .beginControlFlow("public void accept($T " + parentElementVarName + ")",
                    TypeName.get(element.asType()))
                .addStatement("saveWith"+capitalizeFirst(hasOneRelationFieldName)+"("+parentElementVarName+")")
                .endControlFlow()
                .endControlFlow(")")
                .addStatement("return true")
                .build())
            .build());

        classBuilder.addMethod(MethodSpec.methodBuilder("updateWith" + capitalizeFirst(hasOneRelationFieldName))
            .addModifiers(Modifier.PUBLIC)
            .addParameter(TypeName.get(element.asType()), parentElementVarName)
            .addCode(CodeBlock.builder()
                .addStatement("return "+getParentElementTableVarName+".update("+parentElementVarName+") &&\n" +
                    " "+relationElementTableVarName+".update("+parentElementVarName+".get"+capitalizeFirst(hasOneRelationFieldName)+"())")
                .build())
            .returns(boolean.class)
            .build());

        return new Pair<>(relationElementTableClassName, relationElement);
      }

    } else
      throw new Exception(hasOneRelationFieldElement.getSimpleName() + " in " + element.getSimpleName() + " is marked as HasOne but is a collection type");
    return null;

  }

  private void generateRelationsDaoBuilder(ArrayList<ParameterSpec> constructorParameters,
                                           String pack,
                                           String fileName) {

    TypeSpec.Builder builderClass = TypeSpec.classBuilder("Builder")
        .addModifiers(Modifier.FINAL, Modifier.STATIC, Modifier.PUBLIC);

    builderClass.addFields(constructorParameters.stream().map(parameterSpec ->
        FieldSpec.builder(parameterSpec.type, parameterSpec.name)
            .build()).collect(Collectors.toList()));

    builderClass.addMethods(constructorParameters.stream().map(parameterSpec ->
        MethodSpec.methodBuilder("set" + capitalizeFirst(parameterSpec.name))
            .addParameter(parameterSpec)
            .addModifiers(Modifier.PUBLIC)
            .addCode(CodeBlock.builder()
                .addStatement("this." + parameterSpec.name + " = " + parameterSpec.name)
                .addStatement("return this")
                .build())
            .returns(ClassName.get(pack, fileName + ".Builder"))
            .build()).collect(Collectors.toList()));
    StringBuilder returnBuilderStatement = new StringBuilder("return new $T(");
    for (int i = 0; i < constructorParameters.size(); i++) {
      returnBuilderStatement.append(constructorParameters.get(i).name);
      if (i != constructorParameters.size() - 1) returnBuilderStatement.append(", ");
    }
    returnBuilderStatement.append(")");
    builderClass.addMethod(MethodSpec.methodBuilder("build")
        .addModifiers(Modifier.PUBLIC)
        .returns(ClassName.get(pack, fileName))
        .addCode(CodeBlock.builder()
            .addStatement(returnBuilderStatement.toString(), ClassName.get(pack, fileName))
            .build())
        .build());

    classBuilder.addType(builderClass.build());
  }
}
