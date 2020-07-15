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
annotation class VarChar(val name: String = "",
                         val length: Int,
                         val nullable: Boolean = true,
                         val unique: Boolean = false,
                         val index: Boolean = false)

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class Text(val name: String = "",
                      val nullable: Boolean = true,
                      val unique: Boolean = false,
                      val index: Boolean = false)

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class Number(val name: String = "",
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
annotation class Blob(val name: String = "",
                      val nullable: Boolean = true,
                      val unique: Boolean = false,
                      val index: Boolean = false)

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class Real(val name: String = "",
                      val nullable: Boolean = true,
                      val unique: Boolean = false,
                      val index: Boolean = false)

enum class Migrations {
  CREATE, DROP
}

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class Migrate(val from: Int,
                         val to: Int,
                         val action: Migrations)