package online.avogadro.opencv4tasker.llamacpp

import android.content.Context
import android.util.Log
import online.avogadro.opencv4tasker.ai.AIImageAnalyzer
import online.avogadro.opencv4tasker.app.SharedPreferencesHelper
import online.avogadro.opencv4tasker.app.Util
import org.json.JSONException
import java.io.File
import java.io.IOException

class HumansDetectorLlamaCpp : AIImageAnalyzer {

    companion object {
        private const val TAG = "HumansDetectorLlamaCpp"

        private const val PROMPT_SYSTEM =
            "The user will be providing images taken from cheap security cameras, these images might be taken during the day or the night and the angle may vary. Images are usually taken top-down, during the night images may be blurry due to person's movements. Please reply him with a single keyword in the first line and a brief explanation of your choice in the second line, chosen among these:\n" +
            "* HUMAN: an human or a part of an human (usually on the border of the image) is visible in the frame. The human may be seen from above since the camera is usually mounted on an high position\n" +
            "* SPIDER: no humans are visible but a spider is near the camera\n" +
            "* CAT: if it's an animal or a cat, it may be a cat walking away from the camera or walking toward the camera\n" +
            "* NONE: neither an human nor a spider are in frame\n" +
            "* UNCERTAIN: you were unable to tell in which of the above categories the image might fit. Use this response if you are not totally sure that the answer is one of the above\n" +
            "Ignore any shadows"

        fun closeModel() {
            LlamaCppEngine.closeModel()
        }
    }

    private var modelPath: String = ""
    private var mmprojPath: String = ""
    private var lastResponse: String? = null
    private var lastError: String? = null

    @Throws(IOException::class)
    override fun setup(ctx: Context) {
        val model = SharedPreferencesHelper.get(ctx, SharedPreferencesHelper.LLAMACPP_MODEL_PATH)
        if (model.isNullOrEmpty()) {
            throw IOException("llama.cpp model path not configured. Please set it in Settings.")
        }
        if (!File(model).exists()) {
            throw IOException("llama.cpp model file not found at: $model")
        }

        val mmproj = SharedPreferencesHelper.get(ctx, SharedPreferencesHelper.LLAMACPP_MMPROJ_PATH)
        if (mmproj.isNullOrEmpty()) {
            throw IOException("llama.cpp mmproj path not configured. Please set it in Settings.")
        }
        if (!File(mmproj).exists()) {
            throw IOException("llama.cpp mmproj file not found at: $mmproj")
        }

        modelPath = model
        mmprojPath = mmproj
    }

    override fun getLastResponse(): String = lastResponse ?: ""

    override fun getLastError(): String = lastError ?: ""

    @Throws(IOException::class, JSONException::class)
    override fun analyzeImage(systemPrompt: String, userPrompt: String?, imagePath: String): String {
        lastResponse = null
        lastError = null
        try {
            LlamaCppEngine.loadModel(modelPath, mmprojPath)
            val response = LlamaCppEngine.infer(systemPrompt, userPrompt, imagePath)
            lastResponse = response
            return response
        } catch (oom: OutOfMemoryError) {
            val msg = "Out of memory loading llama.cpp model. Close other apps and retry."
            Log.e(TAG, msg, oom)
            lastError = msg
            closeModel()
            return ""
        } catch (e: Exception) {
            Log.e(TAG, "llama.cpp inference error", e)
            lastError = e.message ?: "Unknown error"
            return ""
        }
    }

    fun detectPerson(ctx: Context, imagePath: String?): Int {
        var newPath: String? = null
        return try {
            if (imagePath == null) {
                lastError = "imagePath is null"
                return -1
            }
            newPath = Util.contentToFile(ctx, imagePath)
            val response = analyzeImage(PROMPT_SYSTEM, null, newPath)
            if (response.isEmpty()) return -1
            lastResponse = response
            val firstLine = response.split("\n").firstOrNull()?.trim() ?: ""
            when (firstLine) {
                "HUMAN" -> 100
                "NONE", "SPIDER", "CAT" -> 0
                "UNCERTAIN" -> 30
                else -> -1
            }
        } catch (e: Exception) {
            Log.e(TAG, "detectPerson failed", e)
            lastError = e.message ?: "Unknown error"
            -1
        } finally {
            if (newPath != null && newPath != imagePath) {
                File(newPath).delete()
            }
        }
    }
}
