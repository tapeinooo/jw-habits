package app.jwhabits

import android.app.Application
import app.jwhabits.data.AppDatabase
import app.jwhabits.domain.DayClock

class JwHabitsApp : Application() {
    val db: AppDatabase by lazy { AppDatabase.build(this) }
    val clock: DayClock by lazy { DayClock(this) }
}
