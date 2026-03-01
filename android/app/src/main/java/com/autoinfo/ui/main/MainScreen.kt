package com.autoinfo.ui.main

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autoinfo.domain.model.ConnectionState
import com.autoinfo.domain.model.ObdDevice
import com.autoinfo.domain.model.UploadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    
    // State
    val vehicleId by viewModel.vehicleId.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val pairedDevices by viewModel.pairedDevices.collectAsStateWithLifecycle()
    val selectedDevice by viewModel.selectedDevice.collectAsStateWithLifecycle()
    val isCollecting by viewModel.isCollecting.collectAsStateWithLifecycle()
    val currentTelemetry by viewModel.currentTelemetry.collectAsStateWithLifecycle()
    val uploadState by viewModel.uploadState.collectAsStateWithLifecycle()
    val pendingCount by viewModel.pendingUploadCount.collectAsStateWithLifecycle()
    
    // Permission launcher
    var hasBluetoothPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasBluetoothPermission = permissions.all { it.value }
        if (hasBluetoothPermission) {
            // Refresh paired devices
        }
    }
    
    LaunchedEffect(Unit) {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        
        if (!allGranted) {
            permissionLauncher.launch(permissions)
        } else {
            hasBluetoothPermission = true
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auto-Info") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Vehicle ID
            if (vehicleId.isEmpty()) {
                VehicleIdInput(
                    vehicleId = vehicleId,
                    onVehicleIdChange = { viewModel.setVehicleId(it) }
                )
            } else {
                VehicleInfoCard(
                    vehicleId = vehicleId,
                    onEdit = { viewModel.setVehicleId("") }
                )
            }
            
            // Connection Status
            ConnectionStatusCard(
                connectionState = connectionState,
                selectedDevice = selectedDevice
            )
            
            // Device Selection
            if (!isCollecting && pairedDevices.isNotEmpty() && hasBluetoothPermission) {
                DeviceSelector(
                    devices = pairedDevices,
                    selectedDevice = selectedDevice,
                    onDeviceSelected = { viewModel.selectDevice(it) },
                    onConnect = { viewModel.connectToDevice(it.address) }
                )
            }
            
            // Current Telemetry
            currentTelemetry?.let { telemetry ->
                TelemetryCard(telemetry = telemetry)
            }
            
            // Upload Status
            UploadStatusCard(
                uploadState = uploadState,
                pendingCount = pendingCount,
                onSyncClick = { viewModel.syncPendingData() }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Control Buttons
            ControlButtons(
                isCollecting = isCollecting,
                canStart = vehicleId.isNotEmpty() && selectedDevice != null,
                onStart = { viewModel.startCollection() },
                onStop = { viewModel.stopCollection() }
            )
        }
    }
}

@Composable
fun VehicleIdInput(
    vehicleId: String,
    onVehicleIdChange: (String) -> Unit
) {
    var text by remember { mutableStateOf(vehicleId) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Vehicle ID",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Enter Vehicle ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(
                onClick = { onVehicleIdChange(text) },
                modifier = Modifier.align(Alignment.End),
                enabled = text.isNotEmpty()
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
fun VehicleInfoCard(
    vehicleId: String,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Vehicle: $vehicleId",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
        }
    }
}

@Composable
fun ConnectionStatusCard(
    connectionState: ConnectionState,
    selectedDevice: ObdDevice?
) {
    val (color, text, icon) = when (connectionState) {
        is ConnectionState.Disconnected -> Triple(
            Color.Gray,
            "Disconnected",
            Icons.Default.BluetoothDisabled
        )
        is ConnectionState.Connecting -> Triple(
            Color(0xFFFFA500),
            "Connecting to ${connectionState.deviceName}...",
            Icons.Default.BluetoothSearching
        )
        is ConnectionState.Connected -> Triple(
            Color(0xFF4CAF50),
            "Connected to ${connectionState.deviceName}",
            Icons.Default.BluetoothConnected
        )
        is ConnectionState.Error -> Triple(
            Color(0xFFF44336),
            "Error: ${connectionState.message}",
            Icons.Default.Error
        )
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = color
            )
        }
    }
}

@Composable
fun DeviceSelector(
    devices: List<ObdDevice>,
    selectedDevice: ObdDevice?,
    onDeviceSelected: (ObdDevice) -> Unit,
    onConnect: (ObdDevice) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "OBD-II Adapters",
                style = MaterialTheme.typography.titleMedium
            )
            
            LazyColumn(
                modifier = Modifier.heightIn(max = 200.dp)
            ) {
                items(devices) { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = device.name,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                onDeviceSelected(device)
                                onConnect(device)
                            },
                            enabled = selectedDevice?.address != device.address
                        ) {
                            Text("Connect")
                        }
                    }
                }
            }
            
            if (devices.isEmpty()) {
                Text(
                    text = "No paired OBD adapters found. Pair your ELM327 in Bluetooth settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun TelemetryCard(telemetry: com.autoinfo.domain.model.TelemetryData) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Current Readings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TelemetryValue("RPM", telemetry.rpm?.toString() ?: "--", modifier = Modifier.weight(1f))
                TelemetryValue("Speed", "${telemetry.speed ?: "--"} km/h", modifier = Modifier.weight(1f))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TelemetryValue("Coolant", "${telemetry.coolantTemp ?: "--"}°C", modifier = Modifier.weight(1f))
                TelemetryValue("Throttle", "${telemetry.throttlePos ?: "--"}%", modifier = Modifier.weight(1f))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TelemetryValue("Fuel", "${telemetry.fuelLevel ?: "--"}%", modifier = Modifier.weight(1f))
                TelemetryValue("Load", "${telemetry.engineLoad ?: "--"}%", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun TelemetryValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun UploadStatusCard(
    uploadState: UploadState,
    pendingCount: Int,
    onSyncClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = when (uploadState) {
                        is UploadState.Idle -> "Ready to upload"
                        is UploadState.Uploading -> "Uploading..."
                        is UploadState.Success -> "Upload successful"
                        is UploadState.Error -> "Upload failed: ${uploadState.message}"
                        is UploadState.Offline -> "Offline (${uploadState.pendingCount} pending)"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                if (pendingCount > 0) {
                    Text(
                        text = "$pendingCount records pending",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            
            if (pendingCount > 0) {
                Button(onClick = onSyncClick) {
                    Text("Sync")
                }
            }
        }
    }
}

@Composable
fun ControlButtons(
    isCollecting: Boolean,
    canStart: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isCollecting) {
            Button(
                onClick = onStop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336)
                )
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stop Collection", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = canStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Collection", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
