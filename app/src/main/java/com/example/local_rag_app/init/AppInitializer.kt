package com.example.local_rag_app.init

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.local_rag_app.database.DatabaseManager
import com.example.local_rag_app.database.DocumentLoader
import com.example.local_rag_app.ml.EmbeddingModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppInitializer(private val context: Context) {
    
    private val TAG = "AppInitializer"
    private val PREFS_NAME = "app_prefs"
    private val KEY_DOCUMENTS_VECTORIZED = "documents_vectorized"
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    suspend fun initializeApp(
        onProgress: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            onProgress("Initializing database...")
            val databaseManager = DatabaseManager.getInstance(context)
            databaseManager.initialize()
            
            // Check if documents are already vectorized
            val isVectorized = prefs.getBoolean(KEY_DOCUMENTS_VECTORIZED, false)
            
            if (!isVectorized) {
                onProgress("Loading baggage policy documents...")
                val documentLoader = DocumentLoader(context)
                val chunks = documentLoader.loadAndProcessDocuments()
                
                if (chunks.isEmpty()) {
                    Log.e(TAG, "No documents loaded")
                    return@withContext false
                }
                
                onProgress("Initializing embedding model...")
                val embeddingModel = EmbeddingModel(context)
                embeddingModel.initialize()
                
                onProgress("Vectorizing documents (this may take a few minutes)...")
                var processed = 0
                
                for (chunk in chunks) {
                    processed++
                    if (processed % 5 == 0) {
                        onProgress("Vectorizing documents... ($processed/${chunks.size})")
                    }
                    
                    val embedding = embeddingModel.embed(chunk.text)
                    
                    val docId = "${chunk.airline}_chunk_${chunk.chunkIndex}"
                    databaseManager.storeDocument(
                        docId = docId,
                        text = chunk.text,
                        embedding = embedding,
                        airline = chunk.airline,
                        chunkIndex = chunk.chunkIndex
                    )
                }
                
                embeddingModel.close()
                
                // Mark as vectorized
                prefs.edit().putBoolean(KEY_DOCUMENTS_VECTORIZED, true).apply()
                
                onProgress("Vectorization complete! ${chunks.size} chunks processed.")
                Log.d(TAG, "Documents vectorized and stored successfully")
            } else {
                val count = databaseManager.getDocumentCount()
                onProgress("Database ready with $count documents")
                Log.d(TAG, "Documents already vectorized, skipping initialization")
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error during app initialization", e)
            onProgress("Error: ${e.message}")
            false
        }
    }
    
    fun resetVectorization() {
        prefs.edit().putBoolean(KEY_DOCUMENTS_VECTORIZED, false).apply()
        Log.d(TAG, "Vectorization flag reset")
    }
}
