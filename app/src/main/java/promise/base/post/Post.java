package promise.base.post;

import android.annotation.SuppressLint;
import android.os.Parcel;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import promise.base.ID;
import promise.base.comment.Like;
import promise.base.comment.PostComment;
import promise.base.photo.Photo;
import promise.database.ColumnInfo;
import promise.database.Entity;
import promise.database.HasMany;
import promise.db.ActiveRecord;

@Entity
public class Post extends ActiveRecord<Post> {

  @ColumnInfo(unique = true)
  private ID uId;

  @ColumnInfo(columnName = "ttl", length = 40, unique = true)
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

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeParcelable(this.uId, flags);
    dest.writeString(this.title);
    dest.writeString(this.body);
    dest.writeInt(this.userId);
    dest.writeInt(this.numberOfViews);
    dest.writeLong(this.publishedDate != null ? this.publishedDate.getTime() : -1);
    dest.writeTypedList(this.comments);
    dest.writeTypedList(this.photos);
    dest.writeTypedList(this.likes);
  }

  public Post() {
  }

  protected Post(Parcel in) {
    super(in);
    this.uId = in.readParcelable(ID.class.getClassLoader());
    this.title = in.readString();
    this.body = in.readString();
    this.userId = in.readInt();
    this.numberOfViews = in.readInt();
    long tmpPublishedDate = in.readLong();
    this.publishedDate = tmpPublishedDate == -1 ? null : new Date(tmpPublishedDate);
    this.comments = in.createTypedArrayList(PostComment.CREATOR);
    this.photos = in.createTypedArrayList(Photo.CREATOR);
    this.likes = in.createTypedArrayList(Like.CREATOR);
  }

  public static final Creator<Post> CREATOR = new Creator<Post>() {
    @Override
    public Post createFromParcel(Parcel source) {
      return new Post(source);
    }

    @Override
    public Post[] newArray(int size) {
      return new Post[size];
    }
  };
}
