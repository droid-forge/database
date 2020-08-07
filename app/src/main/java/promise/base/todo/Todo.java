package promise.base.todo;

import android.annotation.SuppressLint;

import org.jetbrains.annotations.NotNull;

import promise.base.photo.Photo;
import promise.db.ActiveRecord;
import promise.database.Entity;
import promise.database.HasOne;

@SuppressLint("ParcelCreator")
@Entity
public class Todo extends ActiveRecord<Todo> {
	private int uid;
	private boolean completed;
	private String title;
	private int userId;

	@HasOne
	private Photo photo;

	public int getUid() {
		return uid;
	}

	public void setUid(int uid) {
		this.uid = uid;
	}

	public Photo getPhoto() {
		return photo;
	}

	public void setPhoto(Photo photo) {
		this.photo = photo;
	}

	public void setCompleted(boolean completed){
		this.completed = completed;
	}

	public boolean isCompleted(){
		return completed;
	}

	public void setTitle(String title){
		this.title = title;
	}

	public String getTitle(){
		return title;
	}

	public void setUserId(int userId){
		this.userId = userId;
	}

	public int getUserId(){
		return userId;
	}

	@Override
 	public String toString(){
		return 
			"Todo{" + 
			"id = '" + uid + '\'' +
			",completed = '" + completed + '\'' + 
			",title = '" + title + '\'' + 
			",userId = '" + userId + '\'' + 
			"}";
		}

	@NotNull
	@Override
	public Todo getEntity() {
		return this;
	}
}
