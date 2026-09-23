package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.IspViewModel
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.*

@Composable
fun ReportsSearchScreen(
    viewModel: IspViewModel,
    onNavigate: (String) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()

    var activeReportType by remember { mutableStateOf("FIBER_INVENTORY") }
    var previewCsv by remember { mutableStateOf<String?>(null) }
    var tabIndex by remember { mutableStateOf(0) } // 0: Universal Search, 1: Export Reports, 2: Audit Logs

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reports_search_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TabRow(
                selectedTabIndex = tabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
            ) {
                Tab(
                    selected = tabIndex == 0,
                    onClick = { tabIndex = 0 },
                    text = { Text("Global Search", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = tabIndex == 1,
                    onClick = { tabIndex = 1 },
                    text = { Text("Export Reports", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = tabIndex == 2,
                    onClick = { tabIndex = 2 },
                    text = { Text("Audit Trail", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }

        if (tabIndex == 0) {
            // Global Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    label = { Text("Search IP, ONU serial, customer, fiber cable, fault...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("global_search_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (searchQuery.isNotBlank()) {
                item {
                    SectionHeader(
                        title = "Search Results (${searchResults.size})",
                        subtitle = "Instant multi-table matching"
                    )
                }

                items(searchResults) { res ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "[${res.entityType}]",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = BrandBlue
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = res.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = res.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusPill(status = res.tag)
                        }
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Universal Search Capabilities:", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("• Customer: Ahmed, Sarah, CUST-1001", fontSize = 12.sp)
                            Text("• ONU: 48575443B1A2, ZTEG00921B", fontSize = 12.sp)
                            Text("• IP Address: 103.145.72.15, 10.200.0.1", fontSize = 12.sp)
                            Text("• Cable & Cores: FBR-48, FBR-BB-01", fontSize = 12.sp)
                            Text("• Resellers: Metro NetLink, FastWave", fontSize = 12.sp)
                        }
                    }
                }
            }
        } else if (tabIndex == 1) {
            // Export Reports View
            item {
                SectionHeader(
                    title = "Generate CSV Network Reports",
                    subtitle = "Select dataset to generate production audit CSV"
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "FIBER_INVENTORY" to "Fiber Cables",
                        "CUSTOMER_LIST" to "Subscribers",
                        "FAULTS" to "Fault Log",
                        "IPAM_SUBNETS" to "IPAM Subnets"
                    ).forEach { (type, label) ->
                        FilterChip(
                            selected = activeReportType == type,
                            onClick = {
                                activeReportType = type
                                previewCsv = viewModel.generateExportCsv(type)
                            },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        previewCsv = viewModel.generateExportCsv(activeReportType)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("btn_generate_report")
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate & Preview $activeReportType CSV")
                }
            }

            if (previewCsv != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("CSV Output Preview", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                StatusPill(status = "READY")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = previewCsv!!,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Audit Logs View
            item {
                SectionHeader(
                    title = "System Audit Trail (${auditLogs.size})",
                    subtitle = "Immutable event logs for network changes and provisioning"
                )
            }

            items(auditLogs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = log.action, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandBlue)
                            Text(
                                text = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(log.timestamp)),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "User: ${log.user} • Target: ${log.entityType} [${log.entityId}]", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = log.details, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
