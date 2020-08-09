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

import java.util.List;

import promise.database.Child;
import promise.database.Relation;
import promise.database.Parent;

@Relation
public class PostCommentToReplyRelation {

  @Parent
  private PostComment postComment;

  @Child(fieldRelatedToParent = "postCommentId")
  private List<PostComment> postCommentReplies;

  public PostComment getPostComment() {
    return postComment;
  }

  public void setPostComment(PostComment postComment) {
    this.postComment = postComment;
  }

  public List<PostComment> getPostCommentReplies() {
    return postCommentReplies;
  }

  public void setPostCommentReplies(List<PostComment> postCommentReplies) {
    this.postCommentReplies = postCommentReplies;
  }
}
