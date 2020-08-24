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

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import promise.base.ID;
import promise.base.post.Post;
import promise.commons.model.Identifiable;
import promise.database.Entity;
import promise.database.HasOne;
import promise.database.PrimaryKeyAutoIncrement;

@Entity

public class Like implements Identifiable<Integer>, Parcelable {
  private ID uId;

  @HasOne
  private Post post;

  @PrimaryKeyAutoIncrement
  private int id;

  private String type;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @NonNull
  @Override
  public String toString() {
    return
        "Like{" +
            ",id = '" + uId + '\'' +

            "}";
  }

  public ID getUId() {
    return uId;
  }

  public void setUId(ID uId) {
    this.uId = uId;
  }

  public Post getPost() {
    return post;
  }

  public void setPost(Post post) {
    this.post = post;
  }

  @Override
  public Integer getId() {
    return id;
  }

  @Override
  public void setId(Integer integer) {
    this.id = integer;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(this.uId, flags);
    dest.writeParcelable(this.post, flags);
    dest.writeInt(this.id);
    dest.writeString(this.type);
  }

  public Like() {
  }

  protected Like(Parcel in) {
    this.uId = in.readParcelable(ID.class.getClassLoader());
    this.post = in.readParcelable(Post.class.getClassLoader());
    this.id = in.readInt();
    this.type = in.readString();
  }

  public static final Parcelable.Creator<Like> CREATOR = new Parcelable.Creator<Like>() {
    @Override
    public Like createFromParcel(Parcel source) {
      return new Like(source);
    }

    @Override
    public Like[] newArray(int size) {
      return new Like[size];
    }
  };
}
