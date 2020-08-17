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

package promise.dbapp.test;

import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

import promise.base.comment.PostComment;
import promise.base.comment.PostCommentDao;
import promise.base.comment.PostCommentToReplyRelation;
import promise.base.comment.PostCommentsTable;
import promise.db.PromiseDatabase;
import promise.db.criteria.Criteria;
import promise.model.IdentifiableList;

public class PostCommentsDaoImpl
    implements PostCommentDao
{

  private PromiseDatabase promiseDatabase;

  public PostCommentsDaoImpl(PromiseDatabase fastDatabase) {
    this.promiseDatabase = fastDatabase;
  }

  private PostCommentToReplyRelation getPostCommentToReplyRelation(PostComment postComment) {
    return new PostCommentToReplyRelation() {{
      setPostComment(postComment);
      setPostCommentReplies(new ArrayList<>(
          promiseDatabase.tableOf(PostComment.class).findAll(
              PostCommentsTable.postCommentIdColumn.with(
                  postComment.getId().toString()))));
    }};
  }

  @Override
  public List<PostCommentToReplyRelation> getPostComments(Criteria andCriteria) {
    Cursor cursor = promiseDatabase.getDatabaseInstance().query(
        promiseDatabase.tableOf(PostComment.class).queryBuilder()
            .whereAnd(andCriteria));
    return getPostCommentToReplyRelationCollection(cursor);
  }

  private List<PostCommentToReplyRelation> getPostCommentToReplyRelationCollection(Cursor cursor) {
    IdentifiableList<? extends PostComment> postComments = promiseDatabase.tableOf(PostComment.class).collection(cursor);
    return postComments.map(this::getPostCommentToReplyRelation);
  }
}
