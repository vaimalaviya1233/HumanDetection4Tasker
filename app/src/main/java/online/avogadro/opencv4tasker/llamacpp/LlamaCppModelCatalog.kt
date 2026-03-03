package online.avogadro.opencv4tasker.llamacpp

data class GgufModelInfo(
    val displayName: String,
    val modelUrl: String,
    val mmprojUrl: String,
    val modelFilename: String,
    val mmprojFilename: String,
    val totalSizeMB: Int
)

object LlamaCppModelCatalog {
    val MODELS = listOf(
        GgufModelInfo(
            "SmolVLM2 256M (279 MB)",
            "https://huggingface.co/ggml-org/SmolVLM2-256M-Video-Instruct-GGUF/resolve/main/SmolVLM2-256M-Video-Instruct-Q8_0.gguf",
            "https://huggingface.co/ggml-org/SmolVLM2-256M-Video-Instruct-GGUF/resolve/main/mmproj-SmolVLM2-256M-Video-Instruct-Q8_0.gguf",
            "SmolVLM2-256M-Video-Instruct-Q8_0.gguf",
            "mmproj-SmolVLM2-256M-Video-Instruct-Q8_0.gguf",
            279
        ),
        GgufModelInfo(
            "SmolVLM2 500M (546 MB)",
            "https://huggingface.co/ggml-org/SmolVLM2-500M-Video-Instruct-GGUF/resolve/main/SmolVLM2-500M-Video-Instruct-Q8_0.gguf",
            "https://huggingface.co/ggml-org/SmolVLM2-500M-Video-Instruct-GGUF/resolve/main/mmproj-SmolVLM2-500M-Video-Instruct-Q8_0.gguf",
            "SmolVLM2-500M-Video-Instruct-Q8_0.gguf",
            "mmproj-SmolVLM2-500M-Video-Instruct-Q8_0.gguf",
            546
        ),
        GgufModelInfo(
            "InternVL3 1B (1.01 GB)",
            "https://huggingface.co/ggml-org/InternVL3-1B-Instruct-GGUF/resolve/main/InternVL3-1B-Instruct-Q8_0.gguf",
            "https://huggingface.co/ggml-org/InternVL3-1B-Instruct-GGUF/resolve/main/mmproj-InternVL3-1B-Instruct-Q8_0.gguf",
            "InternVL3-1B-Instruct-Q8_0.gguf",
            "mmproj-InternVL3-1B-Instruct-Q8_0.gguf",
            1010
        ),
        GgufModelInfo(
            "InternVL3 2B (1.46 GB)",
            "https://huggingface.co/ggml-org/InternVL3-2B-Instruct-GGUF/resolve/main/InternVL3-2B-Instruct-Q4_K_M.gguf",
            "https://huggingface.co/ggml-org/InternVL3-2B-Instruct-GGUF/resolve/main/mmproj-InternVL3-2B-Instruct-Q8_0.gguf",
            "InternVL3-2B-Instruct-Q4_K_M.gguf",
            "mmproj-InternVL3-2B-Instruct-Q8_0.gguf",
            1460
        ),
        GgufModelInfo(
            "SmolVLM2 2.2B (1.70 GB)",
            "https://huggingface.co/ggml-org/SmolVLM2-2.2B-Instruct-GGUF/resolve/main/SmolVLM2-2.2B-Instruct-Q4_K_M.gguf",
            "https://huggingface.co/ggml-org/SmolVLM2-2.2B-Instruct-GGUF/resolve/main/mmproj-SmolVLM2-2.2B-Instruct-Q8_0.gguf",
            "SmolVLM2-2.2B-Instruct-Q4_K_M.gguf",
            "mmproj-SmolVLM2-2.2B-Instruct-Q8_0.gguf",
            1700
        ),
    )
}
