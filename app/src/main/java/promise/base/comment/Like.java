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

package promise.base.comment;

import android.annotation.SuppressLint;

import org.jetbrains.annotations.NotNull;

import promise.base.ID;
import promise.base.post.Post;
import promise.database.Entity;
import promise.database.HasOne;
import promise.db.ActiveRecord;

@SuppressLint("ParcelCreator")
@Entity
//@AddedEntity(fromVersion = 1, toVersion = 2)
public class Like extends ActiveRecord<Like> {
  private ID uId;

  @HasOne
  private Post post;

  @Override
  public String toString() {
    return
        "Comment{" +
            ",id = '" + uId + '\'' +

            "}";
  }

  @NotNull
  @Override
  public Like getEntity() {
    return this;
  }

  public ID getUId() {
    return uId;
  }

  public void setUId(ID uId) {
    this.uId = uId;
  }

  public Post getPost() {
    return post;
  }

  public void setPost(Post post) {
    this.post = post;
  }
}
