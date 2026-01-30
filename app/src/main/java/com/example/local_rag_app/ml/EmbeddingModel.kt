package com.example.local_rag_app.ml

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class EmbeddingModel(private val context: Context) {
    
    private var textEmbedder: TextEmbedder? = null
    private val TAG = "EmbeddingModel"
    
    // Using Universal Sentence Encoder model
    // Note: You'll need to download this model and place it in assets
    private val MODEL_NAME = "universal_sentence_encoder.tflite"
    
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            // Check if model exists in assets, if not we'll need to handle it
            val modelFile = getModelFile()
            
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_NAME)
                .build()
            
            val options = TextEmbedder.TextEmbedderOptions.builder()
                .setBaseOptions(baseOptions)
                .build()
            
            textEmbedder = TextEmbedder.createFromOptions(context, options)
            
            Log.d(TAG, "Embedding model initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing embedding model", e)
            Log.e(TAG, "Make sure to place the embedding model in assets folder")
            throw e
        }
    }
    
    private fun getModelFile(): File {
        val modelFile = File(context.filesDir, MODEL_NAME)
        
        if (!modelFile.exists()) {
            try {
                context.assets.open(MODEL_NAME).use { input ->
                    FileOutputStream(modelFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Model file not found in assets: $MODEL_NAME", e)
            }
        }
        
        return modelFile
    }
    
    suspend fun embed(text: String): FloatArray = withContext(Dispatchers.IO) {
        try {
            val result = textEmbedder?.embed(text)
            val embedding = result?.embeddingResult()?.embeddings()?.firstOrNull()
            
            if (embedding != null) {
                // Convert to FloatArray
                val floatArray = FloatArray(embedding.floatEmbedding().size)
                for (i in floatArray.indices) {
                    floatArray[i] = embedding.floatEmbedding()[i]
                }
                floatArray
            } else {
                Log.e(TAG, "No embedding generated for text")
                FloatArray(100) // Return zero vector as fallback
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating embedding", e)
            FloatArray(100) // Return zero vector as fallback
        }
    }
    
    suspend fun embedBatch(texts: List<String>): List<FloatArray> = withContext(Dispatchers.IO) {
        texts.map { embed(it) }
    }
    
    fun close() {
        try {
            textEmbedder?.close()
            Log.d(TAG, "Embedding model closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing embedding model", e)
        }
    }
}
