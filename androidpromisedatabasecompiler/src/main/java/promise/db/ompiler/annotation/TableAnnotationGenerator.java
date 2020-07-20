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

package promise.db.ompiler.annotation;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;

import java.util.Arrays;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import promise.db.Entity;
import promise.db.Table;
import promise.db.ompiler.CodeBlockGenerator;
import promise.db.ompiler.PersistableEntityUtilsKt;

public class TableAnnotationGenerator implements CodeBlockGenerator<AnnotationSpec> {
  private Element element;
  private ProcessingEnvironment processingEnvironment;

  public TableAnnotationGenerator(Element element, ProcessingEnvironment processingEnvironment) {
    this.element = element;
    this.processingEnvironment = processingEnvironment;
  }

  private String getCompoundIndexAnnotation(Entity.CompoundIndex annotation) {
    StringBuilder stmt = new StringBuilder("@Table.CompoundIndex(indexes = {");
    for (int i = 0; i < annotation.columns().length; i++) {
      String index = annotation.columns()[i];
      stmt.append("@Table.Index(columnName = \"").append(index).append("\")");
      if (i != annotation.columns().length - 1) {
        stmt.append(", \n");
      }
    }
    if (annotation.unique()) {
      stmt.append("},\n" +
          "      unique = true\n" +
          "      )");
    } else stmt.append("})");

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
    StringBuilder stmt = new StringBuilder(" {\n");
    if (compoundIndices.length > 0) {
      //fileBuilder.addImport("promise.db", "Table")
      try {
        for (int index = 0; index < compoundIndices.length; index++) {
          Entity.CompoundIndex compoundIndex = compoundIndices[index];
          stmt.append(getCompoundIndexAnnotation(compoundIndex));
          if (index != compoundIndices.length - 1) {
            stmt.append(", \n");
          }
        }
        stmt.append(" }\n" +
            "          ");

        annotationSpec.addMember("compoundIndexes", CodeBlock.of(stmt.toString()));
      } catch (Throwable e) {
        this.processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, "CompoundIndex gen error " + Arrays.toString(e.getStackTrace()));
      }
    }
    annotationSpec.addMember("tableName", "$S", PersistableEntityUtilsKt.getTableName(element));
    return annotationSpec.build();
  }
}
