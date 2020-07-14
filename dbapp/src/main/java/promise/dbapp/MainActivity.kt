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

package promise.dbapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import promise.commons.tx.PromiseResult
import promise.dbapp.model.ComplexRecord
import promise.dbapp.model.AppDatabase
import promise.dbapp.model.NewRecord
import promise.model.IdentifiableList

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

    AppDatabase.allComplexModels(PromiseResult<IdentifiableList<out ComplexRecord>, Throwable>()
        .withCallback {
          if (it.isNotEmpty()) {
            complex_values_textview.text = it.toString()
          } else complex_values_textview.text = "empty list"
        }
        .withErrorCallback { complex_values_textview.text = it.message })

    clear_button.setOnClickListener {
      AppDatabase.instance.deleteAll()
      complex_values_textview.text = ""
    }

    val newRecordTable = AppDatabase.newRecordTable
    var items = newRecordTable.findAll()
    if (items.isEmpty()) {
      newRecordTable.save(IdentifiableList(NewRecord.someModels()))
      items = newRecordTable.findAll()
    }
    complex_values_textview.text = items.toString()


  }
}
