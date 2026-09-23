package com.example.ui.screens

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FaultEntity
import com.example.engine.CorrelatedFaultTree
import com.example.ui.IspViewModel
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.*

@Composable
fun FaultsScreen(
    viewModel: IspViewModel
) {
    val faults by viewModel.faults.collectAsState()
    val correlatedTrees by viewModel.correlatedFaults.collectAsState()

    var selectedFaultForResolve by remember { mutableStateOf<FaultEntity?>(null) }
    var resolutionText by remember { mutableStateOf("") }
    var viewTab by remember { mutableStateOf(0) } // 0: Root Cause Trees, 1: All Alarms

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("faults_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab selector: Correlated Trees vs Raw Alarms
        item {
            TabRow(
                selectedTabIndex = viewTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
            ) {
                Tab(
                    selected = viewTab == 0,
                    onClick = { viewTab = 0 },
                    text = { Text("Root-Cause Analysis (${correlatedTrees.size})", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = viewTab == 1,
                    onClick = { viewTab = 1 },
                    text = { Text("Alarm Log (${faults.size})", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }

        if (viewTab == 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Hub, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Digital Twin Correlation Engine Active",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Downstream customer alarms collapsed to single physical root failure.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(correlatedTrees) { tree ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("correlated_fault_${tree.rootFaultId}"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                        .background(StatusRed.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Dangerous, contentDescription = null, tint = StatusRed, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = tree.rootCauseType,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusRed
                                    )
                                    Text(
                                        text = "Root Asset: ${tree.rootAssetId}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            StatusPill(status = "ROOT CAUSE")
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = tree.rootFaultTitle,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "IMPACT CASCADE:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "• Affected POP: ${tree.affectedPop}", fontSize = 12.sp)
                                Text(text = "• Severed Links: ${tree.affectedDevices.joinToString(", ")}", fontSize = 12.sp)
                                Text(
                                    text = "• Customers Down: ${tree.affectedCustomersCount} Subscribers (${tree.affectedServicesCount} Services)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusRed
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Sample Endpoints: ${tree.affectedCustomers.take(3).joinToString("; ")}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Recommended Remediation:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = tree.recommendedAction,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            val originalFault = faults.find { it.faultId == tree.rootFaultId }
                            if (originalFault?.status != "RESOLVED") {
                                Button(
                                    onClick = {
                                        selectedFaultForResolve = originalFault
                                        resolutionText = "Spliced severed buffer tubes and certified -19 dBm optical RX."
                                    },
                                    modifier = Modifier.testTag("btn_resolve_${tree.rootFaultId}")
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Mark Resolved", fontSize = 12.sp)
                                }
                            } else {
                                StatusPill(status = "RESOLVED")
                            }
                        }
                    }
                }
            }
        } else {
            // Raw Alarm List
            items(faults) { fault ->
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = fault.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${fault.faultId} • Type: ${fault.faultType} • ${fault.startTime}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusPill(status = fault.severity)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = fault.description, style = MaterialTheme.typography.bodySmall)

                        if (fault.resolutionNotes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Resolution: ${fault.resolutionNotes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    // Resolve Fault Dialog
    if (selectedFaultForResolve != null) {
        val f = selectedFaultForResolve!!
        AlertDialog(
            onDismissRequest = { selectedFaultForResolve = null },
            title = { Text("Resolve Alarm ${f.faultId}") },
            text = {
                Column {
                    Text(text = f.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = resolutionText,
                        onValueChange = { resolutionText = it },
                        label = { Text("Resolution Work Notes") },
                        modifier = Modifier.fillMaxWidth().testTag("input_resolution_notes")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resolveFault(f.faultId, resolutionText)
                        selectedFaultForResolve = null
                    },
                    modifier = Modifier.testTag("btn_confirm_resolve")
                ) {
                    Text("Confirm Resolution")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedFaultForResolve = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
