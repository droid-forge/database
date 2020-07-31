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
import com.squareup.javapoet.TypeName;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
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

  public static boolean implementsInterface(
      ProcessingEnvironment processingEnv,
      TypeElement myTypeElement,
      TypeMirror desiredInterface) {
    boolean found = false;
    while (myTypeElement.getSuperclass()
        != processingEnv.getElementUtils().getTypeElement("java.lang.Object").asType()
        &&   !found) {
      for (TypeMirror t : myTypeElement.getInterfaces()) {
        if (processingEnv.getTypeUtils().isAssignable(t, desiredInterface)){
          found = true;
          break;
        }
        else {
          TypeElement elem = (TypeElement) processingEnv.getTypeUtils().asElement(t);
          return implementsInterface(processingEnv, elem, desiredInterface);
        }
      }
    }

    return found;
  }

  public static boolean extendsClass(
      TypeElement myTypeElement,
      TypeMirror desiredInterface) {
    return myTypeElement.getSuperclass().toString().equals(desiredInterface.toString());

  }
}
