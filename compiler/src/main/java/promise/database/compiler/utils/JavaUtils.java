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

package promise.database.compiler.utils;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;

@SuppressWarnings("WeakerAccess")
public class JavaUtils {

  // code generation participants

  // end of code generation participants
  private static final Map<Class<?>, Class<?>> PRIMITIVES_TO_WRAPPERS
      = new HashMap<Class<?>, Class<?>>() {{
    put(boolean.class, Boolean.class);
    put(byte.class, Byte.class);
    put(char.class, Character.class);
    put(double.class, Double.class);
    put(float.class, Float.class);
    put(int.class, Integer.class);
    put(long.class, Long.class);
    put(short.class, Short.class);
    put(void.class, Void.class);
  }};

  /**
   * @param element
   * @return
   */
  public static FieldSpec generateEntityTableLogField(Element element) {
    return FieldSpec.builder(
        String.class, "TAG")
        .addModifiers(Modifier.STATIC, Modifier.PRIVATE, Modifier.FINAL)
        .initializer("$T.makeTag(" + element.getSimpleName() + ".class)",
            ClassName.get("promise.commons.data.log", "LogUtil"))
        .build();
  }

  public static void generateAddConverterStatementInDatabaseConstructorMethod(
      ProcessingEnvironment processingEnvironment,
      MethodSpec.Builder constructorMethod,
      TypeElement converter) {
    String pack = processingEnvironment.getElementUtils().getPackageOf(converter).toString();
    constructorMethod.addStatement("this.typeConverter = $T.provider($T.create(new $T())).get()",
        ClassName.get("promise.commons", "SingletonInstanceProvider"),
        ClassName.get(pack, UtilsKt.getInstanceProviderClassName(converter.getSimpleName().toString())),
        UtilsKt.toTypeName(converter));
  }

  public static CodeBlock.Builder generateGetRelationDaoCodeBlock(Map.Entry<ClassName, HashMap<ClassName, Element>> relationDao) {
    ClassName providerClassName = ClassName.get(relationDao.getKey().packageName(),
        UtilsKt.getInstanceProviderClassName(relationDao.getKey().simpleName()));
    CodeBlock.Builder codeBlock = CodeBlock.builder();
    codeBlock.add("return $T.provider($T.create(new $T.Builder()\n",
        ClassName.get("promise.commons", "SingletonInstanceProvider"),
        providerClassName,
        relationDao.getKey());
    for (Map.Entry<ClassName, Element> tableAndEntity : relationDao.getValue().entrySet())
      codeBlock.add(".set" + tableAndEntity.getKey().simpleName() + "(get" + tableAndEntity.getKey().simpleName() + "())\n");
    codeBlock.addStatement(".build())).get()");
    return codeBlock;
  }

  public static void generateIfStatementObtainClassString(
      ProcessingEnvironment processingEnv,
      CodeBlock.Builder codeBlock,
      TypeElement entity) {
    String pack = processingEnv.getElementUtils().getPackageOf(entity).toString();
    if (PersistableEntityUtilsKt.checkIfAnyElementNeedsTypeConverter(entity)) {
      String tableVarName = PersistableEntityUtilsKt.camelCase(PersistableEntityUtilsKt.getTableClassNameString(entity));
      codeBlock.beginControlFlow("if (entityClass == $T.class)", ClassName.get(pack, entity.getSimpleName().toString()));
      codeBlock.addStatement("$T " + tableVarName + " = getDatabaseInstance().obtain($T.class)",
          ClassName.get(pack, PersistableEntityUtilsKt.getTableClassNameString(entity)),
          ClassName.get(pack, PersistableEntityUtilsKt.getTableClassNameString(entity)));
      codeBlock.addStatement(tableVarName + ".setTypeConverter(typeConverter)");
      codeBlock.addStatement("return (FastTable<T>) " + tableVarName);
      codeBlock.endControlFlow();
    } else
      codeBlock.addStatement("if (entityClass == $T.class) return getDatabaseInstance().obtain($T.class)",
          ClassName.get(pack, entity.getSimpleName().toString()),
          ClassName.get(pack, PersistableEntityUtilsKt.getTableClassNameString(entity)));
  }

  public static CodeBlock generateGetTableStatement(
      ProcessingEnvironment processingEnv,
      TypeElement entity) {
    String pack = processingEnv.getElementUtils().getPackageOf(entity).toString();
    return CodeBlock.builder()
        .addStatement("return ($T) tableOf($T.class)",
            ClassName.get(pack, PersistableEntityUtilsKt.getTableClassNameString(entity)),
            TypeName.get(entity.asType()))
        .build();
  }

  public static CodeBlock generateSerializerRelationPutStatement(Element element, String colName) {
    CodeBlock.Builder codeBlock = CodeBlock.builder();
    String variableName = PersistableEntityUtilsKt.camelCase(element.getSimpleName().toString());
    codeBlock.addStatement("$T " + variableName + " = t.get" + PersistableEntityUtilsKt.capitalizeFirst(element.getSimpleName().toString()) + "()", TypeName.get(element.asType()));
    codeBlock.beginControlFlow("if(" + variableName + " != null)");
    codeBlock.addStatement("values.put(" + colName + ".getName(), " + variableName + ".getId())");
    codeBlock.endControlFlow();
    codeBlock.beginControlFlow("else");
    codeBlock.addStatement("values.put(" + colName + ".getName(), 0)");
    codeBlock.endControlFlow();
    return codeBlock.build();
  }

  public static CodeBlock generateDeserializerRelationSetStatement(
      String entitySetName,
      Element element,
      String colName) {
    CodeBlock.Builder codeBlock = CodeBlock.builder();
    String variableName = PersistableEntityUtilsKt.camelCase(element.getSimpleName().toString());
    String variableNameId = variableName + "Id";
    codeBlock.addStatement("int " + variableNameId + " = e.getInt(" + colName + ".getIndex(e))");
    codeBlock.beginControlFlow("if(" + variableNameId + " != 0)");
    codeBlock.addStatement("$T " + variableName + " = new $T()",
        TypeName.get(element.asType()),
        TypeName.get(element.asType()));
    codeBlock.addStatement(variableName + ".setId(" + variableNameId + ")");
    codeBlock.addStatement(entitySetName + ".set" + PersistableEntityUtilsKt.capitalizeFirst(element.getSimpleName().toString()) + "(" + variableName + ")");
    codeBlock.endControlFlow();
    return codeBlock.build();
  }

  public static void generateCatchSQliteExceptionBlockForDeserializer(CodeBlock.Builder codeBlock, String typeDataType) {
    codeBlock.beginControlFlow("catch($T ex)",
        ClassName.get("android.database", "CursorIndexOutOfBoundsException"));
    codeBlock.addStatement("LogUtil.e(TAG, \"deserialize\", ex)");
    codeBlock.addStatement("return new " + typeDataType + "()");
  }

  public static void generateDatabaseMigrationOverrideControlBlock(CodeBlock.Builder codeBlock) {
    codeBlock.add("@Override \n");
    codeBlock.beginControlFlow("public void onMigrate(FastDatabase database, $T sqLiteDatabase, int oldVersion, int newVersion)",
        ClassName.get("androidx.sqlite.db", "SupportSQLiteDatabase"));
  }

  public static CodeBlock generateReturnDaoImplInstance(ClassName daoImpl) {
    return CodeBlock.builder()
        .addStatement("return new $T(this)", daoImpl)
        .build();
  }

  // safe because both Long.class and long.class are of type Class<Long>
  @SuppressWarnings("unchecked")
  public static <T> Class<T> wrap(Class<T> c) {
    return c.isPrimitive() ? (Class<T>) PRIMITIVES_TO_WRAPPERS.get(c) : c;
  }

  public static boolean isTypeEqual(TypeMirror typeMirror, TypeName otherType) {
    if (typeMirror.getKind() == TypeKind.NONE || otherType == TypeName.VOID ||
        typeMirror.getKind() == TypeKind.VOID) {
      return false;
    }
    return otherType.toString().equals(TypeName.get(typeMirror).toString());
  }

  public static DeclaredType getDeclaredType(ProcessingEnvironment processingEnvironment, TypeElement typeElement) {
    return processingEnvironment.getTypeUtils().getDeclaredType(typeElement);
  }

  public static boolean isSubTypeOfDeclaredType(ProcessingEnvironment processingEnvironment, Element typeElement, DeclaredType declaredType) {
    return processingEnvironment.getTypeUtils().isAssignable(typeElement.asType(), declaredType);
  }

  public static DeclaredType toWildCardType(ProcessingEnvironment processingEnvironment, TypeElement typeElement, int wildTimes) {
    WildcardType WILDCARD_TYPE_NULL = processingEnvironment.getTypeUtils().getWildcardType(null, null);
    TypeMirror[] mirrors = new TypeMirror[wildTimes];
    for (int i = 0; i < wildTimes; i++) mirrors[i] = WILDCARD_TYPE_NULL;
    return processingEnvironment.getTypeUtils().getDeclaredType(typeElement, mirrors);
  }

  public static boolean isCollectionType(ProcessingEnvironment processingEnvironment, VariableElement variableElement) {
    DeclaredType collectionType = toWildCardType(processingEnvironment,
        processingEnvironment.getElementUtils().getTypeElement(Collection.class.getName()),
        1);
    return isSubTypeOfDeclaredType(processingEnvironment, variableElement, collectionType);
  }

  public static List<? extends TypeMirror> getParameterizedTypeMirrors(VariableElement variableElement) {
    if (variableElement.asType() instanceof DeclaredType) {
      DeclaredType declaredType = (DeclaredType) variableElement.asType();
      return declaredType.getTypeArguments();
    }
    return null;
  }

  public static TypeMirror getTypeMirror(VariableElement variableElement) {
    if (variableElement.asType() instanceof DeclaredType) return variableElement.asType();
    return null;
  }
}
