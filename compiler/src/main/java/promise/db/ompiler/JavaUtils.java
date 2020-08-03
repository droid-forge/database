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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.sun.tools.javac.jvm.Code;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public class JavaUtils {
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

  public static FieldSpec generateEntityTableLogField(Element element) {
    return FieldSpec.builder(
        String.class, "TAG")
        .addModifiers(Modifier.STATIC, Modifier.PRIVATE, Modifier.FINAL)
        .initializer("$T.makeTag(" +element.getSimpleName() +".class)",
            ClassName.get("promise.commons.data.log","LogUtil" ))
        .build();
  }

  public static void generateAddConverterStatementInDatabaseConstructorMethod(
      ProcessingEnvironment processingEnvironment,
      MethodSpec.Builder constructorMethod,
      TypeElement converter) {
    String pack = processingEnvironment.getElementUtils().getPackageOf(converter).toString();
    constructorMethod.addStatement("this.typeConverter = $T.provider($T.create(new $T())).get()",
        ClassName.get("promise.commons", "SingletonInstanceProvider"),
        ClassName.get(pack, TypeConverterProcessorKt.getTypeConverterClassName(converter)),
        UtilsKt.toTypeName(converter));
  }

  public static void generateIfStatementObtainClassString(
      ProcessingEnvironment processingEnv,
      CodeBlock.Builder codeBlock,
      TypeElement entity) {
    String pack = processingEnv.getElementUtils().getPackageOf(entity).toString();
    if (PersistableEntityUtilsKt.checkIfAnyElementNeedsTypeConverter(entity)) {
      String tableVarName = PersistableEntityUtilsKt.camelCase(PersistableEntityUtilsKt.getClassName(entity));
      String stmt = "if (entityClass == Exam.class) {\n" +
          "      ExamsTable examsTable = getDatabaseInstance().obtain(ExamsTable.class);\n" +
          "      examsTable.setTypeConverter(typeConverter);\n" +
          "      return (FastTable<T>) examsTable;\n" +
          "    }";
      codeBlock.beginControlFlow("if (entityClass == $T.class)", ClassName.get(pack, entity.getSimpleName().toString()));
      codeBlock.addStatement("$T "+tableVarName+" = getDatabaseInstance().obtain($T.class)",
          ClassName.get(pack, PersistableEntityUtilsKt.getClassName(entity)),
          ClassName.get(pack, PersistableEntityUtilsKt.getClassName(entity)));
      codeBlock.addStatement(tableVarName + ".setTypeConverter(typeConverter)");
      codeBlock.addStatement("return (FastTable<T>) "+tableVarName);
      codeBlock.endControlFlow();
    }
    else codeBlock.addStatement("if (entityClass == $T.class) return getDatabaseInstance().obtain($T.class)",
        ClassName.get(pack, entity.getSimpleName().toString()),
        ClassName.get(pack, PersistableEntityUtilsKt.getClassName(entity)));
  }

  public static CodeBlock generateSerializerRelationPutStatement(Element element, String colName) {
    CodeBlock.Builder codeBlock =  CodeBlock.builder();
    String variableName = PersistableEntityUtilsKt.camelCase(element.getSimpleName().toString());
    codeBlock.addStatement("$T "+ variableName+ " = t.get"+PersistableEntityUtilsKt.capitalizeFirst(element.getSimpleName().toString())+"()", TypeName.get(element.asType()));
    codeBlock.beginControlFlow("if("+variableName+" != null)");
    codeBlock.addStatement("values.put("+colName+".getName(), "+variableName+".getId())");
    codeBlock.endControlFlow();
    codeBlock.beginControlFlow("else");
    codeBlock.addStatement("values.put("+colName+".getName(), 0)");
    codeBlock.endControlFlow();
    return codeBlock.build();
  }

  public static CodeBlock generateDeserializerRelationSetStatement(
      String entitySetName,
      Element element,
      String colName) {
    String gen = "" +
        " int personId = e.getInt(personColumn.getIndex(e));\n" +
        "      if (personId != 0) {\n" +
        "        Person person = new Person();\n" +
        "        person.setId(personId);\n" +
        "        dog.setPerson(person);\n" +
        "      }";
    CodeBlock.Builder codeBlock =  CodeBlock.builder();
    String variableName = PersistableEntityUtilsKt.camelCase(element.getSimpleName().toString());
    String variableNameId = variableName + "Id";
    codeBlock.addStatement("int "+ variableNameId+ " = e.getInt("+colName+".getIndex(e))");
    codeBlock.beginControlFlow("if("+variableNameId+" != 0)");
    codeBlock.addStatement("$T "+variableName+ " = new $T()",
        TypeName.get(element.asType()),
        TypeName.get(element.asType()));
    codeBlock.addStatement(variableName + ".setId("+variableNameId+")");
    codeBlock.addStatement(entitySetName+".set"+PersistableEntityUtilsKt.capitalizeFirst(element.getSimpleName().toString())+"("+variableName+")");
    codeBlock.endControlFlow();
    return codeBlock.build();
  }

  public static void generateCatchSQliteExceptionBlockForDeserializer(CodeBlock.Builder codeBlock, String typeDataType) {
    codeBlock.beginControlFlow("catch($T ex)",
        ClassName.get("android.database", "CursorIndexOutOfBoundsException"));
    codeBlock.addStatement("LogUtil.e(TAG, \"deserialize\", ex)");
    codeBlock.addStatement("return new "+typeDataType+"()");
  }

  public static void generateDatabaseMigrationOverrideControlBlock(CodeBlock.Builder codeBlock) {
    codeBlock.add("@Override \n");
    codeBlock.beginControlFlow("public void onMigrate(FastDatabase database, $T sqLiteDatabase, int oldVersion, int newVersion)", ClassName.get("android.database.sqlite", "SQLiteDatabase"));
  }

  // safe because both Long.class and long.class are of type Class<Long>
  @SuppressWarnings("unchecked")
  public static <T> Class<T> wrap(Class<T> c) {
    return c.isPrimitive() ? (Class<T>) PRIMITIVES_TO_WRAPPERS.get(c) : c;
  }


  public static TypeElement getSuperClass(TypeElement typeElement) {
    TypeMirror type = typeElement.getSuperclass();
    if (type.getKind() == TypeKind.NONE) {
      return null;
    }
    return (TypeElement) ((DeclaredType) type).asElement();
  }

  static boolean isTypeEqual(TypeMirror typeMirror, TypeName otherType) {
    if (typeMirror.getKind() == TypeKind.NONE || otherType == TypeName.VOID ||
    typeMirror.getKind() == TypeKind.VOID) {
      return false;
    }
    return otherType.toString().equals(TypeName.get(typeMirror).toString());
  }

  static boolean isSubtypeOfType(TypeMirror typeMirror, TypeName otherType) {
    if (typeMirror.getKind() == TypeKind.NONE) {
      return false;
    }
    if (isTypeEqual(typeMirror, otherType)) {
      return true;
    }
    if (typeMirror.getKind() != TypeKind.DECLARED) {
      return false;
    }
    DeclaredType declaredType = (DeclaredType) typeMirror;
    List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
    if (typeArguments.size() > 0) {
      StringBuilder typeString = new StringBuilder(declaredType.asElement().toString());
      typeString.append('<');
      for (int i = 0; i < typeArguments.size(); i++) {
        if (i > 0) {
          typeString.append(',');
        }
        typeString.append('?');
      }
      typeString.append('>');
      if (typeString.toString().equals(otherType.toString())) {
        return true;
      }
    }
    Element element = declaredType.asElement();
    if (!(element instanceof TypeElement)) {
      return false;
    }
    TypeElement typeElement = (TypeElement) element;
    TypeMirror superType = typeElement.getSuperclass();
    if (isSubtypeOfType(superType, otherType)) {
      return true;
    }
    for (TypeMirror interfaceType : typeElement.getInterfaces()) {
      if (isSubtypeOfType(interfaceType, otherType)) {
        return true;
      }
    }
    return false;
  }

  public static boolean extendsClass(
      TypeElement myTypeElement,
      TypeMirror desiredInterface) {
    return myTypeElement.getSuperclass().toString().equals(desiredInterface.toString());

  }
}
