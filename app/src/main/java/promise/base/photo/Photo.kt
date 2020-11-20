package promise.base.photo

import android.os.Parcel
import android.os.Parcelable
import promise.base.post.Post
import promise.database.Entity
import promise.database.HasOne
import promise.db.ActiveRecord

@Entity
class Photo() : ActiveRecord<Photo>() {
  var albumId: Int? = null
  var title: String? = null
  var url: String? = null
  var thumbnailUrl: String? = null

  @HasOne
  var photoPost: Post? = null

  constructor(parcel: Parcel) : this() {
    albumId = parcel.readValue(Int::class.java.classLoader) as? Int
    title = parcel.readString()
    url = parcel.readString()
    thumbnailUrl = parcel.readString()
    photoPost = parcel.readParcelable(Post::class.java.classLoader)
  }

  override fun getEntity(): Photo = this

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    super.writeToParcel(parcel, flags)
    parcel.writeValue(albumId)
    parcel.writeString(title)
    parcel.writeString(url)
    parcel.writeString(thumbnailUrl)
    parcel.writeParcelable(photoPost, flags)
  }

  override fun describeContents(): Int = 0

  companion object CREATOR : Parcelable.Creator<Photo> {
    override fun createFromParcel(parcel: Parcel): Photo = Photo(parcel)
    override fun newArray(size: Int): Array<Photo?> = arrayOfNulls(size)
  }

}

