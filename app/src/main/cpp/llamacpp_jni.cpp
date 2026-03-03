#include <jni.h>
#include <string>
#include <android/log.h>

#include "llama.h"
#include "mtmd.h"
#include "mtmd-helper.h"

#define TAG "LlamaCppJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

struct LlamaCppHandle {
    llama_model   *model   = nullptr;
    llama_context *ctx     = nullptr;
    mtmd_context  *mtmd_ctx = nullptr;
};

static std::string jstring_to_string(JNIEnv *env, jstring js) {
    if (!js) return "";
    const char *c = env->GetStringUTFChars(js, nullptr);
    std::string s(c);
    env->ReleaseStringUTFChars(js, c);
    return s;
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_online_avogadro_opencv4tasker_llamacpp_LlamaCppEngine_nativeLoadModel(
        JNIEnv *env, jobject /*thiz*/,
        jstring modelPath, jstring mmprojPath, jint nCtx, jint nGpuLayers) {

    std::string model_path  = jstring_to_string(env, modelPath);
    std::string mmproj_path = jstring_to_string(env, mmprojPath);

    LOGI("Loading model: %s", model_path.c_str());

    // Load model
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = nGpuLayers;

    llama_model *model = llama_model_load_from_file(model_path.c_str(), model_params);
    if (!model) {
        LOGE("Failed to load model from: %s", model_path.c_str());
        return 0;
    }

    // Create context
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx   = nCtx > 0 ? nCtx : 2048;
    ctx_params.n_batch = 512;

    llama_context *ctx = llama_init_from_model(model, ctx_params);
    if (!ctx) {
        LOGE("Failed to create llama context");
        llama_model_free(model);
        return 0;
    }

    // Initialize mtmd (multimodal) context if mmproj is provided
    mtmd_context *mtmd_ctx = nullptr;
    if (!mmproj_path.empty()) {
        LOGI("Loading mmproj: %s", mmproj_path.c_str());
        mtmd_context_params mtmd_params = mtmd_context_params_default();
        mtmd_params.use_gpu  = false; // CPU for vision encoder (safer on mobile)
        mtmd_params.n_threads = 4;

        mtmd_ctx = mtmd_init_from_file(mmproj_path.c_str(), model, mtmd_params);
        if (!mtmd_ctx) {
            LOGE("Failed to load mmproj from: %s", mmproj_path.c_str());
            llama_free(ctx);
            llama_model_free(model);
            return 0;
        }
        LOGI("Multimodal context initialized successfully");
    }

    auto *handle = new LlamaCppHandle();
    handle->model    = model;
    handle->ctx      = ctx;
    handle->mtmd_ctx = mtmd_ctx;

    LOGI("Model loaded successfully (ctx=%d)", nCtx);
    return reinterpret_cast<jlong>(handle);
}

JNIEXPORT jstring JNICALL
Java_online_avogadro_opencv4tasker_llamacpp_LlamaCppEngine_nativeInfer(
        JNIEnv *env, jobject /*thiz*/,
        jlong handlePtr, jstring systemPrompt, jstring userPrompt,
        jstring imagePath, jint maxTokens) {

    if (handlePtr == 0) {
        return env->NewStringUTF("ERROR: model not loaded");
    }

    auto *handle = reinterpret_cast<LlamaCppHandle *>(handlePtr);
    std::string sys_prompt  = jstring_to_string(env, systemPrompt);
    std::string user_prompt = jstring_to_string(env, userPrompt);
    std::string image_path  = jstring_to_string(env, imagePath);
    int max_tokens = maxTokens > 0 ? maxTokens : 512;

    const llama_vocab *vocab = llama_model_get_vocab(handle->model);

    // Build the prompt using the model's chat template
    const char *tmpl = llama_model_chat_template(handle->model, nullptr);

    std::string full_user;
    bool has_image = !image_path.empty() && handle->mtmd_ctx;

    if (has_image) {
        // Insert the media marker for mtmd to replace with image tokens
        const char *marker = mtmd_default_marker();
        full_user = std::string(marker) + "\n" + user_prompt;
    } else {
        full_user = user_prompt;
    }

    // Apply chat template
    std::vector<llama_chat_message> messages;
    if (!sys_prompt.empty()) {
        messages.push_back({"system", sys_prompt.c_str()});
    }
    messages.push_back({"user", full_user.c_str()});

    // First call to get required size
    int32_t needed = llama_chat_apply_template(
            tmpl, messages.data(), messages.size(), true, nullptr, 0);
    if (needed < 0) {
        LOGE("llama_chat_apply_template failed (size query)");
        return env->NewStringUTF("ERROR: chat template failed");
    }

    std::vector<char> buf(needed + 1);
    int32_t written = llama_chat_apply_template(
            tmpl, messages.data(), messages.size(), true, buf.data(), buf.size());
    if (written < 0) {
        LOGE("llama_chat_apply_template failed");
        return env->NewStringUTF("ERROR: chat template failed");
    }
    buf[written] = '\0';
    std::string formatted_prompt(buf.data(), written);

    // Clear memory (KV cache) for fresh generation
    llama_memory_clear(llama_get_memory(handle->ctx), true);

    llama_pos n_past = 0;

    if (has_image) {
        // Multimodal path: tokenize with mtmd, which replaces the marker with image embeddings
        mtmd_input_text input_text;
        input_text.text         = formatted_prompt.c_str();
        input_text.add_special  = true;
        input_text.parse_special = true;

        // Load image bitmap
        mtmd_bitmap *bmp = mtmd_helper_bitmap_init_from_file(handle->mtmd_ctx, image_path.c_str());
        if (!bmp) {
            LOGE("Failed to load image: %s", image_path.c_str());
            return env->NewStringUTF("ERROR: failed to load image");
        }

        const mtmd_bitmap *bitmaps[] = { bmp };

        mtmd_input_chunks *chunks = mtmd_input_chunks_init();
        int32_t tok_res = mtmd_tokenize(handle->mtmd_ctx, chunks, &input_text, bitmaps, 1);
        mtmd_bitmap_free(bmp);

        if (tok_res != 0) {
            LOGE("mtmd_tokenize failed: %d", tok_res);
            mtmd_input_chunks_free(chunks);
            return env->NewStringUTF("ERROR: multimodal tokenization failed");
        }

        // Evaluate all chunks (text + image embeddings)
        llama_pos new_n_past = 0;
        int32_t eval_res = mtmd_helper_eval_chunks(
                handle->mtmd_ctx, handle->ctx, chunks,
                0, 0, 512, true, &new_n_past);

        mtmd_input_chunks_free(chunks);

        if (eval_res != 0) {
            LOGE("mtmd_helper_eval_chunks failed: %d", eval_res);
            return env->NewStringUTF("ERROR: multimodal eval failed");
        }
        n_past = new_n_past;
    } else {
        // Text-only path: simple tokenization
        int n_prompt_tokens = -llama_tokenize(vocab, formatted_prompt.c_str(),
                                              formatted_prompt.size(), nullptr, 0, true, true);
        std::vector<llama_token> prompt_tokens(n_prompt_tokens);
        llama_tokenize(vocab, formatted_prompt.c_str(), formatted_prompt.size(),
                       prompt_tokens.data(), prompt_tokens.size(), true, true);

        // Evaluate prompt
        llama_batch batch = llama_batch_get_one(prompt_tokens.data(), prompt_tokens.size());
        if (llama_decode(handle->ctx, batch) != 0) {
            LOGE("llama_decode failed for prompt");
            return env->NewStringUTF("ERROR: prompt eval failed");
        }
        n_past = prompt_tokens.size();
    }

    // Set up sampler
    llama_sampler_chain_params sparams = llama_sampler_chain_default_params();
    llama_sampler *smpl = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(smpl, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(smpl, llama_sampler_init_top_p(0.9f, 1));
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(0.7f));
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(0));

    // Generation loop
    std::string result;
    llama_token eos = llama_vocab_eos(vocab);

    for (int i = 0; i < max_tokens; i++) {
        llama_token new_token = llama_sampler_sample(smpl, handle->ctx, -1);

        if (llama_vocab_is_eog(vocab, new_token)) {
            break;
        }

        // Convert token to text
        char piece[256];
        int n = llama_token_to_piece(vocab, new_token, piece, sizeof(piece), 0, true);
        if (n > 0) {
            result.append(piece, n);
        }

        // Prepare next batch
        llama_batch next_batch = llama_batch_get_one(&new_token, 1);
        if (llama_decode(handle->ctx, next_batch) != 0) {
            LOGE("llama_decode failed at token %d", i);
            break;
        }
        n_past++;
    }

    llama_sampler_free(smpl);

    LOGI("Generated %zu chars", result.size());
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_online_avogadro_opencv4tasker_llamacpp_LlamaCppEngine_nativeFree(
        JNIEnv *env, jobject /*thiz*/, jlong handlePtr) {

    if (handlePtr == 0) return;

    auto *handle = reinterpret_cast<LlamaCppHandle *>(handlePtr);

    if (handle->mtmd_ctx) {
        mtmd_free(handle->mtmd_ctx);
    }
    if (handle->ctx) {
        llama_free(handle->ctx);
    }
    if (handle->model) {
        llama_model_free(handle->model);
    }

    delete handle;
    LOGI("Model freed");
}

} // extern "C"
