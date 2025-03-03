package online.avogadro.opencv4tasker.tasker

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot

@TaskerInputRoot
class AnalyzeImageInput @JvmOverloads constructor(
    @field:TaskerInputField("imagePath") var imagePath: String? = null,
    @field:TaskerInputField("engine") var engine: String? = null,
    @field:TaskerInputField("systemPrompt") var systemPrompt: String? = null,
    @field:TaskerInputField("userPrompt") var userPrompt: String? = null
)
