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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WorkOrderEntity
import com.example.ui.IspViewModel
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.*

@Composable
fun FieldOpsScreen(
    viewModel: IspViewModel
) {
    val workOrders by viewModel.workOrders.collectAsState()
    val technicians by viewModel.technicians.collectAsState()
    val isFieldOnline by viewModel.isFieldOnline.collectAsState()
    val syncStatus by viewModel.fieldSyncStatus.collectAsState()

    var showCreateWoDialog by remember { mutableStateOf(false) }
    var selectedWoForUpdate by remember { mutableStateOf<WorkOrderEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("field_ops_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Field Connectivity & Sync Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFieldOnline) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else StatusAmber.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isFieldOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (isFieldOnline) BrandBlue else StatusAmber,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isFieldOnline) "Field Online Mode" else "Field Offline Cache Mode",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Sync Status: $syncStatus • Room Local SQLite",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { viewModel.toggleFieldOnlineMode() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFieldOnline) BrandBlue else StatusAmber
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_toggle_offline")
                    ) {
                        Text(if (isFieldOnline) "Go Offline" else "Sync Online", fontSize = 12.sp)
                    }
                }
            }
        }

        // Section: Active Technicians
        item {
            SectionHeader(
                title = "Field Technicians (${technicians.size})",
                subtitle = "Splicing teams & last-mile line engineers"
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(technicians) { tech ->
                    Card(
                        modifier = Modifier
                            .width(240.dp)
                            .testTag("tech_card_${tech.technicianId}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = tech.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                StatusPill(status = tech.currentStatus)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = tech.team, style = MaterialTheme.typography.bodySmall, color = BrandBlue)
                            Text(text = tech.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "GPS: (${String.format("%.4f", tech.currentLat)}, ${String.format("%.4f", tech.currentLng)})",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Section: Work Orders
        item {
            SectionHeader(
                title = "Dispatch Work Orders (${workOrders.size})",
                subtitle = "Fault remediation and new subscriber installations",
                actionText = "New Ticket",
                onActionClick = { showCreateWoDialog = true }
            )
        }

        items(workOrders) { wo ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("work_order_${wo.workOrderId}"),
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
                            Text(
                                text = wo.workOrderId,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (wo.offlinePendingSync) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "OFFLINE QUEUED",
                                    fontSize = 9.sp,
                                    color = StatusAmber,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(StatusAmber.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        StatusPill(status = wo.status)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = wo.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Assigned To: ${wo.technicianName} • Priority: ${wo.priority}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Location: ${wo.siteAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (wo.partsUsed.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Parts / Enclosures: ${wo.partsUsed}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (wo.resolutionNotes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Notes: ${wo.resolutionNotes}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StatusGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { selectedWoForUpdate = wo },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_update_wo_${wo.workOrderId}")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Update Status", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Create Work Order Dialog
    if (showCreateWoDialog) {
        var title by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var priority by remember { mutableStateOf("CRITICAL") }
        var selectedTechId by remember { mutableStateOf(technicians.firstOrNull()?.technicianId ?: "TECH-01") }

        AlertDialog(
            onDismissRequest = { showCreateWoDialog = false },
            title = { Text("Create Work Order") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task Summary") },
                        modifier = Modifier.fillMaxWidth().testTag("input_wo_title")
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Site / Pole Address") },
                        modifier = Modifier.fillMaxWidth().testTag("input_wo_address")
                    )

                    Text("Priority:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("CRITICAL", "HIGH", "MEDIUM", "LOW").forEach { pr ->
                            FilterChip(
                                selected = priority == pr,
                                onClick = { priority = pr },
                                label = { Text(pr, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.createWorkOrder(title, selectedTechId, priority, address)
                            showCreateWoDialog = false
                        }
                    },
                    modifier = Modifier.testTag("btn_submit_wo")
                ) {
                    Text("Dispatch")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateWoDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Update Work Order Dialog
    if (selectedWoForUpdate != null) {
        val wo = selectedWoForUpdate!!
        var newStatus by remember { mutableStateOf(wo.status) }
        var notes by remember { mutableStateOf(wo.resolutionNotes) }

        AlertDialog(
            onDismissRequest = { selectedWoForUpdate = null },
            title = { Text("Update ${wo.workOrderId}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = wo.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)

                    Text("Status:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ASSIGNED", "WORKING", "COMPLETED", "RESOLVED").forEach { st ->
                            FilterChip(
                                selected = newStatus == st,
                                onClick = { newStatus = st },
                                label = { Text(st, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Resolution Notes & Parts Used") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateWorkOrderStatus(wo.workOrderId, newStatus, notes)
                        selectedWoForUpdate = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedWoForUpdate = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
