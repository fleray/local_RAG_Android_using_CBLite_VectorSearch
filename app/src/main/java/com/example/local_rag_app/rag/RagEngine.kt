package com.example.local_rag_app.rag

import android.content.Context
import android.util.Log
import com.example.local_rag_app.database.DatabaseManager
import com.example.local_rag_app.ml.EmbeddingModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RagEngine(private val context: Context) {
    
    private val TAG = "RagEngine"
    private lateinit var databaseManager: DatabaseManager
    private lateinit var embeddingModel: EmbeddingModel
    private lateinit var llmInference: LlmInference
    
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            databaseManager = DatabaseManager.getInstance(context)
            
            embeddingModel = EmbeddingModel(context)
            embeddingModel.initialize()
            
            llmInference = LlmInference(context)
            llmInference.initialize()
            
            Log.d(TAG, "RAG Engine initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing RAG Engine", e)
            throw e
        }
    }
    
    suspend fun query(userQuestion: String): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Processing query: $userQuestion")
            
            // Step 1: Generate embedding for the user's question
            val queryEmbedding = embeddingModel.embed(userQuestion)
            Log.d(TAG, "Generated query embedding")
            
            // Step 2: Search for similar documents in the vector database
            val similarDocs = databaseManager.searchSimilarDocuments(queryEmbedding, limit = 3)
            Log.d(TAG, "Found ${similarDocs.size} similar documents")
            
            if (similarDocs.isEmpty()) {
                return@withContext "I don't have enough information about that in my baggage policy database. Please ask about airline baggage policies for Lufthansa, Ryanair, Emirates, or Southwest."
            }
            
            // Step 3: Build context from retrieved documents
            val context = similarDocs.joinToString("\n\n") { result ->
                "From ${result.airline} baggage policy:\n${result.text}"
            }
            
            Log.d(TAG, "Built context from ${similarDocs.size} documents")
            
            // Step 4: Generate response using LLM with augmented context - retry if empty
            var response = ""
            var attempts = 0
            val maxAttempts = 4
            
            while (response.isEmpty() && attempts < maxAttempts) {
                attempts++
                Log.d(TAG, "Generating response, attempt $attempts")
                response = llmInference.generateResponseWithContext(userQuestion, context)
            }
            
            if (response.isEmpty()) {
                response = "I apologize, but I'm unable to generate a response at the moment."
            } else if (attempts > 1) {
                response = "Sorry for the delay (attempts: $attempts). $response"
            }
            
            Log.d(TAG, "Generated response")
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error processing query", e)
            "I apologize, but I encountered an error while processing your question. Please try again."
        }
    }
    
    fun close() {
        try {
            embeddingModel.close()
            llmInference.close()
            Log.d(TAG, "RAG Engine closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing RAG Engine", e)
        }
    }
}
