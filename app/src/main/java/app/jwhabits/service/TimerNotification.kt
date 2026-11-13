package app.jwhabits.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.jwhabits.JwHabitsApp
import app.jwhabits.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 奉仕中の通知（経過時間つき、「終了」ボタンつき）。 */
object TimerNotification {
    private const val CHANNEL_ID = "service_timer"
    private const val NOTIFICATION_ID = 1001
    const val ACTION_STOP = "app.jwhabits.action.STOP_TIMER"

    private fun manager(context: Context) = context.getSystemService(NotificationManager::class.java)

    fun show(context: Context, startMs: Long) {
        val nm = manager(context)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "奉仕タイマー", NotificationManager.IMPORTANCE_DEFAULT).apply {
                // 「サイレント」の欄に畳まれず「終了」がすぐ押せるよう DEFAULT にし、音と振動は切る
                setSound(null, null)
                enableVibration(false)
            }
        )
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), flags)
        val stop = PendingIntent.getBroadcast(
            context, 0, Intent(context, TimerStopReceiver::class.java).setAction(ACTION_STOP), flags,
        )
        val n = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("奉仕中")
            .setUsesChronometer(true)
            .setWhen(startMs)
            .setShowWhen(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "終了", stop).build())
            .build()
        // 通知の許可がなくても、タイマー自体は動く
        runCatching { nm.notify(NOTIFICATION_ID, n) }
    }

    fun cancel(context: Context) = manager(context).cancel(NOTIFICATION_ID)
}

/** 通知の「終了」ボタン。タイマーを止めて通知を消す。 */
class TimerStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TimerNotification.ACTION_STOP) return
        val app = context.applicationContext as JwHabitsApp
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.service.stop()
            } finally {
                TimerNotification.cancel(app)
                pending.finish()
            }
        }
    }
}
