package promise.base.photo

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import promise.base.ID
import promise.base.post.Post
import promise.db.ActiveRecord
import promise.db.Entity
import promise.db.HasOne

@SuppressLint("ParcelCreator")
@Entity
class Photo: ActiveRecord<Photo>() {
	var albumId: Int? = null
	var title: String? = null
	var url: String? = null
	var thumbnailUrl: String? = null
	@HasOne var post: Post? = null

	override fun getEntity(): Photo {
		return this
	}

}

