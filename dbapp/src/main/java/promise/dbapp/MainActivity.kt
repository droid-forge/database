package promise.dbapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import promise.commons.model.Result
import promise.dbapp.model.ComplexModel
import promise.dbapp.model.Database
import promise.model.SList

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setSupportActionBar(toolbar)

    fab.setOnClickListener { view ->
      Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
          .setAction("Action", null).show()
    }
  }

  override fun onPostCreate(savedInstanceState: Bundle?) {
    super.onPostCreate(savedInstanceState)

    val database = Database()
    database.allComplexModels(Result<SList<out ComplexModel>, Throwable>()
        .withCallBack {
          if (it.isNotEmpty()) {
            complex_values_textview.text = it.toString()
          } else complex_values_textview.text = "empty list"
        }
        .withErrorCallBack { complex_values_textview.text = it.message })

    clear_button.setOnClickListener {
      database.deleteAllAsync()
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe {
            complex_values_textview.text = ""
          }
    }
  }
}
