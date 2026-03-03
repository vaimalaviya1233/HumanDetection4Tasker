package online.avogadro.opencv4tasker.llamacpp

import android.os.Build
import android.util.Log

class LlamaCppEngine {

    companion object {
        private const val TAG = "LlamaCppEngine"

        /** true if the native library is loaded and available */
        val isNativeAvailable: Boolean by lazy {
            val isArm64 = Build.SUPPORTED_ABIS.any { it == "arm64-v8a" }
            if (!isArm64) return@lazy false

            try {
                System.loadLibrary("llamacpp_jni")
                true
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "llama.cpp native library not available", e)
                false
            }
        }

        @Volatile
        private var handle: Long = 0L
        private var loadedModelPath: String? = null
        private var loadedMmprojPath: String? = null

        @Synchronized
        fun loadModel(modelPath: String, mmprojPath: String, nCtx: Int = 2048, nGpuLayers: Int = 0) {
            if (handle != 0L && loadedModelPath == modelPath && loadedMmprojPath == mmprojPath) {
                Log.i(TAG, "Model already loaded, reusing")
                return
            }
            closeModel()
            val engine = LlamaCppEngine()
            handle = engine.nativeLoadModel(modelPath, mmprojPath, nCtx, nGpuLayers)
            if (handle == 0L) {
                throw RuntimeException("Failed to load model: $modelPath")
            }
            loadedModelPath = modelPath
            loadedMmprojPath = mmprojPath
        }

        @Synchronized
        fun infer(systemPrompt: String, userPrompt: String?, imagePath: String?, maxTokens: Int = 512): String {
            if (handle == 0L) throw IllegalStateException("Model not loaded. Call loadModel() first.")
            val engine = LlamaCppEngine()
            return engine.nativeInfer(handle, systemPrompt, userPrompt ?: "", imagePath ?: "", maxTokens)
        }

        @Synchronized
        fun closeModel() {
            if (handle != 0L) {
                try {
                    val engine = LlamaCppEngine()
                    engine.nativeFree(handle)
                } catch (e: Exception) {
                    Log.w(TAG, "Error freeing model", e)
                }
                handle = 0L
                loadedModelPath = null
                loadedMmprojPath = null
            }
        }
    }

    external fun nativeLoadModel(modelPath: String, mmprojPath: String, nCtx: Int, nGpuLayers: Int): Long
    external fun nativeInfer(handle: Long, systemPrompt: String, userPrompt: String, imagePath: String, maxTokens: Int): String
    external fun nativeFree(handle: Long)
}
