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

package promise.base.post

import promise.base.ID
import promise.base.comment.PostComment
import promise.model.IdentifiableList
import javax.inject.Inject
import javax.inject.Singleton

interface PostRepository {

  fun savePost(post: Post)

  fun getPosts(): List<Post>

  fun deletePosts()
}

@Singleton
class PostRepositoryImpl
@Inject
constructor(
    private var postRelationsDao: PostRelationsDao,
    private var postsTable: PostsTable) : PostRepository {
  init {
    val posts = IdentifiableList(promise.commons.model.List.generate(5) {
      Post().apply {
        uId = ID().apply {
          id = it.toString()
        }
        title = "post".plus(it)
        body = "body".plus(it)
        userId = it
        comments = promise.commons.model.List.generate(4) {
          PostComment().apply {
            name = "nm".repeat(it)
            uId = ID().apply {
              id = (it + 1).toString()
            }
            body = "hbytcvbcrxgfvbtrxt"
            email = "ejmail;jgfccghcfcvhbhcgvb"
          }
        }
      }
    })

    postRelationsDao.saveWithComments(posts)
  }

  override fun savePost(post: Post) {
    postsTable.save(post)
  }

  override fun getPosts(): List<Post> = postRelationsDao.listWithComments()

  override fun deletePosts() {
    val persons = getPosts()
    persons.forEach {
      postRelationsDao.deleteComments(it)
      it.delete()
    }
  }
}