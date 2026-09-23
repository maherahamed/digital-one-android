package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.IspViewModel
import com.example.ui.components.CapacityProgressBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.*

@Composable
fun IpamCapacityScreen(
    viewModel: IspViewModel
) {
    val subnets by viewModel.subnets.collectAsState()
    val totalCores by viewModel.totalCores.collectAsState()
    val activeCores by viewModel.activeCores.collectAsState()
    val fdbCount by viewModel.fdbCount.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ipam_capacity_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                title = "Network Capacity Gauges",
                subtitle = "Utilization thresholds for hardware ports, PON splitters and fiber"
            )
        }

        // Capacity Cards
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CapacityProgressBar(
                        used = activeCores,
                        total = maxOf(totalCores, 1),
                        label = "Overall Fiber Backbone Core Usage"
                    )
                    CapacityProgressBar(
                        used = 106,
                        total = 256,
                        label = "GPON PON Port ONU Saturation (MA5800 + C320)"
                    )
                    CapacityProgressBar(
                        used = 19,
                        total = 32,
                        label = "FDB / FAT Last-Mile Drop Port Allocation"
                    )
                    CapacityProgressBar(
                        used = 6,
                        total = 16,
                        label = "10G / 100G Switch Uplink Transceivers"
                    )
                }
            }
        }

        // Subnets (IPAM)
        item {
            SectionHeader(
                title = "IP Address Management (IPAM)",
                subtitle = "Public IP Pools, CGNAT Customer Subnets, Management & Resellers"
            )
        }

        items(subnets) { subnet ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("subnet_card_${subnet.subnetId}"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = subnet.subnetCidr,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = BrandBlue
                            )
                            Text(
                                text = "Gateway: ${subnet.gateway} • VLAN ${subnet.vlanId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(status = subnet.ipType)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = subnet.description, style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(10.dp))
                    CapacityProgressBar(
                        used = subnet.usedIps,
                        total = subnet.totalIps,
                        label = "IP Utilization"
                    )
                }
            }
        }
    }
}
