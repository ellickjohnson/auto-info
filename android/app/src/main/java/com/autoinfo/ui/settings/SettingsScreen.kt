package com.autoinfo.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autoinfo.ui.main.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val apiUrl by viewModel.apiUrl.collectAsStateWithLifecycle()
    val pollingInterval by viewModel.pollingInterval.collectAsStateWithLifecycle()
    
    var apiUrlText by remember(apiUrl) { mutableStateOf(apiUrl) }
    var pollingIntervalText by remember(pollingInterval) { mutableStateOf(pollingInterval.toString()) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // API URL
            OutlinedTextField(
                value = apiUrlText,
                onValueChange = { apiUrlText = it },
                label = { Text("API URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Polling Interval
            OutlinedTextField(
                value = pollingIntervalText,
                onValueChange = { pollingIntervalText = it },
                label = { Text("Polling Interval (ms)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            
            // Polling interval presets
            Text(
                text = "Presets:",
                style = MaterialTheme.typography.labelMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = pollingInterval == 500L,
                    onClick = { pollingIntervalText = "500" },
                    label = { Text("500ms") }
                )
                FilterChip(
                    selected = pollingInterval == 1000L,
                    onClick = { pollingIntervalText = "1000" },
                    label = { Text("1s") }
                )
                FilterChip(
                    selected = pollingInterval == 2000L,
                    onClick = { pollingIntervalText = "2000" },
                    label = { Text("2s") }
                )
                FilterChip(
                    selected = pollingInterval == 5000L,
                    onClick = { pollingIntervalText = "5000" },
                    label = { Text("5s") }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Save Button
            Button(
                onClick = {
                    viewModel.setApiUrl(apiUrlText)
                    pollingIntervalText.toLongOrNull()?.let {
                        viewModel.setPollingInterval(it)
                    }
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }
            
            // About section
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium
            )
            
            Text(
                text = "Auto-Info Android App\nVersion 1.0.0\n\nConnects to ELM327 Bluetooth OBD-II adapter to collect vehicle telemetry and upload to your Auto-Info server.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
