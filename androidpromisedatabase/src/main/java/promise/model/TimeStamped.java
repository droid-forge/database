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

package promise.model;

import android.os.Parcel;
import android.os.Parcelable;

import promise.commons.model.Identifiable;
import promise.db.model.ITimeStamped;

public abstract class TimeStamped implements Identifiable<Integer>, ITimeStamped, Parcelable {
  private int id = 0;
  private long createdAt, updatedAt;

  public TimeStamped() {
    setCreatedAt(System.currentTimeMillis());
    setUpdatedAt(System.currentTimeMillis());
  }

  public void readFromParcel(Parcel in) {
    setId(in.readInt());
    setCreatedAt(in.readLong());
    setUpdatedAt(in.readLong());
  }

  public long getCreatedAt() {
    return createdAt;
  }

  @Override
  public void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  @Override
  public void setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public Integer getId() {
    return id;
  }

  @Override
  public void setId(Integer id) {
    this.id = id;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(getId());
    dest.writeLong(getCreatedAt());
    dest.writeLong(getUpdatedAt());
  }
}
