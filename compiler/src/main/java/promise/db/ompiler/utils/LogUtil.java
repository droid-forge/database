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

package promise.db.ompiler.utils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

public class LogUtil {

  private static ProcessingEnvironment processingEnv;

  private static final String TAG = "PromiseDatabaseCompiler: ";

  public static void initLogger(ProcessingEnvironment processingEnvironment) {
    LogUtil.processingEnv = processingEnvironment;
  }

  private LogUtil() {
    //no instance
  }

  public static void w( Object... messages) {
    StringBuilder builder = new StringBuilder();
    builder.append(TAG);
    for (Object message: messages) {
      builder.append(message.toString());
    }
    processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, builder.toString());
  }

  public static void n(Object... messages) {
    StringBuilder builder = new StringBuilder();

    builder.append(TAG);
    for (Object message: messages) {
      builder.append(message.toString());
    }
    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, builder.toString());
  }

  public static void e(Throwable t) {
    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
        TAG + Utils.INSTANCE.getStackTraceString(t));
  }

  public static void e(Throwable t, Element element) {
    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
        TAG + Utils.INSTANCE.getStackTraceString(t), element);
  }

}

