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
//
//import android.database.Cursor;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import promise.base.comment.PostComment;
//import promise.base.comment.PostCommentDao;
//import promise.base.comment.PostCommentToReplyRelation;
//import promise.base.comment.PostCommentsTable;
//import promise.db.PromiseDatabase;
//import promise.db.criteria.Criteria;
//import promise.model.IdentifiableList;

public class PostCommentsDaoImpl
    //extends PostCommentDao
{
//
//  private PromiseDatabase fastDatabase;
//  private PostCommentsTable postCommentsTable;
//
//  public PostCommentsDaoImpl(PromiseDatabase fastDatabase) {
//    this.fastDatabase = fastDatabase;
//    this.postCommentsTable = (PostCommentsTable) fastDatabase.tableOf(PostComment.class);
//  }
//
//  @Override
//  public List<PostCommentToReplyRelation> getPostComments(Criteria andCriteria) {
//    Cursor cursor = fastDatabase.getDatabaseInstance().query(
//        postCommentsTable.queryBuilder()
//            .whereAnd(andCriteria));
//    return getPostCommentRepliesImpl(cursor);
//  }
//
//  public List<PostComment> getPostCommentRepliesImpl(PostComment postComment) {
//    return new ArrayList<>(postCommentsTable.findAll(PostCommentsTable.idColumn.with(postComment.getId())));
//  }
//
//  private List<PostCommentToReplyRelation> getPostCommentRepliesImpl(Cursor cursor) {
//    IdentifiableList<? extends PostComment> postComments = postCommentsTable.collection(cursor);
//    return postComments.map(postComment -> new PostCommentToReplyRelation() {{
//      setPostComment(postComment);
//      setPostCommentReplies(getPostCommentRepliesImpl(postComment));
//    }});
//  }
//
//  private PostCommentToReplyRelation getPostCommentReplyImpl(Cursor cursor) {
//    PostComment postComment = postCommentsTable.single(cursor);
//    return new PostCommentToReplyRelation() {{
//      setPostComment(postComment);
//      setPostCommentReplies(getPostCommentRepliesImpl(postComment));
//    }};
//  }
}
