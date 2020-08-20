package promise.base.post;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

import promise.base.ID;
import promise.base.comment.Like;
import promise.base.comment.PostComment;
import promise.base.photo.Photo;
import promise.database.Entity;
import promise.database.HasMany;
import promise.db.ActiveRecord;

@SuppressLint("ParcelCreator")
@Entity
public class Post extends ActiveRecord<Post> {
  private ID uId;
  private String title;
  private String body;
  private int userId;

  private int numberOfViews;
  private Date publishedDate;
  @HasMany
  private List<PostComment> comments;
  @HasMany
  private List<Photo> photos;
  @HasMany
  private List<Like> likes;

  public int getNumberOfViews() {
    return numberOfViews;
  }

  public void setNumberOfViews(int numberOfViews) {
    this.numberOfViews = numberOfViews;
  }

  @NonNull
  @Override
  public String toString() {
    return "Post{" +
        "uId=" + uId +
        ", title='" + title + '\'' +
        ", body='" + body + '\'' +
        ", userId=" + userId +
        ", comments=" + comments +
        ", photos=" + photos +
        '}';
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public int getUserId() {
    return userId;
  }

  public void setUserId(int userId) {
    this.userId = userId;
  }

  public ID getUId() {
    return uId;
  }

  public void setUId(ID uId) {
    this.uId = uId;
  }

  public ID getuId() {
    return uId;
  }

  public void setuId(ID uId) {
    this.uId = uId;
  }

  public List<PostComment> getComments() {
    return comments;
  }

  public void setComments(List<PostComment> comments) {
    this.comments = comments;
  }

  public List<Photo> getPhotos() {
    return photos;
  }

  public void setPhotos(List<Photo> photos) {
    this.photos = photos;
  }

  public List<Like> getLikes() {
    return likes;
  }

  public void setLikes(List<Like> likes) {
    this.likes = likes;
  }

  @NotNull
  @Override
  public Post getEntity() {
    return this;
  }

  public Date getPublishedDate() {
    return publishedDate;
  }

  public void setPublishedDate(Date publishedDate) {
    this.publishedDate = publishedDate;
  }
}
