package com.example.local_rag_app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun InitializationScreen(
    progressMessage: String,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!isError) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 4.dp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            Text(
                text = if (isError) "Initialization Error" else "Initializing App",
                style = MaterialTheme.typography.headlineMedium,
                color = if (isError) 
                    MaterialTheme.colorScheme.error 
                else 
                    MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = progressMessage,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            
            if (!isError && progressMessage.contains("Vectorizing")) {
                Spacer(modifier = Modifier.height(24.dp))
                
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(8.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "This is a one-time process.\nSubsequent launches will be instant.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    }
}
