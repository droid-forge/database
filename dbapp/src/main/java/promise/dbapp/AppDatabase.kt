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

package promise.dbapp

import promise.commons.tx.PromiseResult
import promise.db.DatabaseEntity
import promise.dbapp.model.*
import promise.model.IdentifiableList

@DatabaseEntity(
    persistableEntities = [
      Person::class,
      Cat::class,
      Dog::class,
      ComplexRecord::class,
      NewRecord::class
    ],
    version = 2
)
/**/
abstract class AppDatabase {

  abstract fun getPersonFastTable(): PersonFastTable

  abstract fun getCatFastTable(): CatFastTable

  abstract fun getDogFastTable(): DogFastTable

  abstract fun complexModelTable(): ComplexRecordFastTable

  abstract fun newRecordTable(): NewRecordFastTable

  fun getAllDogsWithCats(): List<Dog> {
    return getDogFastTable().findAll()
  }

  fun allComplexModels(result: PromiseResult<IdentifiableList<out ComplexRecord>, Throwable>) {
    val items = complexModelTable().findAll()
    if (items.isEmpty()) saveSomeComplexModels(PromiseResult<Boolean, Throwable>()
        .withCallback {
          allComplexModels(result)
        })
    else result.response(items)
  }

  private fun saveSomeComplexModels(result: PromiseResult<Boolean, Throwable>) {
    complexModelTable().save(IdentifiableList(ComplexRecord.someModels()))
    result.response(true)
  }
}