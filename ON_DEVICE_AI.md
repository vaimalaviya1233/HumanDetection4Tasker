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

## ⛔ Limitazione bloccante per AWS4Tasker

> **"GenAI API inference is permitted only when the app is the top foreground application"**

L'ML Kit GenAI **non funziona in background**. Poiché AWS4Tasker viene tipicamente invocato da Tasker/MacroDroid come action in background, questa limitazione rende l'integrazione impraticabile allo stato attuale.

Un possibile workaround sarebbe lanciare una `Activity` temporanea che va in foreground, esegue l'inference e si chiude — ma è un approccio non elegante e con UX problematica.

**Stato:** ⏸ In attesa che Google rimuova il vincolo foreground-only, o che emerga un workaround affidabile.

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
