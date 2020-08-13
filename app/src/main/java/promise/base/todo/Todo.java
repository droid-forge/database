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

	private String description;

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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
 	public String toString(){
		return 
			"Todo{" + 
			"id = '" + uid + '\'' +
			",completed = '" + completed + '\'' + 
			",title = '" + title + '\'' +
			"}";
		}

	@NotNull
	@Override
	public Todo getEntity() {
		return this;
	}
}
