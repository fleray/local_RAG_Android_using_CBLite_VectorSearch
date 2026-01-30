package com.example.local_rag_app.database

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class DocumentLoader(private val context: Context) {
    
    private val TAG = "DocumentLoader"
    private val CHUNK_SIZE = 512 // characters per chunk
    
    suspend fun loadAndProcessDocuments(): List<DocumentChunk> = withContext(Dispatchers.IO) {
        val chunks = mutableListOf<DocumentChunk>()
        
        try {
            val assetManager = context.assets
            val policyFiles = assetManager.list("baggage_policies") ?: emptyArray()
            
            Log.d(TAG, "Found ${policyFiles.size} policy files")
            
            for (fileName in policyFiles) {
                if (fileName.endsWith(".txt")) {
                    val airline = fileName.removeSuffix(".txt")
                    val content = readAssetFile("baggage_policies/$fileName")
                    val documentChunks = chunkDocument(content, airline)
                    chunks.addAll(documentChunks)
                    Log.d(TAG, "Loaded $airline: ${documentChunks.size} chunks")
                }
            }
            
            Log.d(TAG, "Total chunks loaded: ${chunks.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading documents", e)
        }
        
        chunks
    }
    
    private fun readAssetFile(path: String): String {
        return try {
            val inputStream = context.assets.open(path)
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading file: $path", e)
            ""
        }
    }
    
    private fun chunkDocument(content: String, airline: String): List<DocumentChunk> {
        val chunks = mutableListOf<DocumentChunk>()
        
        // Split by paragraphs first
        val paragraphs = content.split("\n\n").filter { it.isNotBlank() }
        
        var currentChunk = StringBuilder()
        var chunkIndex = 0
        
        for (paragraph in paragraphs) {
            val trimmedParagraph = paragraph.trim()
            
            // If adding this paragraph would exceed chunk size and we have content, save current chunk
            if (currentChunk.length + trimmedParagraph.length > CHUNK_SIZE && currentChunk.isNotEmpty()) {
                chunks.add(
                    DocumentChunk(
                        text = currentChunk.toString().trim(),
                        airline = airline,
                        chunkIndex = chunkIndex++
                    )
                )
                currentChunk = StringBuilder()
            }
            
            // Add paragraph to current chunk
            if (currentChunk.isNotEmpty()) {
                currentChunk.append("\n\n")
            }
            currentChunk.append(trimmedParagraph)
            
            // If this paragraph alone is larger than chunk size, save it
            if (currentChunk.length >= CHUNK_SIZE) {
                chunks.add(
                    DocumentChunk(
                        text = currentChunk.toString().trim(),
                        airline = airline,
                        chunkIndex = chunkIndex++
                    )
                )
                currentChunk = StringBuilder()
            }
        }
        
        // Add remaining content
        if (currentChunk.isNotEmpty()) {
            chunks.add(
                DocumentChunk(
                    text = currentChunk.toString().trim(),
                    airline = airline,
                    chunkIndex = chunkIndex
                )
            )
        }
        
        return chunks
    }
}

data class DocumentChunk(
    val text: String,
    val airline: String,
    val chunkIndex: Int
)
