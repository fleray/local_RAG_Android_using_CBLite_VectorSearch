package com.example.local_rag_app.rag

import android.content.Context
import android.util.Log
import com.llamatik.library.platform.LlamaBridge
import com.llamatik.library.platform.GenStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LlmInference(private val context: Context) {
    
    private val TAG = "LlmInference"
    private var isModelLoaded = false
    
    // Model file name - should be placed in assets folder
    // Ensure this file matches what is in your assets
    private val MODEL_NAME = "Qwen2.5-1.5B-Instruct.Q8_0.gguf"
    
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            val modelFile = java.io.File(context.filesDir, MODEL_NAME)
            
            // Copy from assets to internal storage if not already there
            if (!modelFile.exists()) {
                Log.d(TAG, "Copying model from assets...")
                try {
                    context.assets.open(MODEL_NAME).use { input ->
                        modelFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    Log.d(TAG, "Model copied successfully.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error copying model: ${e.message}")
                    Log.e(TAG, "System: Error copying model file. Please ensure '$MODEL_NAME' is in assets.")
                    throw e
                }
            }
            
            val modelPath = modelFile.absolutePath
            
            Log.d(TAG, "Initializing LlamaBridge with path: $modelPath")
            val success = LlamaBridge.initGenerateModel(modelPath)
            if (success) {
                isModelLoaded = true
                Log.d(TAG, "LLM initialized successfully")
            } else {
                Log.e(TAG, "Failed to initialize LLM")
                throw RuntimeException("Failed to initialize LlamaBridge")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing LLM", e)
            Log.e(TAG, "Make sure to download and place the Llama model in assets folder")
            throw e
        }
    }
    

    
    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
        if (!isModelLoaded) {
            return@withContext "Error: Model not initialized."
        }

        suspendCancellableCoroutine { continuation ->
            val fullResponse = StringBuilder()
            
            val callback = object : GenStream {
                override fun onDelta(text: String) {
                    fullResponse.append(text)
                }

                override fun onComplete() {
                    Log.d(TAG, "Generation complete")
                    if (continuation.isActive) {
                        continuation.resume(fullResponse.toString())
                    }
                }

                override fun onError(message: String) {
                    Log.e(TAG, "Error generating text: $message")
                    if (continuation.isActive) {
                        continuation.resumeWithException(RuntimeException(message))
                    }
                }
            }

            try {
                LlamaBridge.generateStream(prompt, callback)
            } catch (e: Exception) {
                Log.e(TAG, "Exception during generation", e)
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }
    
    suspend fun generateResponseWithContext(
        userQuery: String,
        context: String
    ): String = withContext(Dispatchers.IO) {
        val prompt = buildPrompt(userQuery, context)
        generateResponse(prompt)
    }
    
    private fun buildPrompt(userQuery: String, context: String): String {
        return """You are a helpful airline baggage policy assistant. Use the following context to answer the user's question accurately and concisely. NEVER repeat the question and NEVER expose your thoughts.

Context:
$context

User Question: $userQuery

Answer: Provide a clear, helpful answer based on the context above. NEVER repeat the question and NEVER expose your thoughts. If the context doesn't contain enough information to answer the question, say so politely."""
    }
    
    fun close() {
        // LlamaBridge doesn't seem to have a close/release method exposed in the reference code
        // But if it did, we would call it here.
        Log.d(TAG, "LLM closed (no-op for LlamaBridge)")
    }
}
