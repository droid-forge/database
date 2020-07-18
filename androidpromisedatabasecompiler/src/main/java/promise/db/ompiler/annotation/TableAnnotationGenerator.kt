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

package promise.db.ompiler.annotation

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import promise.db.Entity
import promise.db.Table
import promise.db.ompiler.CodeBlockGenerator
import promise.db.ompiler.getTableCompoundIndices
import promise.db.ompiler.getTableName
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

class TableAnnotationGenerator(
    private val fileBuilder: FileSpec.Builder,
    private val element: Element) : CodeBlockGenerator<AnnotationSpec> {

  private fun getCompoundIndexAnnotation(annotation: Entity.CompoundIndex): String {
    var stmt = "Table.CompoundIndex(indexes = ["
    annotation.columns.forEachIndexed { i, index ->
      stmt += "Table.Index(columnName = \"${index}\")"
      if (i != annotation.columns.size - 1) {
        stmt += ", \n"
      }
    }
    stmt += if (annotation.unique) {
      """
      ],
      unique = true
      )
    """.trimIndent()
    } else {
      """
      ])
    """.trimIndent()
    }
    return stmt
  }

  override fun generate(): AnnotationSpec {
    val string = """
     @Table(
    tableName = "p",
    compoundIndexes = [
      Table.CompoundIndex(indexes = [
        Table.Index(columnName = "n"),
        Table.Index(columnName = "a")
      ],
      unique = true)
    ]
)
    """.trimIndent()
    val annotationSpec = AnnotationSpec.builder(Table::class.java)
    val compoundIndices = (element as TypeElement).getTableCompoundIndices()
    var stmt = "compoundIndexes = [\n"
    if (compoundIndices.isNotEmpty()) {
      fileBuilder.addImport("promise.db", "Table")
      try {
        compoundIndices.forEachIndexed { index, compoundIndex ->
          stmt += getCompoundIndexAnnotation(compoundIndex)
          if (index != compoundIndices.size - 1) {
            stmt += ", \n"
          }
        }
        stmt += """
          ]
          
        """.trimIndent()
        annotationSpec.addMember(stmt)
      } catch (e: Throwable) {
      }
    }
    annotationSpec.addMember(CodeBlock.builder()
        .addStatement("tableName = %S", element.getTableName())
        .build())
    return annotationSpec.build()
  }
}