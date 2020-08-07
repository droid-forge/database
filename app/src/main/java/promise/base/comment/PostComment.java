package promise.base.comment;

import android.annotation.SuppressLint;

import org.jetbrains.annotations.NotNull;

import promise.base.ID;
import promise.base.post.Post;
import promise.db.ActiveRecord;
import promise.db.Entity;
import promise.db.HasOne;

@SuppressLint("ParcelCreator")
@Entity
public class PostComment extends ActiveRecord<PostComment> {
	private String name;
	private ID uId;
	private String body;
	private String email;

	@HasOne
	private Post post;

	public void setName(String name){
		this.name = name;
	}

	public String getName(){
		return name;
	}

	public void setBody(String body){
		this.body = body;
	}

	public String getBody(){
		return body;
	}

	public void setEmail(String email){
		this.email = email;
	}

	public String getEmail(){
		return email;
	}

	public Post getPost() {
		return post;
	}

	public void setPost(Post post) {
		this.post = post;
	}

	@Override
 	public String toString(){
		return 
			"Comment{" + 
			"name = '" + name + '\'' +
			",id = '" + uId + '\'' +
			",body = '" + body + '\'' + 
			",email = '" + email + '\'' + 
			"}";
		}

	@NotNull
	@Override
	public PostComment getEntity() {
		return this;
	}

	public ID getUId() {
		return uId;
	}

	public void setUId(ID uId) {
		this.uId = uId;
	}
}
