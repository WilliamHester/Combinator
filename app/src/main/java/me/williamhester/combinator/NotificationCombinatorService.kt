package me.williamhester.combinator

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/** The service responsible for listening to and re-issuing notifications.  */
class NotificationCombinatorService : NotificationListenerService() {
  private val channelId = "combinator"
  private val textMessageApps = setOf(
      "com.google.android.talk",
      "com.google.android.apps.messaging",
      "com.facebook.orca",
      "com.Slack",
      "com.google.android.apps.dynamite")
  private val executorService = Executors.newScheduledThreadPool(1)
  private var notificationManager: NotificationManager? = null
  private var cancelNotificationFuture: ScheduledFuture<*>? = null

  override fun onCreate() {
    super.onCreate()
    notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val infoChannel =
        NotificationChannel(channelId, "Notifications", NotificationManager.IMPORTANCE_LOW)
    infoChannel.description = "Notifications recreated to be displayed on a Fitbit"
    infoChannel.enableLights(false)
    infoChannel.enableVibration(false)
    notificationManager!!.createNotificationChannel(infoChannel)
  }

  override fun onNotificationPosted(notification: StatusBarNotification) {
    super.onNotificationPosted(notification)
    if (!textMessageApps.contains(notification.packageName)) {
      return
    }
    if (cancelNotificationFuture != null) {
      cancelNotificationFuture!!.cancel(false)
      cancelNotificationFuture = null
    }
    val extras = notification.notification.extras
    val name = extras.getString(Notification.EXTRA_TITLE)
    val text = extras.get(Notification.EXTRA_TEXT).toString()

    val myNotification = Notification.Builder(this, channelId)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(name)
        .setContentText(text)
        .build()

    notificationManager!!.notify(1, myNotification)
    cancelNotificationFuture = executorService.schedule(
        {
          notificationManager!!.cancel(1)
          cancelNotificationFuture = null
        },
        10,
        TimeUnit.SECONDS)
  }
}
