package online.avogadro.opencv4tasker.tasker

import android.content.Context
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.condition.TaskerPluginRunnerCondition
import com.joaomgcd.taskerpluginlibrary.condition.TaskerPluginRunnerConditionNoInput
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.extensions.requestQuery
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputVariable
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultCondition
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionSatisfied
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess

/**
 * Input configuration for the Notification Intercepted event
 */
@TaskerInputRoot
class NotificationInterceptedEventInput @JvmOverloads constructor(
    @field:TaskerInputField("enabled") var enabled: Boolean = true,
    @field:TaskerInputField("appNameFilter") var appNameFilter: String = ""
)

/**
 * Output data for the Notification Intercepted event
 */
@TaskerInputRoot
@TaskerOutputObject
class NotificationInterceptedEvent(
    @get:TaskerOutputVariable("notification_title")
    @TaskerInputField(key="notification_title")
    var notificationTitle: String = "",
    
    @get:TaskerOutputVariable("notification_text")
    @TaskerInputField(key="notification_text")
    var notificationText: String = "",
    
    @get:TaskerOutputVariable("image_path")
    @TaskerInputField(key="image_path")
    var imagePath: String = "",
    
    @get:TaskerOutputVariable("app_package")
    @TaskerInputField(key="app_package")
    var appPackage: String = "",
    
    @get:TaskerOutputVariable("app_name")
    @TaskerInputField(key="app_name")
    var appName: String = ""
)

/**
 * Tasker Plugin Event Helper for Notification Intercepted
 */
class NotificationInterceptedEventHelper(config: TaskerPluginConfig<NotificationInterceptedEventInput>) : 
    TaskerPluginConfigHelper<NotificationInterceptedEventInput, NotificationInterceptedEvent, NotificationInterceptedRunnerConditionEvent>(config) {
    
    override val runnerClass: Class<NotificationInterceptedRunnerConditionEvent>
        get() = NotificationInterceptedRunnerConditionEvent::class.java
    
    override val inputClass = NotificationInterceptedEventInput::class.java
    
    override val outputClass = NotificationInterceptedEvent::class.java
    
    override fun addToStringBlurb(input: TaskerInput<NotificationInterceptedEventInput>, blurbBuilder: StringBuilder) {
        if (input.regular.enabled) {
            if (input.regular.appNameFilter.isNotEmpty()) {
                blurbBuilder.append(" monitoring notifications from apps containing '${input.regular.appNameFilter}'")
            } else {
                blurbBuilder.append(" monitoring notifications with images from all apps")
            }
        } else {
            blurbBuilder.append(" notification monitoring disabled")
        }
    }
}

///**
// * Event Runner - dummy implementation since events are triggered externally
// */
//class NotificationInterceptedEventRunner : TaskerPluginRunnerAction<NotificationInterceptedEventInput, NotificationInterceptedEvent>() {
//
//    override fun run(context: Context, input: TaskerInput<NotificationInterceptedEventInput>): TaskerPluginResult<NotificationInterceptedEvent> {
//        // This is just a placeholder - the actual event triggering is handled by the NotificationInterceptorService
//        // Return success to indicate the event configuration is valid
//        return TaskerPluginResultSucess()
//    }
//}

class NotificationInterceptedRunnerConditionEvent() : TaskerPluginRunnerCondition<NotificationInterceptedEventInput, NotificationInterceptedEvent, NotificationInterceptedEvent>() {
    override val isEvent: Boolean get() = true

    override fun getSatisfiedCondition(context: Context, input: TaskerInput<NotificationInterceptedEventInput>, update: NotificationInterceptedEvent?): TaskerPluginResultCondition<NotificationInterceptedEvent> {
        return TaskerPluginResultConditionSatisfied(context, update)
    }
}

fun Context.triggerTaskerEventNotificationIntercepted(bundle: Any?) = ActivityConfigNotificationInterceptedEvent::class.java.requestQuery(this, bundle)