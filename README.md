# Airline Baggage Policy Chatbot

A local RAG-powered Android chatbot that answers questions about airline baggage policies using on-device AI.

## Features

- 🤖 **Local AI**: Runs completely on-device using MediaPipe LLM Inference
- 🔍 **Vector Search**: Uses Couchbase Lite with vector search for semantic retrieval
- 📚 **RAG Pipeline**: Retrieval Augmented Generation for accurate, context-aware responses
- ✈️ **Airline Policies**: Pre-loaded with baggage policies for Lufthansa, Ryanair, Emirates, and Southwest
- 💬 **Streamlit-like UI**: Clean, modern chat interface built with Jetpack Compose

## Architecture

- **Database**: Couchbase Lite 3.3 with Vector Search extension
- **Embeddings**: MediaPipe Text Embedder (Universal Sentence Encoder)
- **LLM**: gemma-3-1b-it-Q8_0.gguf (1.07 GB) OR Llama-3.2-3B-Instruct-Q4_K_S.gguf (1.93 GB) via Llamatik
- **UI**: Jetpack Compose with Material 3

## Required Model Files

Before building the app, you need to download two model files and place them in the `app/src/main/assets` folder:

### 1. Embedding Model

**Option A: Universal Sentence Encoder (Recommended)**
- Download from: [TensorFlow Hub](https://tfhub.dev/google/universal-sentence-encoder-lite/2)
- File name: `universal_sentence_encoder.tflite`
- Size: ~50MB

**Option B: Alternative Embedding Models**
You can use other TFLite embedding models. Update the `MODEL_NAME` in `ml/EmbeddingModel.kt` accordingly.

### 2. LLM Model

**Option 1: Gemma3 1B (int4 quantized)**
- Download from: LLMStudio
- Accessible through Llamatik (llama.cpp JNI bridge)
- Recommended file: `gemma-3-1b-it-Q8_0.gguf` 
- Size: ~1.07GB
- Alternative: You can use Gemma3 1B for smaller size (~529MB)

**Option 2: Llama-3.2-3B (int4 quantized)**
- Download from: LLMStudio
- Accessible through Llamatik (llama.cpp JNI bridge)
- Recommended file:  `Llama-3.2-3B-Instruct-Q4_K_S.gguf`
- Size: ~1.93GB
- Alternative: You can use DeepSeek R1 1.5B like `DeepSeek-R1-Distill-Qwen-1.5B-Q8_0.gguf` of size 1.89GB

**Download Steps:**
1. Open LLMStudio and search your LLM
2. Download the LLM model, for example `Llama-3.2-3B-Instruct-Q4_K_S.gguf`
3. Place in `app/src/main/assets/`

### Assets Folder Structure

```
app/src/main/assets/
├── baggage_policies/
│   ├── lufthansa.txt
│   ├── ryanair.txt
│   ├── emirates.txt
│   └── southwest.txt
├── universal_sentence_encoder.tflite
└── gemma-2b-it-gpu-int4.bin
```

## Build Requirements

- Android Studio Hedgehog or later
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 36
- Recommended device: 4GB+ RAM, Android 10+

## Couchbase Lite Enterprise License

This app uses Couchbase Lite Enterprise Edition for vector search capabilities. For production use, you'll need a valid license. For evaluation and development, you can use the Community License.

## Building the App

1. Clone the repository
2. Download and place the model files in `app/src/main/assets/` (see above)
3. Open the project in Android Studio
4. Sync Gradle files
5. Build and run on a physical device (emulator may be slow)

## First Launch

On first launch, the app will:
1. Initialize the Couchbase Lite database
2. Load the baggage policy documents
3. Generate embeddings for all document chunks (highly depends on the number of documents: for 4 small docs it takes a few seconds)
4. Store vectors in the database

Subsequent launches will be instant as the database is already initialized.

## Usage

Ask questions like:
- "What is Lufthansa's baggage policy?"
- "How many bags can I bring on Emirates?"
- "Which airline allows free checked bags?"
- "What's the weight limit for Ryanair?"
- "Compare baggage allowances between Southwest and Lufthansa"

## Performance Notes

- **First query**: MAY VARY from 5-10 seconds MINIMUM as the LLM loads into memory, up to 1-3 minutes depending on target devices (OLD/NEW) and GPU acceleration availability
- **Subsequent queries**: MAY VARY from 2-5 seconds up to 1-3 minutes depending on target devices (OLD/NEW) and GPU acceleration availability
- **Memory usage**: ~2-3GB during inference
- **Storage**: ~2GB for models + database

## Troubleshooting

### "Model file not found" error
- Ensure model files are in the correct `assets` folder
- Check file names match exactly
- Rebuild the project after adding files

### Out of memory errors
- Enable `largeHeap` in AndroidManifest.xml (already configured)
- Close other apps before running
- Use a device with at least 4GB RAM

### Slow performance
- Use a device with Android 10+ and 4GB+ RAM
- Ensure GPU acceleration is available
- Consider using Gemma 1B instead of 2B

## License

This project is for educational and evaluation purposes. Ensure you comply with:
- Couchbase Lite Enterprise license terms
- Google's Gemma model license

## Credits

- **Couchbase Lite**: Vector database
- **MediaPipe**: On-device ML inference
- **Google Gemma**: Language model
- **Jetpack Compose**: UI framework
# local_RAG_Android_using_CBLite_VectorSearch
