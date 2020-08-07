package promise.base.post;

import android.annotation.SuppressLint;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import promise.base.ID;
import promise.base.comment.PostComment;
import promise.base.photo.Photo;
import promise.db.ActiveRecord;
import promise.db.Entity;
import promise.db.HasMany;

@SuppressLint("ParcelCreator")
@Entity
public class Post extends ActiveRecord<Post> {
	private ID uId;
	private String title;
	private String body;
	private int userId;

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

	@HasMany
	private List<PostComment> comments;

	@HasMany
	private List<Photo> photos;


	public String getTitle(){
		return title;
	}

	public String getBody(){
		return body;
	}

	public int getUserId(){
		return userId;
	}

	public ID getUId() {
		return uId;
	}

	public void setUId(ID uId) {
		this.uId = uId;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public void setUserId(int userId) {
		this.userId = userId;
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

	@NotNull
	@Override
	public Post getEntity() {
		return this;
	}
}
