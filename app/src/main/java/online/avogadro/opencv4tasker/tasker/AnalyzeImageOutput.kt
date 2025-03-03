package online.avogadro.opencv4tasker.tasker

import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputVariable

@TaskerOutputObject
class AnalyzeImageOutput(
    @get:TaskerOutputVariable("response") var response: String? = ""
)
