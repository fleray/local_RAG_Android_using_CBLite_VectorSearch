package com.example.local_rag_app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.local_rag_app.data.ChatMessage
import com.example.local_rag_app.rag.RagEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val ragEngine = RagEngine(application.applicationContext)
    private var isInitialized = false
    
    init {
        // Add welcome message
        _messages.value = listOf(
            ChatMessage(
                text = "Hello! I'm your airline baggage policy assistant. I can help you with baggage information for Lufthansa, Ryanair, Emirates, and Southwest Airlines. What would you like to know?",
                isUser = false
            )
        )
    }
    
    fun initializeRagEngine() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                ragEngine.initialize()
                isInitialized = true
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Failed to initialize AI engine: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        // Add user message
        val userMessage = ChatMessage(text = text, isUser = true)
        _messages.value += userMessage
        
        // Add loading message
        val loadingMessage = ChatMessage(
            text = "Thinking...",
            isUser = false,
            isLoading = true
        )
        _messages.value += loadingMessage
        
        viewModelScope.launch {
            try {
                if (!isInitialized) {
                    ragEngine.initialize()
                    isInitialized = true
                }
                
                val response = ragEngine.query(text)
                
                // Remove loading message and add actual response
                _messages.value = _messages.value.dropLast(1) + ChatMessage(
                    text = response,
                    isUser = false
                )
            } catch (e: Exception) {
                // Remove loading message and add error message
                _messages.value = _messages.value.dropLast(1) + ChatMessage(
                    text = "I apologize, but I encountered an error: ${e.message}",
                    isUser = false
                )
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        ragEngine.close()
    }
}
