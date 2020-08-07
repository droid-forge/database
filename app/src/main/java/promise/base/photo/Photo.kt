package promise.base.photo

import android.annotation.SuppressLint
import promise.base.post.Post
import promise.db.ActiveRecord
import promise.database.Entity
import promise.database.HasOne

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

