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
package promise.db


@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class VarChar(val columnName: String = "",
                         val length: Int,
                         val nullable: Boolean = true,
                         val unique: Boolean = false,
                         val index: Boolean = false)

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class Text(val columnName: String = "",
                      val nullable: Boolean = true,
                      val unique: Boolean = false,
                      val index: Boolean = false)

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class Number(val columnName: String = "",
                        val default: Int = 0,
                        val nullable: Boolean = true,
                        val unique: Boolean = false,
                        val index: Boolean = false)

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class PrimaryKey(val name: String = "")

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class Embedded(val prefix: String = "")

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class Ignore

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class PrimaryKeyAutoIncrement

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class Blob(val columnName: String = "",
                      val nullable: Boolean = true,
                      val unique: Boolean = false,
                      val index: Boolean = false)

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class Real(val columnName: String = "",
                      val nullable: Boolean = true,
                      val unique: Boolean = false,
                      val index: Boolean = false)

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class OneToOne(val name: String = "")

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class ManyToOne(val name: String = "")

enum class MigrationOptions {
  CREATE, DROP, CREATEINDEX
}

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class Migrate(val fromVersion: Int,
                         val toVersion: Int,
                         val action: MigrationOptions)



@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class Migrations(val values: Array<Migrate>)

