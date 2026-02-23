# On-Device LLM su Android — Appunti di ricerca

> Ricerca effettuata: febbraio 2026
> Contesto: valutazione per AWS4Tasker (plugin Tasker per analisi immagini AI)

---

## API disponibili

### 1. ML Kit GenAI APIs (via Android AICore) — La strada ufficiale Google

API di alto livello che usano **Gemini Nano** già installato nel sistema tramite `AICore` (servizio di sistema Android). Il modello (~4GB) è gestito dal sistema, non dall'app.

**Funzionalità:**
| API | Input | Output |
|-----|-------|--------|
| **Prompt API** | testo + immagine (multimodale) | testo libero |
| **Image Description** | immagine | descrizione breve |
| **Summarization** | testo | riassunto |
| **Proofreading / Rewriting** | testo | testo corretto/riscritto |
| **Speech Recognition** | audio | testo |

**Hardware richiesto:** Qualcomm Snapdragon, Google Tensor, MediaTek Dimensity top di gamma.
**Completamente offline:** ✅ Sì.

**Dispositivi supportati (febbraio 2026):**
| Famiglia | Modelli |
|----------|---------|
| Samsung Galaxy | **S25 / S25+ / S25 Ultra** ✅, Z Fold7 |
| Google Pixel | Pixel 9, 9 Pro, 9 Pro XL, 9 Pro Fold, Pixel 10 series ✅ |
| OnePlus | 13, Pad 3 |
| OPPO | Find N5, Find X8 series |
| Honor | Magic 6/7 series, V3, V5 |
| Xiaomi | 15 series |
| Motorola | Razr 60 Ultra |
| iQOO | 13, 15 |
| vivo | X200, X300 |
| realme | GT 7 series |
| POCO | F7, X7 Pro |

**Integrazione Android (Prompt API con immagine):**
```kotlin
val promptRequest = PromptRequest.builder()
    .addTextPart("Describe this image")
    .addImagePart(bitmap)
    .build()
promptClient.generateText(promptRequest)
```

---

### 2. MediaPipe LLM Inference API — Sperimentale, modelli custom

Permette di caricare modelli custom (Gemma, Phi-2, Falcon, ecc.) e fare inferenza su CPU/GPU/NPU. Funziona su più dispositivi rispetto ad AICore.

- **Pro:** più dispositivi supportati, modelli intercambiabili
- **Contro:** marcato `experimental and research use only` da Google, non adatto a produzione

---

### 3. LiteRT (ex TensorFlow Lite) — Framework basso livello

Già usato nel progetto per MediaPipe object detection. Non pensato per LLM generici ma per modelli specializzati e piccoli.

---

## Prestazioni su S25 Ultra

Su Snapdragon 8 Elite (S25 Ultra) con LiteRT + NPU:
- **TTFT (time-to-first-token) su immagini:** ~0.12 secondi
- NPU offre **3x speedup** rispetto a GPU su prefill
- Benchmark: Gemma 3 1B su S25 Ultra con LiteRT supera nettamente llama.cpp

---

## Confronto con le API cloud attuali

| Criterio | ML Kit GenAI (Gemini Nano) | Claude/Gemini Cloud |
|----------|---------------------------|---------------------|
| Offline | ✅ Completamente offline | ❌ Richiede internet |
| Privacy | ✅ Tutto locale | ⚠️ Dati al cloud |
| Qualità analisi | ⚠️ Buona ma limitata (nano) | ✅ Molto alta |
| Background | ❌ **Solo foreground** | ✅ Funziona in background |
| Costo | ✅ Gratis | ⚠️ A consumo |
| Dispositivi | Solo flagship 2024+ | Qualsiasi |
| Latenza | ✅ ~0.12s TTFT su S25 Ultra | ⚠️ Dipende da rete |

---

## ⛔ Limitazione bloccante: ML Kit GenAI è foreground-only

> **"GenAI API inference is permitted only when the app is the top foreground application"**
> **"including using a foreground service, will result in an `ErrorCode.BACKGROUND_USE_BLOCKED` response"**

Questa restrizione è **deliberata e hardcoded in AICore**. Non esiste workaround ufficiale:
- Non funziona con `ForegroundService` (bloccato esplicitamente)
- Non funziona con `WorkManager`
- Non funziona se l'app è in background

Poiché AWS4Tasker viene tipicamente invocato da Tasker/MacroDroid come action in background, questa limitazione rende l'integrazione impraticabile allo stato attuale.

**Stato:** ⏸ In attesa che Google rimuova il vincolo foreground-only.

---

## Indagine su alternative proprietarie (Samsung / Pixel)

### Samsung Neural SDK
- Storico SDK proprietario Samsung per inferenza neurale su Exynos/Snapdragon
- **❌ Non più disponibile per sviluppatori terze parti** (download policy cambiata)
- Sostituito da Exynos AI Studio (toolchain di compilazione modelli, non un'API runtime pubblica)

### Samsung Galaxy AI Engine / Exynos AI Studio
- Toolkit per ottimizzare e compilare modelli AI per hardware Samsung
- **Non è un'API runtime per sviluppatori Android standard** — è uno strumento interno/OEM
- Nessuna API pubblica per inferenza LLM in background su S25

### Pixel / Google AI Edge SDK
- Sperimentale, disponibile solo su Pixel 9 series
- Basato sempre su AICore → stessa restrizione foreground

**Conclusione:** né Google né Samsung offrono una via ufficiale per inferenza LLM on-device in background su dispositivi consumer.

---

## Alternative open source: inferenza LLM senza restrizioni foreground

Queste librerie girano come codice dell'app, senza passare per AICore, e **non hanno restrizioni foreground**.

### Cactus ⭐ (più promettente per AWS4Tasker)
- **Sito:** https://cactuscompute.com
- **GitHub:** https://github.com/cactus-compute/cactus
- Y Combinator backed; engine C++ con SDK Kotlin nativo per Android
- Supporta **modelli vision multimodali** (LFM2-VL series) → analisi immagini ✅
- Ottimizzato per ARM: Snapdragon, Google Tensor, Exynos, MediaTek
- Nessuna restrizione foreground: funziona in `Service` background
- Supporta NPU (INT4, quantizzazione)
- API OpenAI-compatibile (Kotlin)
- Open source

**Modelli supportati:**
| Tipo | Esempi |
|------|--------|
| LLM testo | Gemma 3 (270m-1b), Qwen (0.6B-1.7B), LFM (350M-8B) |
| **Vision/Multimodale** | **LFM2-VL** (analisi immagini) ✅ |
| Speech | Whisper, Moonshine |
| Embedding | vari |

**Prestazioni indicative (budget device Galaxy A17 5G):**
- LFM2-350M: 87 tps prefill / 24 tps decode, ~395MB RAM

### MLC LLM
- **GitHub:** https://github.com/mlc-ai/mlc-llm
- Engine ML con compilazione ottimizzata; SDK Kotlin + Gradle Android
- Backend: OpenCL, Vulkan, CUDA
- Nessuna restrizione foreground
- Più complesso da integrare, richiede compilazione modelli

### MediaPipe LLM Inference API (Google, sperimentale)
- Già nel progetto (dipendenza MediaPipe)
- Supporta Gemma, Phi-2, Falcon, Stable LM
- Nessuna restrizione foreground (non usa AICore)
- Marcato `experimental and research use only` — non consigliato per produzione

### llama.cpp via NDK
- Approccio "artigianale": compilare llama.cpp con Android NDK e chiamarlo via JNI
- Funziona in background senza restrizioni
- Molto flessibile (qualsiasi modello GGUF)
- Alta complessità di integrazione e manutenzione

---

## Tabella comparativa completa

| Soluzione | Background | Vision | S25 Ultra | Pixel | Qualità | Complessità |
|-----------|-----------|--------|-----------|-------|---------|-------------|
| ML Kit GenAI (AICore) | ❌ foreground only | ✅ Prompt API | ✅ | ✅ | ✅ alta (Nano) | bassa |
| **Cactus** | ✅ | ✅ LFM2-VL | ✅ | ✅ | ⚠️ modelli piccoli | **bassa** |
| MLC LLM | ✅ | ⚠️ parziale | ✅ | ✅ | ⚠️ | media |
| MediaPipe LLM (exp.) | ✅ | ⚠️ | ✅ | ✅ | ⚠️ | media |
| llama.cpp NDK | ✅ | ✅ GGUF VLM | ✅ | ✅ | ✅ | alta |

---

## Raccomandazione aggiornata

Per AWS4Tasker, la strada più praticabile oggi per on-device background inference è **Cactus**:
- SDK Kotlin nativo → integrazione analoga agli engine esistenti
- Supporto vision multimodale (immagini)
- Funziona in background senza restrizioni
- Ottimizzato per i chipset di S25 Ultra e Pixel
- Non richiede download/gestione modelli di sistema

Il limite principale rimane la qualità inferenziale: i modelli embedded (sub-1B) sono molto meno capaci di Claude/Gemini cloud per analisi complesse. Ideale per task semplici (presenza umana, classificazione basica) con fallback cloud per analisi approfondite.

---

## Fonti

- [ML Kit GenAI APIs Overview](https://developers.google.com/ml-kit/genai)
- [ML Kit GenAI APIs - Android Developers](https://developer.android.com/ai/gemini-nano/ml-kit-genai)
- [GenAI Prompt API](https://developers.google.com/ml-kit/genai/prompt/android)
- [GenAI Image Description API](https://developers.google.com/ml-kit/genai/image-description/android)
- [On-device GenAI APIs as part of ML Kit - Blog](https://android-developers.googleblog.com/2025/05/on-device-gen-ai-apis-ml-kit-gemini-nano.html)
- [ML Kit Prompt API Alpha - Blog](https://android-developers.googleblog.com/2025/10/ml-kit-genai-prompt-api-alpha-release.html)
- [Samsung Galaxy S25 multimodal Gemini Nano - Android Police](https://www.androidpolice.com/samsung-galaxy-s25-multimodal-gemini-nano/)
- [LiteRT Universal Framework - Google Blog](https://developers.googleblog.com/litert-the-universal-framework-for-on-device-ai/)
- [LLM Inference guide Android - Google AI Edge](https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android)
- [Samsung Neural SDK - Samsung Developer](https://developer.samsung.com/neural/overview.html)
- [Unpacking Samsung's On-Device AI SDK Strategy](https://semiconductor.samsung.com/news-events/tech-blog/unpacking-samsungs-comprehensive-on-device-ai-sdk-toolchain-strategy/)
- [Cactus GitHub](https://github.com/cactus-compute/cactus)
- [Cactus v1 - InfoQ](https://www.infoq.com/news/2025/12/cactus-on-device-inference/)
- [MLC LLM Android SDK](https://llm.mlc.ai/docs/deploy/android.html)
- [Automate Android with local LLM](https://code.mendhak.com/automate-android-with-local-llm/)
