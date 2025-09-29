package online.avogadro.opencv4tasker.tasker

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputVariable

@TaskerInputRoot
@TaskerOutputObject
class CancelNotificationOutput @JvmOverloads constructor(
    @get:TaskerOutputVariable("success")
    @TaskerInputField(key="success")
    var success: Boolean = false,

    @get:TaskerOutputVariable("message")
    @TaskerInputField(key="message")
    var message: String = ""
)