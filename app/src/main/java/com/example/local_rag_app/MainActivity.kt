package com.example.local_rag_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.local_rag_app.init.AppInitializer
import com.example.local_rag_app.ui.ChatScreen
import com.example.local_rag_app.ui.InitializationScreen
import com.example.local_rag_app.ui.theme.MyApplicationTheme
import com.example.local_rag_app.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BaggagePolicyApp()
                }
            }
        }
    }
}

@Composable
fun BaggagePolicyApp() {
    var isInitialized by remember { mutableStateOf(false) }
    var progressMessage by remember { mutableStateOf("Starting initialization...") }
    var isError by remember { mutableStateOf(false) }
    
    val chatViewModel: ChatViewModel = viewModel()
    val messages by chatViewModel.messages.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val appInitializer = AppInitializer(context)
            
            val success = appInitializer.initializeApp { message ->
                progressMessage = message
            }
            
            if (success) {
                progressMessage = "Initializing AI engine..."
                chatViewModel.initializeRagEngine()
                isInitialized = true
            } else {
                isError = true
                progressMessage = "Failed to initialize the app. Please restart."
            }
        }
    }
    
    if (isInitialized) {
        ChatScreen(
            messages = messages,
            isLoading = isLoading,
            onSendMessage = { message ->
                chatViewModel.sendMessage(message)
            }
        )
    } else {
        InitializationScreen(
            progressMessage = progressMessage,
            isError = isError
        )
    }
}