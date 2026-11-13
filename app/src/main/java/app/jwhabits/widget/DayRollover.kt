package app.jwhabits.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.jwhabits.JwHabitsApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZoneId

/** 日付が切り替わる時刻に、ウィジェットを描き直す（正確さは求めない）。 */
object DayRollover {
    fun schedule(app: JwHabitsApp) {
        val next = app.clock.nextCutoff().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pi = PendingIntent.getBroadcast(
            app, 0, Intent(app, DayRolloverReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        // 少し遅れてよい（正確なアラームの許可は要らない）
        app.getSystemService(AlarmManager::class.java).set(AlarmManager.RTC, next + 1_000, pi)
    }
}

class DayRolloverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as JwHabitsApp
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                WidgetUpdater.updateAll(app)
                DayRollover.schedule(app)
            } finally {
                pending.finish()
            }
        }
    }
}
