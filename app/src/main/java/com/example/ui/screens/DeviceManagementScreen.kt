package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceEntity
import com.example.engine.DeviceTelemetry
import com.example.engine.MonitoringEngine
import com.example.ui.IspViewModel
import com.example.ui.components.QrAssetDialog
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DeviceManagementScreen(
    viewModel: IspViewModel
) {
    val devices by viewModel.devices.collectAsState()
    val scope = rememberCoroutineScope()

    var filterType by remember { mutableStateOf("ALL") }
    var selectedDeviceForTelemetry by remember { mutableStateOf<DeviceEntity?>(null) }
    var activeTelemetry by remember { mutableStateOf<DeviceTelemetry?>(null) }
    var isPollingTelemetry by remember { mutableStateOf(false) }
    var selectedDeviceForQr by remember { mutableStateOf<DeviceEntity?>(null) }

    val filteredDevices = if (filterType == "ALL") {
        devices
    } else {
        devices.filter {
            when (filterType) {
                "ROUTER" -> it.deviceType.contains("ROUTER")
                "SWITCH" -> it.deviceType.contains("SWITCH")
                "OLT" -> it.deviceType == "OLT"
                "CATV" -> it.deviceType.contains("CATV") || it.deviceType.contains("SATELLITE") || it.deviceType.contains("EDFA") || it.deviceType.contains("TRANSMITTER") || it.deviceType.contains("RECEIVER")
                else -> true
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("device_management_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    "ALL" to "All Equipment",
                    "ROUTER" to "Core Routers",
                    "SWITCH" to "Switches",
                    "OLT" to "GPON OLTs",
                    "CATV" to "CATV Headend"
                ).forEach { (key, label) ->
                    item {
                        FilterChip(
                            selected = filterType == key,
                            onClick = { filterType = key },
                            label = { Text(label, fontSize = 12.sp) },
                            modifier = Modifier.testTag("filter_$key")
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Hardware Inventory (${filteredDevices.size})",
                subtitle = "Active routing, switching, GPON and CATV distribution equipment"
            )
        }

        items(filteredDevices) { device ->
            val (icon, color) = when {
                device.deviceType.contains("ROUTER") -> Pair(Icons.Default.Lan, StatusPurple)
                device.deviceType.contains("SWITCH") -> Pair(Icons.Default.DeviceHub, BrandBlue)
                device.deviceType == "OLT" -> Pair(Icons.Default.Dns, StatusGreen)
                device.deviceType.contains("SATELLITE") || device.deviceType.contains("RECEIVER") -> Pair(Icons.Default.SatelliteAlt, BrandCyan)
                device.deviceType.contains("EDFA") -> Pair(Icons.Default.Bolt, StatusAmber)
                else -> Pair(Icons.Default.Sensors, BrandBlue)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("device_card_${device.deviceId}"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = device.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${device.vendor} ${device.model} (${device.deviceType})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        StatusPill(status = device.status)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Management IP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = device.managementIp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Column {
                            Text(text = "Location / Rack", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${device.siteId} | ${device.rack}", fontSize = 12.sp)
                        }
                        Column {
                            Text(text = "Serial No.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = device.serialNumber.take(10), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                    }

                    if (device.notes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = device.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { selectedDeviceForQr = device },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.QrCode2, contentDescription = "QR Code", tint = BrandBlue, modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Button(
                            onClick = {
                                selectedDeviceForTelemetry = device
                                isPollingTelemetry = true
                                scope.launch {
                                    activeTelemetry = MonitoringEngine.getTelemetryForDevice(device.deviceType, device.deviceId, device.managementIp)
                                    isPollingTelemetry = false
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_poll_${device.deviceId}")
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Live Telemetry", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Telemetry Polling Dialog
    if (selectedDeviceForTelemetry != null) {
        val dev = selectedDeviceForTelemetry!!
        AlertDialog(
            onDismissRequest = {
                selectedDeviceForTelemetry = null
                activeTelemetry = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Sensors, contentDescription = null, tint = BrandBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Telemetry: ${dev.name}")
                }
            },
            text = {
                if (isPollingTelemetry) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandBlue)
                    }
                } else if (activeTelemetry != null) {
                    val tel = activeTelemetry!!
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Adapter: ${tel.adapterType} (${if (tel.isMock) "Safe Sandbox Simulation" else "Live"})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ICMP Ping Latency:")
                            Text("${tel.pingLatencyMs} ms", fontWeight = FontWeight.Bold, color = StatusGreen)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Packet Loss:")
                            Text("${tel.packetLossPercent}%", fontWeight = FontWeight.Bold, color = StatusGreen)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("CPU Utilization:")
                            Text("${tel.cpuUsagePercent}%", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Memory Usage:")
                            Text("${tel.memoryUsagePercent}%", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Temperature:")
                            Text("${tel.temperatureCelsius}°C", fontWeight = FontWeight.Bold)
                        }
                        if (tel.opticalRxPowerDbm != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("GPON Optical RX:")
                                Text("${tel.opticalRxPowerDbm} dBm", fontWeight = FontWeight.Bold, color = BrandBlue)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    selectedDeviceForTelemetry = null
                    activeTelemetry = null
                }) {
                    Text("Close")
                }
            }
        )
    }

    // QR Asset Dialog
    if (selectedDeviceForQr != null) {
        val dev = selectedDeviceForQr!!
        QrAssetDialog(
            assetTitle = dev.name,
            assetCode = dev.qrCode.ifEmpty { "D1-QR-${dev.deviceId}" },
            details = "Asset ID: ${dev.assetId} | Serial: ${dev.serialNumber}\nSite: ${dev.siteId} | Rack: ${dev.rack}",
            onDismiss = { selectedDeviceForQr = null }
        )
    }
}
