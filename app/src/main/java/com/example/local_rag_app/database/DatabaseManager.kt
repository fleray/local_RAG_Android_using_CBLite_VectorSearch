package com.example.local_rag_app.database

import android.content.Context
import android.util.Log
import com.couchbase.lite.*
import com.couchbase.lite.Collection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DatabaseManager private constructor(private val context: Context) {
    
    private var database: Database? = null
    private var collection: Collection? = null
    private val TAG = "DatabaseManager"
    
    companion object {
        @Volatile
        private var INSTANCE: DatabaseManager? = null
        
        fun getInstance(context: Context): DatabaseManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabaseManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            CouchbaseLite.init(context)
            
            // Enable vector search extension
            try {
                CouchbaseLite.enableVectorSearch()
                Log.d(TAG, "Vector search enabled")
            } catch (e: Exception) {
                Log.e(TAG, "Error enabling vector search", e)
                throw IllegalStateException("Could not enable vector search", e)
            }
            
            val config = DatabaseConfiguration()
            database = Database("baggage_policies", config)
            
            collection = database?.defaultCollection

            createVectorIndex()
            
            Log.d(TAG, "Database initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing database", e)
            throw e
        }
    }
    
    private fun createVectorIndex() {
        try {
            // VectorIndexConfiguration(expression, dimensions, centroids)
            val indexConfig = VectorIndexConfiguration("embedding", 100, 20)
            
            collection?.createIndex("vector_index", indexConfig)
            Log.d(TAG, "Vector search index created successfully")
        } catch (e: Exception) {
            if (e.message?.contains("already exists") == true) {
                Log.d(TAG, "Vector index already exists")
            } else {
                Log.e(TAG, "Error creating vector index", e)
                throw e
            }
        }
    }
    
    suspend fun storeDocument(
        docId: String,
        text: String,
        embedding: FloatArray,
        airline: String,
        chunkIndex: Int
    ) = withContext(Dispatchers.IO) {
        try {
            val mutableDoc = MutableDocument(docId)
            mutableDoc.setString("text", text)
            mutableDoc.setArray("embedding", MutableArray(embedding.map { it }))
            mutableDoc.setString("airline", airline)
            mutableDoc.setInt("chunkIndex", chunkIndex)
            mutableDoc.setLong("timestamp", System.currentTimeMillis())
            
            collection?.save(mutableDoc)
            Log.d(TAG, "Document stored: $docId")
        } catch (e: Exception) {
            Log.e(TAG, "Error storing document", e)
            throw e
        }
    }
    
    suspend fun searchSimilarDocuments(
        queryEmbedding: FloatArray,
        limit: Int = 3
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val query = database?.createQuery(
                "SELECT text, airline, chunkIndex, " +
                "APPROX_VECTOR_DISTANCE(embedding, \$queryVector) AS distance " +
                "FROM _default._default " +
                "ORDER BY APPROX_VECTOR_DISTANCE(embedding, \$queryVector) " +
                "LIMIT $limit"
            )
            
            val params = Parameters()
            params.setArray("queryVector", MutableArray(queryEmbedding.map { it }))
            query?.parameters = params
            
            val results = mutableListOf<SearchResult>()
            query?.execute()?.forEach { result ->
                results.add(
                    SearchResult(
                        text = result.getString("text") ?: "",
                        airline = result.getString("airline") ?: "",
                        chunkIndex = result.getInt("chunkIndex"),
                        distance = result.getFloat("distance")
                    )
                )
            }
            
            Log.d(TAG, "Found ${results.size} similar documents")
            results
        } catch (e: Exception) {
            Log.e(TAG, "Error searching documents", e)
            emptyList()
        }
    }
    
    suspend fun getDocumentCount(): Long = withContext(Dispatchers.IO) {
        try {
            collection?.count ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error getting document count", e)
            0L
        }
    }
    
    suspend fun clearAllDocuments() = withContext(Dispatchers.IO) {
        try {
            val query = database?.createQuery("SELECT META().id FROM _default._default")
            query?.execute()?.forEach { result ->
                val docId = result.getString("id")
                docId?.let { collection?.getDocument(it)?.let { doc -> collection?.delete(doc) } }
            }
            Log.d(TAG, "All documents cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing documents", e)
        }
    }
    
    fun close() {
        try {
            database?.close()
            Log.d(TAG, "Database closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing database", e)
        }
    }
}

data class SearchResult(
    val text: String,
    val airline: String,
    val chunkIndex: Int,
    val distance: Float
)
