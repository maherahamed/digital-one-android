package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun WhatIfPlannerScreen(
    viewModel: IspViewModel
) {
    val whatIfResult by viewModel.whatIfResult.collectAsState()
    val lat by viewModel.plannerLat.collectAsState()
    val lng by viewModel.plannerLng.collectAsState()

    var inputLat by remember { mutableStateOf(lat.toString()) }
    var inputLng by remember { mutableStateOf(lng.toString()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("what_if_planner_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (whatIfResult.isFeasible) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else StatusRed.copy(alpha = 0.15f)
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
                                text = "What-If Expansion Feasibility",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Last-Mile FDB & Core Bottleneck Analyzer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(status = if (whatIfResult.isFeasible) "FEASIBLE" else "BOTTLENECK")
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = whatIfResult.recommendation,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (whatIfResult.isFeasible) StatusGreen else StatusRed
                    )
                }
            }
        }

        // Detailed Metrics
        item {
            SectionHeader(
                title = "Expansion Bottleneck Diagnostics",
                subtitle = "Upstream path from target subscriber GPS"
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Nearest FDB Asset:", style = MaterialTheme.typography.bodySmall)
                        Text(whatIfResult.nearestFdbName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Drop Wire Distance:", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${whatIfResult.nearestFdbDistanceMeters.toInt()} meters",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (whatIfResult.nearestFdbDistanceMeters > 500) StatusAmber else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("FDB Outlet Availability:", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${whatIfResult.fdbSparePorts} spare / ${whatIfResult.fdbTotalPorts} total",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (whatIfResult.hasAvailableFdbPort) StatusGreen else StatusRed
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Upstream Feeder Cable:", style = MaterialTheme.typography.bodySmall)
                        Text("${whatIfResult.upstreamCableCode} (${whatIfResult.upstreamCableSpareCores} Spare Cores)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Estimated Drop Cable Loss:", style = MaterialTheme.typography.bodySmall)
                        Text("${String.format("%.2f", whatIfResult.estimatedDropLossDb)} dB", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Projected Customer RX Power:", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${String.format("%.1f", whatIfResult.estimatedRxPowerDbm)} dBm",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (whatIfResult.estimatedRxPowerDbm < -25.0) StatusAmber else StatusGreen
                        )
                    }
                }
            }
        }

        // Coordinate Inputs
        item {
            SectionHeader(
                title = "Prospective Customer GPS Coordinates",
                subtitle = "Enter prospective site lat/lng or pick preset expansion points"
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = inputLat,
                            onValueChange = { inputLat = it },
                            label = { Text("Latitude") },
                            modifier = Modifier.weight(1f).testTag("input_lat")
                        )
                        OutlinedTextField(
                            value = inputLng,
                            onValueChange = { inputLng = it },
                            label = { Text("Longitude") },
                            modifier = Modifier.weight(1f).testTag("input_lng")
                        )
                    }

                    Button(
                        onClick = {
                            val l1 = inputLat.toDoubleOrNull() ?: 23.8780
                            val l2 = inputLng.toDoubleOrNull() ?: 90.3950
                            viewModel.setPlannerCoordinates(l1, l2)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_analyze_feasibility")
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simulate Connection")
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Preset Locations:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AssistChip(
                            onClick = {
                                inputLat = "23.8780"
                                inputLng = "90.3950"
                                viewModel.setPlannerCoordinates(23.8780, 90.3950)
                            },
                            label = { Text("Sector 7 (Near)", fontSize = 11.sp) }
                        )
                        AssistChip(
                            onClick = {
                                inputLat = "23.8640"
                                inputLng = "90.4040"
                                viewModel.setPlannerCoordinates(23.8640, 90.4040)
                            },
                            label = { Text("Sector 13 Plaza", fontSize = 11.sp) }
                        )
                        AssistChip(
                            onClick = {
                                inputLat = "23.8950"
                                inputLng = "90.3800"
                                viewModel.setPlannerCoordinates(23.8950, 90.3800)
                            },
                            label = { Text("Far Unserved Area", fontSize = 11.sp) }
                        )
                    }
                }
            }
        }
    }
}
