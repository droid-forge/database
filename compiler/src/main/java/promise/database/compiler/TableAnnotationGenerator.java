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

package promise.database.compiler;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;

import java.util.ArrayList;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import promise.database.Entity;
import promise.database.Table;
import promise.database.compiler.utils.LogUtil;
import promise.database.compiler.utils.LogUtil;
import promise.database.compiler.utils.PersistableEntityUtilsKt;

public class TableAnnotationGenerator implements CodeGenerator<AnnotationSpec> {
  private Element element;
  private ProcessingEnvironment processingEnvironment;

  public TableAnnotationGenerator(Element element, ProcessingEnvironment processingEnvironment) {
    this.element = element;
    this.processingEnvironment = processingEnvironment;
  }

  private String getCompoundIndexAnnotation(Entity.CompoundIndex annotation) {
    StringBuilder stmt = new StringBuilder("@Table.CompoundIndex(indexes = {\n");
    for (int i = 0; i < annotation.columns().length; i++) {
      String index = annotation.columns()[i];
      stmt.append("@Table.Index(columnName = \"").append(index).append("\")");
      if (i != annotation.columns().length - 1) stmt.append(",\n");
    }
    if (annotation.unique()) stmt.append("},\n" +
        "unique = true\n" +
        ")");
    else stmt.append("\n})");
    return stmt.toString();
  }

  @Override
  public AnnotationSpec generate() throws Exception {

    AnnotationSpec.Builder annotationSpec = AnnotationSpec.builder(Table.class);
    Entity.CompoundIndex[] compoundIndices = PersistableEntityUtilsKt.getTableCompoundIndices((TypeElement) element);

    StringBuilder stmt = new StringBuilder("{\n");
    if (compoundIndices.length > 0) try {
      for (int index = 0; index < compoundIndices.length; index++) {
        Entity.CompoundIndex compoundIndex = compoundIndices[index];
        stmt.append(getCompoundIndexAnnotation(compoundIndex));
        if (index != compoundIndices.length - 1) stmt.append(",\n");
      }
      stmt.append("}\n");
      annotationSpec.addMember("compoundIndexes", CodeBlock.of(stmt.toString()));
    } catch (Throwable e) {
      LogUtil.e(e);
    }
    String[] indices = PersistableEntityUtilsKt.getTableIndices((TypeElement) element);
    if (indices != null) {
      StringBuilder stmt2 = new StringBuilder("{\n");
      try {
        for (int i = 0; i < indices.length; i++) {
          stmt2.append("@Table.Index(columnName = \"").append(indices[i]).append("\")\n");
          if (i != indices.length - 1) stmt2.append(",\n");
        }
        stmt2.append("}\n");
        annotationSpec.addMember("indices", CodeBlock.of(stmt2.toString()));
      } catch (Throwable e) {
        LogUtil.e(e);
      }
    }
    Map<String, String>[] foreignKeys = PersistableEntityUtilsKt.getTableForeignKeys((TypeElement) element, processingEnvironment);
    if (foreignKeys != null) {
      StringBuilder stmt3 = new StringBuilder("{\n");
      try {
        for (int i = 0; i < foreignKeys.length; i++) {
          StringBuilder stmt4 = new StringBuilder("@Table.ForeignKey(");
          Map<String, String> pairs = foreignKeys[i];
          for (int j = 0; j < pairs.entrySet().size(); j++) {
            Map.Entry<String, String> entry = new ArrayList<>(pairs.entrySet()).get(j);
            stmt4.append(entry.getKey()).append(" = \"").append(entry.getValue()).append("\"");
            if (j != pairs.entrySet().size() - 1) stmt4.append(", ");
          }
          stmt4.append(")");
          stmt3.append(stmt4);
          if (i != foreignKeys.length - 1) stmt3.append(", \n");
        }

        stmt3.append("}\n");
        annotationSpec.addMember("foreignKeys", CodeBlock.of(stmt3.toString()));
      } catch (Throwable e) {
        LogUtil.e(e);
      }
    }
    annotationSpec.addMember("tableName", "$S", PersistableEntityUtilsKt.getTableName(element));
    return annotationSpec.build();
  }
}
