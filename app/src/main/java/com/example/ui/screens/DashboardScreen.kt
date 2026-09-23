package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.IspViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    viewModel: IspViewModel,
    onNavigate: (String) -> Unit
) {
    val oltCount by viewModel.oltCount.collectAsState()
    val routerCount by viewModel.routerCount.collectAsState()
    val switchCount by viewModel.switchCount.collectAsState()
    val cableCount by viewModel.cableCount.collectAsState()
    val totalKm by viewModel.totalKm.collectAsState()
    val totalCores by viewModel.totalCores.collectAsState()
    val activeCores by viewModel.activeCores.collectAsState()
    val spareCores by viewModel.spareCores.collectAsState()
    val internetCust by viewModel.internetCustCount.collectAsState()
    val catvCust by viewModel.catvCustCount.collectAsState()
    val resellerCount by viewModel.resellerCount.collectAsState()
    val fdbCount by viewModel.fdbCount.collectAsState()
    val splitterCount by viewModel.splitterCount.collectAsState()
    val activeFaults by viewModel.activeFaultCount.collectAsState()
    val criticalFaults by viewModel.criticalFaultCount.collectAsState()
    val sites by viewModel.sites.collectAsState()
    val cables by viewModel.cables.collectAsState()
    val recentFaults by viewModel.activeFaults.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header: Digital Twin Operations Center
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_status_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Digital One Network Operations",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Centralized Telemetry & Geographic Infrastructure Twin",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        StatusPill(status = if (criticalFaults > 0) "CRITICAL ALERTS" else "OPERATIONAL")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onNavigate("topology") },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("btn_network_topology"),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                        ) {
                            Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Topology", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { onNavigate("map") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_gis_map")
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Map", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { onNavigate("trace") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_path_trace")
                        ) {
                            Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Trace", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Section 1: Real-Time Recharts Telemetry Overview
        item {
            RechartsOperationsSummaryBar()
        }

        // Section 2: Recharts Real-Time Bandwidth Usage Trends
        item {
            SectionHeader(
                title = "Network Bandwidth Telemetry",
                subtitle = "Recharts real-time throughput trends & peak utilization"
            )
        }

        item {
            RechartsBandwidthChart(
                initialTimeframe = ChartTimeframe.TWENTY_FOUR_HOURS
            )
        }

        // Section 3: Recharts Real-Time Latency & Jitter Trends
        item {
            SectionHeader(
                title = "Latency & QoS Monitoring",
                subtitle = "Core BGP ping, DNS resolvers, and packet loss rate"
            )
        }

        item {
            RechartsLatencyTrendChart()
        }

        // Section 4: Geographic Asset Distribution (Google Maps Placeholder)
        item {
            SectionHeader(
                title = "Geographic Asset Distribution",
                subtitle = "Google Maps Digital Twin (POPs, poles, FDBs & cable routes)",
                actionText = "Fullscreen Map",
                onActionClick = { onNavigate("map") }
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .testTag("dashboard_google_map_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                GoogleMapsNetworkView(
                    modifier = Modifier.fillMaxSize(),
                    sites = sites,
                    cables = cables,
                    isCompact = true,
                    onNavigateFullMap = { onNavigate("map") },
                    onAssetClick = { marker ->
                        if (marker.type == "FAULT") {
                            onNavigate("faults")
                        } else {
                            onNavigate("trace")
                        }
                    }
                )
            }
        }

        // Section 5: Physical Core Infrastructure Stats
        item {
            SectionHeader(
                title = "Physical Infrastructure Assets",
                subtitle = "Distribution hardware, aggregation switches, and OLT chassis",
                actionText = "Manage Devices",
                onActionClick = { onNavigate("devices") }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMetricCard(
                    title = "Total POPs & Hubs",
                    value = "${sites.size}",
                    subtitle = "Central NOC & Sector Hubs",
                    icon = Icons.Default.Hub,
                    iconTint = BrandBlue,
                    modifier = Modifier.weight(1f),
                    testTag = "stat_pops"
                )
                StatMetricCard(
                    title = "OLTs & PON Ports",
                    value = "$oltCount OLTs",
                    subtitle = "Huawei MA5800 & ZTE C320",
                    icon = Icons.Default.Dns,
                    iconTint = BrandCyan,
                    modifier = Modifier.weight(1f),
                    testTag = "stat_olts"
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMetricCard(
                    title = "Core BGP Routers",
                    value = "$routerCount",
                    subtitle = "100G Dual-Homed Transit",
                    icon = Icons.Default.Router,
                    iconTint = StatusPurple,
                    modifier = Modifier.weight(1f),
                    testTag = "stat_routers"
                )
                StatMetricCard(
                    title = "Distribution Switches",
                    value = "$switchCount",
                    subtitle = "Cisco 3850 Aggregation",
                    icon = Icons.Default.DeviceHub,
                    iconTint = StatusGreen,
                    modifier = Modifier.weight(1f),
                    testTag = "stat_switches"
                )
            }
        }

        // Section 6: Fiber Network & Cores Matrix
        item {
            SectionHeader(
                title = "Fiber Cable & Core Utilization",
                subtitle = "TIA-598 color-coded cores and live capacity",
                actionText = "Fiber Manager",
                onActionClick = { onNavigate("fiber") }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMetricCard(
                    title = "Fiber Routes",
                    value = "$cableCount Cables",
                    subtitle = "${String.format("%.1f", totalKm)} KM Total Route",
                    icon = Icons.Default.Cable,
                    iconTint = BrandBlue,
                    modifier = Modifier.weight(1f),
                    testTag = "stat_fiber_cables"
                )
                StatMetricCard(
                    title = "Total Fiber Cores",
                    value = "$totalCores",
                    subtitle = "$activeCores Active | $spareCores Spare",
                    icon = Icons.Default.GridOn,
                    iconTint = if (spareCores < 5) StatusAmber else StatusGreen,
                    modifier = Modifier.weight(1f),
                    testTag = "stat_fiber_cores"
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    CapacityProgressBar(
                        used = activeCores,
                        total = maxOf(totalCores, 1),
                        label = "Overall Metro Fiber Core Utilization"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "FDBs: $fdbCount | Splitters: $splitterCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "CATV Headend: 1550nm EDFA Transmitters",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusPurple
                        )
                    }
                }
            }
        }

        // Section 7: Subscribers & Services
        item {
            SectionHeader(
                title = "Subscriber & Reseller Pool",
                subtitle = "Unified subscriber digital records (Internet + CATV)",
                actionText = "Subscribers",
                onActionClick = { onNavigate("customers") }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMetricCard(
                    title = "Internet Subscribers",
                    value = "$internetCust",
                    subtitle = "GPON PPPoE & Static IP",
                    icon = Icons.Default.Public,
                    iconTint = BrandBlue,
                    modifier = Modifier.weight(1f),
                    testTag = "stat_internet_cust"
                )
                StatMetricCard(
                    title = "CATV Subscribers",
                    value = "$catvCust",
                    subtitle = "RF Optical Dish Drops",
                    icon = Icons.Default.Tv,
                    iconTint = StatusPurple,
                    modifier = Modifier.weight(1f),
                    testTag = "stat_catv_cust"
                )
            }
        }

        // Section 8: Active Alarms & Fault Center
        if (recentFaults.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Active Alarms & Fault Root Cause",
                    subtitle = "$activeFaults active alarms, $criticalFaults critical incident",
                    actionText = "Fault Center",
                    onActionClick = { onNavigate("faults") }
                )
            }

            items(recentFaults) { fault ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("faults") }
                        .testTag("fault_alert_${fault.faultId}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (fault.severity == "CRITICAL") StatusRed.copy(alpha = 0.1f) else StatusAmber.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (fault.severity == "CRITICAL") StatusRed.copy(alpha = 0.2f) else StatusAmber.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (fault.severity == "CRITICAL") StatusRed else StatusAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = fault.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                StatusPill(status = fault.severity)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = fault.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Affected: ${fault.affectedCustomersCount} Subscribers • POP: ${fault.affectedPopName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (fault.severity == "CRITICAL") StatusRed else StatusAmber
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
