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

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import promise.base.ID
import promise.base.comment.PostComment
import promise.base.post.Post
import promise.base.post.PostRelationsDao
import promise.model.IdentifiableList
import javax.inject.Inject
import promise.commons.model.List

class MainActivity : DaggerAppCompatActivity() {

 @Inject
 lateinit var postRelationsDao: PostRelationsDao

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setSupportActionBar(toolbar)

    fab.setOnClickListener { view ->
      Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
          .setAction("Action", null).show()
    }
  }

  override fun onPostCreate(savedInstanceState: Bundle?) {
    super.onPostCreate(savedInstanceState)
  
    val posts = IdentifiableList(List.generate(5) {
      Post().apply {
        uId = ID(it.toString())
        title = "post".plus(it)
        body = "body".plus(it)
        userId = it
        comments = List.generate(4) {
          PostComment().apply {
            name = "nm".repeat(it)
            uId = ID((it + 1).toString())
            body = "hbytcvbcrxgfvbtrxt"
            email = "ejmail;jgfccghcfcvhbhcgvb"
          }
        }
      }
    })

   postRelationsDao.saveWithComments(posts)

   val persons = postRelationsDao.listWithComments()

   complex_values_textview.text = persons.toString()

   persons.forEach {
     postRelationsDao.deleteComments(it)
     it.delete()
   }
  }

}
