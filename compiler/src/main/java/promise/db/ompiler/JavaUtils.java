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
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
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
    codeBlock.addStatement("if (entityClass == $T.class) return getDatabaseInstance().obtain($T.class)",
        ClassName.get(pack, entity.getSimpleName().toString()),
        ClassName.get(pack, PersistableEntityUtilsKt.getClassName(entity)));
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
    return otherType.toString().equals(TypeName.get(typeMirror).toString());
  }

  static boolean isSubtypeOfType(TypeMirror typeMirror, TypeName otherType) {
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
      if (typeString.toString().equals(otherType)) {
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
