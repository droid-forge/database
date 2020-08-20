package promise.base.todo;

import android.annotation.SuppressLint;

import org.jetbrains.annotations.NotNull;

import promise.base.photo.Photo;
import promise.database.Entity;
import promise.database.HasOne;
import promise.db.ActiveRecord;

@SuppressLint("ParcelCreator")
@Entity
public class Todo extends ActiveRecord<Todo> {
  private int uid;
  private boolean completed;

  private String title;

  private long timeStarted;
  private long timeFinished;
  private String description;
  @HasOne
  private Photo photo;

  public long getTimeFinished() {
    return timeFinished;
  }

  public void setTimeFinished(long timeFinished) {
    this.timeFinished = timeFinished;
  }

  public long getTimeStarted() {
    return timeStarted;
  }

  public void setTimeStarted(long timeStarted) {
    this.timeStarted = timeStarted;
  }

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

  public boolean isCompleted() {
    return completed;
  }

  public void setCompleted(boolean completed) {
    this.completed = completed;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String toString() {
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
