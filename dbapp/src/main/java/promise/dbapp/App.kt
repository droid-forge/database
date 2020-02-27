package promise.dbapp

import android.app.Application
import promise.commons.Promise

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    Promise.init(this)
  }
}