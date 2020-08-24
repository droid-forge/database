package promise.base.comment;

import android.annotation.SuppressLint;
import android.os.Parcel;

import org.jetbrains.annotations.NotNull;

import promise.base.ID;
import promise.base.post.Post;
import promise.database.Entity;
import promise.database.HasOne;
import promise.db.ActiveRecord;

@Entity
public class PostComment extends ActiveRecord<PostComment> {
  private String name;
  private ID uId;
  private String body;
  private String email;

  private String postCommentId = "";

  @HasOne
  private Post post;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Post getPost() {
    return post;
  }

  public void setPost(Post post) {
    this.post = post;
  }

  public String getPostCommentId() {
    return postCommentId;
  }

  public void setPostCommentId(String postCommentId) {
    this.postCommentId = postCommentId;
  }

  @Override
  public String toString() {
    return
        "Comment{" +
            "name = '" + name + '\'' +
            ",id = '" + uId + '\'' +
            ",body = '" + body + '\'' +
            ",email = '" + email + '\'' +
            "}";
  }

  @NotNull
  @Override
  public PostComment getEntity() {
    return this;
  }

  public ID getUId() {
    return uId;
  }

  public void setUId(ID uId) {
    this.uId = uId;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeString(this.name);
    dest.writeParcelable(this.uId, flags);
    dest.writeString(this.body);
    dest.writeString(this.email);
    dest.writeString(this.postCommentId);
    dest.writeParcelable(this.post, flags);
  }

  public PostComment() {
  }

  protected PostComment(Parcel in) {
    super(in);
    this.name = in.readString();
    this.uId = in.readParcelable(ID.class.getClassLoader());
    this.body = in.readString();
    this.email = in.readString();
    this.postCommentId = in.readString();
    this.post = in.readParcelable(Post.class.getClassLoader());
  }

  public static final Creator<PostComment> CREATOR = new Creator<PostComment>() {
    @Override
    public PostComment createFromParcel(Parcel source) {
      return new PostComment(source);
    }

    @Override
    public PostComment[] newArray(int size) {
      return new PostComment[size];
    }
  };
}
