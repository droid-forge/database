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

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import promise.db.Entity;
import promise.db.Table;
import promise.db.ompiler.CodeGenerator;
import promise.db.ompiler.PersistableEntityUtilsKt;

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
      if (i != annotation.columns().length - 1) {
        stmt.append(",\n");
      }
    }
    if (annotation.unique()) {
      stmt.append("},\n" +
          "unique = true\n" +
          ")");
    } else stmt.append("\n})");

    return stmt.toString();
  }

  @Override
  public AnnotationSpec generate() throws Exception {
    String gen = "@Table(\n" +
        "         tableName = \"p\",\n" +
        "         compoundIndexes = {\n" +
        "             @Table.CompoundIndex(\n" +
        "                 indexes = {\n" +
        "                     @Table.Index(columnName = \"n\"),\n" +
        "                     @Table.Index(columnName = \"m\")\n" +
        "                 }, unique = true\n" +
        "             ),\n" +
        "             @Table.CompoundIndex(\n" +
        "                 indexes = {\n" +
        "                     @Table.Index(columnName = \"a\"),\n" +
        "                     @Table.Index(columnName = \"m\")\n" +
        "                 }\n" +
        "             )\n" +
        "         },\n" +
        "         indexes = {\n" +
        "             @Table.Index(columnName = \"n\")\n" +
        "         },\n" +
        "         foreignKeys = {\n" +
        "             @Table.ForeignKey(columnName = \"m\", referencedColumnName = \"legs\", referencedTableName = \"cats\")\n" +
        "         }\n" +
        "     )";
    AnnotationSpec.Builder annotationSpec = AnnotationSpec.builder(Table.class);
    Entity.CompoundIndex[] compoundIndices = PersistableEntityUtilsKt.getTableCompoundIndices((TypeElement) element);
    //System.out.println(compoundIndices.toString());
    //this.processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, "CompoundIndices " + Arrays.toString(compoundIndices));
    StringBuilder stmt = new StringBuilder("{\n");
    if (compoundIndices.length > 0) {
      //fileBuilder.addImport("promise.db", "Table")
      try {
        for (int index = 0; index < compoundIndices.length; index++) {
          Entity.CompoundIndex compoundIndex = compoundIndices[index];
          stmt.append(getCompoundIndexAnnotation(compoundIndex));
          if (index != compoundIndices.length - 1) {
            stmt.append(",\n");
          }
        }
        stmt.append("}\n");

        annotationSpec.addMember("compoundIndexes", CodeBlock.of(stmt.toString()));
      } catch (Throwable e) {
        this.processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, "CompoundIndex gen error " + Arrays.toString(e.getStackTrace()));
      }
    }
    String[] indices = PersistableEntityUtilsKt.getTableIndices((TypeElement) element);
    if (indices != null) {
      StringBuilder stmt2 = new StringBuilder("{\n");
      try {
        for (int i = 0; i < indices.length; i++) {
          stmt2.append("@Table.Index(columnName = \"").append(indices[i]).append("\")\n");
          if (i != indices.length - 1) {
            stmt2.append(",\n");
          }
        }
        stmt2.append("}\n");
        annotationSpec.addMember("indices", CodeBlock.of(stmt2.toString()));
      } catch (Throwable e) {
        this.processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, "Index gen error " + Arrays.toString(e.getStackTrace()));
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
        this.processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, "Index gen error " + Arrays.toString(e.getStackTrace()));
      }
    }
    annotationSpec.addMember("tableName", "$S", PersistableEntityUtilsKt.getTableName(element));
    return annotationSpec.build();
  }
}
