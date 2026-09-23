package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.model.FiberCableEntity
import com.example.data.model.FiberCoreEntity
import com.example.ui.IspViewModel
import com.example.ui.components.CapacityProgressBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.ServiceTypeBadge
import com.example.ui.components.StatusPill
import com.example.ui.theme.*

@Composable
fun FiberManagementScreen(
    viewModel: IspViewModel
) {
    val cables by viewModel.cables.collectAsState()
    val cores by viewModel.cores.collectAsState()
    val fdbs by viewModel.fdbs.collectAsState()
    val splitters by viewModel.splitters.collectAsState()

    var selectedCableId by remember { mutableStateOf<String>("FBR-BB-01") }
    var selectedCoreForEdit by remember { mutableStateOf<FiberCoreEntity?>(null) }
    var tabIndex by remember { mutableStateOf(0) } // 0: Cables & Cores, 1: FDB & Splitters

    val currentCable = cables.find { it.cableId == selectedCableId } ?: cables.firstOrNull()
    val cableCores = cores.filter { it.cableId == currentCable?.cableId }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("fiber_management_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab Selector: Cables vs FDBs & Splitters
        item {
            TabRow(
                selectedTabIndex = tabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
            ) {
                Tab(
                    selected = tabIndex == 0,
                    onClick = { tabIndex = 0 },
                    text = { Text("Fiber Cables & Core Matrix", fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.Cable, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = tabIndex == 1,
                    onClick = { tabIndex = 1 },
                    text = { Text("FDB / FAT & Splitters", fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.MeetingRoom, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }

        if (tabIndex == 0) {
            // Cable Selector
            item {
                Column {
                    Text(
                        text = "Select Fiber Cable Asset:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(cables) { cable ->
                            val isSelected = cable.cableId == selectedCableId
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCableId = cable.cableId },
                                label = { Text("${cable.code} (${cable.coreCount}C)", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Cable, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier.testTag("cable_chip_${cable.cableId}")
                            )
                        }
                    }
                }
            }

            // Selected Cable Detail Header
            if (currentCable != null) {
                item {
                    val activeCount = cableCores.count { it.status == "ACTIVE" }
                    val spareCount = cableCores.count { it.status == "SPARE" }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = currentCable.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${currentCable.cableType} | Route: ${currentCable.routeName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusPill(status = currentCable.status)
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            CapacityProgressBar(
                                used = activeCount,
                                total = currentCable.coreCount,
                                label = "Core Capacity ($activeCount Active, $spareCount Spare, ${currentCable.coreCount - activeCount - spareCount} Reserved)"
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Length: ${currentCable.lengthMeters.toInt()}m | Span: ${currentCable.startSiteId} → ${currentCable.endSiteId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Core Grid Header
                item {
                    SectionHeader(
                        title = "TIA-598 Color-Coded Cores (${cableCores.size})",
                        subtitle = "Tap any core to reassign status or inspect connected service"
                    )
                }

                // Visual Core Grid (showing each individual core block)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        cableCores.chunked(4).forEach { rowCores ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowCores.forEach { core ->
                                    FiberCoreCard(
                                        core = core,
                                        modifier = Modifier.weight(1f),
                                        onClick = { selectedCoreForEdit = core }
                                    )
                                }
                                // Fill empty slots in row if < 4
                                repeat(4 - rowCores.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // FDB & Splitters View
            item {
                SectionHeader(
                    title = "Fiber Distribution Boxes (FAT)",
                    subtitle = "Outdoor IP68 terminal boxes and capacity"
                )
            }

            items(fdbs) { fdb ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BrandBlue.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = fdb.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(text = "Code: ${fdb.code} | Site: ${fdb.locationSiteId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            StatusPill(status = fdb.status)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        CapacityProgressBar(
                            used = fdb.usedPorts,
                            total = fdb.capacityPorts,
                            label = "FDB Outlets (${fdb.usedPorts} In-Use, ${fdb.sparePorts} Spare Ports)"
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Feeder Input: ${fdb.inputFiberCableId} (Core #${fdb.inputFiberCoreNumber}) | Splitter: ${fdb.splitterId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                SectionHeader(
                    title = "PLC Optical Splitters & CATV Taps",
                    subtitle = "Splitter ratios (1:2 to 1:64) and calculated loss"
                )
            }

            items(splitters) { splitter ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(StatusPurple.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CallSplit, contentDescription = null, tint = StatusPurple, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = splitter.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "Ratio: ${splitter.ratio} | Insertion Loss: ${splitter.calculatedLossDb} dB",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        StatusPill(status = splitter.status)
                    }
                }
            }
        }
    }

    // Core Edit / Reassign Dialog
    if (selectedCoreForEdit != null) {
        val core = selectedCoreForEdit!!
        var newStatus by remember { mutableStateOf(core.status) }
        var newService by remember { mutableStateOf(core.serviceType) }

        AlertDialog(
            onDismissRequest = { selectedCoreForEdit = null },
            title = {
                Text("Core #${core.coreNumber} (${core.colorName}) Configuration")
            },
            text = {
                Column {
                    Text(
                        text = "Cable: ${core.cableId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Status:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        listOf("ACTIVE", "SPARE", "RESERVED", "FAULT").forEach { st ->
                            FilterChip(
                                selected = newStatus == st,
                                onClick = { newStatus = st },
                                label = { Text(st, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Assigned Service:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        listOf("INTERNET", "CATV", "CCTV", "SPARE").forEach { srv ->
                            FilterChip(
                                selected = newService == srv,
                                onClick = { newService = srv },
                                label = { Text(srv, fontSize = 11.sp) }
                            )
                        }
                    }

                    if (core.connectedDeviceId.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Connected Equipment: ${core.connectedDeviceId} (${core.connectedPort})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateFiberCoreStatus(core.cableId, core.coreNumber, newStatus, newService)
                        selectedCoreForEdit = null
                    },
                    modifier = Modifier.testTag("save_core_button")
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCoreForEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FiberCoreCard(
    core: FiberCoreEntity,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val coreColor = try {
        Color(android.graphics.Color.parseColor(core.colorHex))
    } catch (e: Exception) {
        BrandBlue
    }

    Card(
        modifier = modifier
            .testTag("core_card_${core.coreNumber}")
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TIA-598 Color bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(coreColor)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(coreColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "C-%02d".format(core.coreNumber),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = core.serviceType.take(4),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = when (core.serviceType) {
                    "INTERNET" -> BrandBlue
                    "CATV" -> StatusPurple
                    "CCTV" -> BrandCyan
                    else -> StatusGray
                }
            )
            Text(
                text = core.status.take(3),
                fontSize = 9.sp,
                color = when (core.status) {
                    "ACTIVE" -> StatusGreen
                    "SPARE" -> BrandBlue
                    "FAULT" -> StatusRed
                    else -> StatusAmber
                }
            )
        }
    }
}
