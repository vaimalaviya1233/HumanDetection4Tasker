package online.avogadro.opencv4tasker.tasker

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot

@TaskerInputRoot
class CancelNotificationInput @JvmOverloads constructor(
    @field:TaskerInputField("notificationKey") var notificationKey: String? = null
)