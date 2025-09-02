package online.avogadro.opencv4tasker.tasker
import android.content.Context

object NotificationRaiser {
    public fun raiseAlarmEvent(c: Context?, b: Any) {
        c?.triggerTaskerEventNotificationIntercepted(b)
    }
}
