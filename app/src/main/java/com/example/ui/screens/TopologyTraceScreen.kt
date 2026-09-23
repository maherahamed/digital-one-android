package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.TraceNode
import com.example.ui.IspViewModel
import com.example.ui.components.SectionHeader
import com.example.ui.components.ServiceTypeBadge
import com.example.ui.components.StatusPill
import com.example.ui.theme.*

@Composable
fun TopologyTraceScreen(
    viewModel: IspViewModel
) {
    val customers by viewModel.customers.collectAsState()
    val services by viewModel.customerServices.collectAsState()
    val selectedCustId by viewModel.selectedCustomerId.collectAsState()
    val selectedServiceType by viewModel.selectedServiceType.collectAsState()
    val pathResult by viewModel.currentPathTrace.collectAsState()

    var selectedNodeForDetail by remember { mutableStateOf<TraceNode?>(null) }

    val currentCustomer = customers.find { it.customerId == selectedCustId } ?: customers.firstOrNull()
    val customerAvailableServices = services.filter { it.customerId == currentCustomer?.customerId }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("topology_trace_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Customer Selector Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Select Subscriber to Trace Physical & Logical Route",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizontal Customer Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(customers) { cust ->
                            val isSelected = cust.customerId == selectedCustId
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    viewModel.selectCustomer(cust.customerId, selectedServiceType)
                                },
                                label = { Text(cust.name, fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier.testTag("chip_${cust.customerId}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Service Type Selector for this Customer (Internet vs CATV vs CCTV)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Customer Service:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            customerAvailableServices.forEach { svc ->
                                val isSelected = svc.serviceType == selectedServiceType
                                Button(
                                    onClick = {
                                        viewModel.selectCustomer(selectedCustId, svc.serviceType)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) {
                                            if (svc.serviceType == "INTERNET") BrandBlue else StatusPurple
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("btn_service_${svc.serviceType}")
                                ) {
                                    Text(svc.serviceType, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Trace Summary Card
        if (pathResult != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${pathResult!!.serviceType} Digital Twin Path",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = pathResult!!.planName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusPill(status = pathResult!!.overallStatus)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Estimated RX Power",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${pathResult!!.expectedRxDbm} dBm",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = BrandBlue
                                )
                            }
                            Column {
                                Text(
                                    text = "Measured RX Power",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${pathResult!!.measuredRxDbm} dBm",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (pathResult!!.measuredRxDbm < -24.0) StatusAmber else StatusGreen
                                )
                            }
                            Column {
                                Text(
                                    text = "Total Path Loss",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${String.format("%.1f", pathResult!!.totalCalculatedLossDb)} dB",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Path Diagram Nodes
            item {
                SectionHeader(
                    title = "End-to-End Route Breakdown",
                    subtitle = "Physical fiber, port mappings & devices from Customer to Core / Uplink"
                )
            }

            items(pathResult!!.nodes) { node ->
                TraceNodeRow(
                    node = node,
                    isSelected = selectedNodeForDetail?.stepOrder == node.stepOrder,
                    onClick = { selectedNodeForDetail = node }
                )
            }
        }
    }

    // Node Detail Modal / BottomSheet
    if (selectedNodeForDetail != null) {
        AlertDialog(
            onDismissRequest = { selectedNodeForDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = BrandBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Step #${selectedNodeForDetail!!.stepOrder}: ${selectedNodeForDetail!!.title}")
                }
            },
            text = {
                Column {
                    Text(
                        text = selectedNodeForDetail!!.subtitle,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = selectedNodeForDetail!!.details,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selectedNodeForDetail!!.opticalLevelDbm != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Optical Power Level: ${selectedNodeForDetail!!.opticalLevelDbm} dBm",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedNodeForDetail = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun TraceNodeRow(
    node: TraceNode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (nodeIcon, nodeColor) = when (node.nodeType) {
        "CUSTOMER" -> Pair(Icons.Default.Person, BrandBlue)
        "ONU" -> Pair(Icons.Default.Router, BrandCyan)
        "FDB" -> Pair(Icons.Default.MeetingRoom, StatusAmber)
        "SPLITTER" -> Pair(Icons.Default.CallSplit, StatusPurple)
        "FIBER_CORE" -> Pair(Icons.Default.Cable, Color(0xFF0284C7))
        "PON_PORT", "OLT" -> Pair(Icons.Default.Dns, StatusGreen)
        "SWITCH" -> Pair(Icons.Default.DeviceHub, BrandBlue)
        "CORE_ROUTER" -> Pair(Icons.Default.Lan, StatusPurple)
        "UPSTREAM_ISP" -> Pair(Icons.Default.Cloud, Color(0xFF2563EB))
        "CATV_TAP" -> Pair(Icons.Default.Tv, StatusPurple)
        "EDFA_AMPLIFIER" -> Pair(Icons.Default.Bolt, StatusAmber)
        "OPTICAL_TRANSMITTER" -> Pair(Icons.Default.Sensors, StatusGreen)
        "RECEIVER_IRD", "SATELLITE_DISH" -> Pair(Icons.Default.SatelliteAlt, BrandBlue)
        else -> Pair(Icons.Default.FiberManualRecord, StatusGray)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Step indicator & line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(nodeColor.copy(alpha = 0.2f))
                    .border(1.5.dp, nodeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${node.stepOrder}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = nodeColor
                )
            }
            // Vertical connection line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(26.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Card Content
        Card(
            modifier = Modifier
                .weight(1f)
                .testTag("trace_node_${node.stepOrder}"),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(10.dp),
            border = if (isSelected) CardDefaults.outlinedCardBorder().copy(width = 1.5.dp) else null
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(nodeColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = nodeIcon, contentDescription = null, tint = nodeColor, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = node.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = node.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (node.opticalLevelDbm != null) {
                    Text(
                        text = "${node.opticalLevelDbm} dBm",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                        modifier = Modifier
                            .background(BrandBlue.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
