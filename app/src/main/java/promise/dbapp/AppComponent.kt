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

import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import promise.base.post.PostRelationsDao
import promise.base.post.PostsTable
import javax.inject.Singleton

@Component(
    modules = [
      AndroidSupportInjectionModule::class,
      ActivityBuildersModule::class,
      DatabaseDependencies::class
    ]
)
@Singleton
interface AppComponent : AndroidInjector<App> {

//  fun postsTable(): PostsTable
//  fun postRelationsDao(): PostRelationsDao
  @Component.Builder
  interface Builder {

    @BindsInstance
    fun app(app: App): Builder

    fun build(): AppComponent
  }
}