package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.engine.TopologyEngine
import com.example.ui.IspViewModel
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.*

@Composable
fun OpticalEngineeringScreen(
    viewModel: IspViewModel
) {
    var engMode by remember { mutableStateOf(0) } // 0: GPON Optical Budget, 1: CATV RF Budget

    // GPON Inputs
    var txPower by remember { mutableStateOf("+2.8") }
    var distanceKm by remember { mutableStateOf("2.5") }
    var connectors by remember { mutableStateOf("4") }
    var splices by remember { mutableStateOf("3") }
    var splitterLoss by remember { mutableStateOf("17.7") }
    var measuredRx by remember { mutableStateOf("-20.5") }

    // CATV Inputs
    var catvTxLevel by remember { mutableStateOf("10.0") }
    var catvFiberLoss by remember { mutableStateOf("3.5") }
    var catvEdfaGain by remember { mutableStateOf("14.0") }
    var catvTapLoss by remember { mutableStateOf("14.0") }
    var catvDropLoss by remember { mutableStateOf("2.5") }
    var catvMeasuredRf by remember { mutableStateOf("14.2") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("optical_engineering_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TabRow(
                selectedTabIndex = engMode,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
            ) {
                Tab(
                    selected = engMode == 0,
                    onClick = { engMode = 0 },
                    text = { Text("GPON Optical Link Budget", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = engMode == 1,
                    onClick = { engMode = 1 },
                    text = { Text("CATV RF Distribution Budget", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }

        if (engMode == 0) {
            val txVal = txPower.toDoubleOrNull() ?: 2.8
            val distVal = distanceKm.toDoubleOrNull() ?: 2.5
            val connVal = connectors.toIntOrNull() ?: 4
            val spliceVal = splices.toIntOrNull() ?: 3
            val splitVal = splitterLoss.toDoubleOrNull() ?: 17.7
            val rxVal = measuredRx.toDoubleOrNull() ?: -20.5

            val calculation = TopologyEngine.calculateOpticalBudget(
                txPowerDbm = txVal,
                distanceKm = distVal,
                connectorCount = connVal,
                spliceCount = spliceVal,
                splitterLossDb = splitVal,
                measuredRxDbm = rxVal
            )

            // Result Gauge Card
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
                            Text(
                                text = "Calculated Link Budget Results",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            StatusPill(status = calculation.status.name)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Expected RX", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${String.format("%.2f", calculation.expectedRxDbm)} dBm",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 18.sp,
                                    color = BrandBlue
                                )
                            }
                            Column {
                                Text("Total Path Loss", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${String.format("%.2f", calculation.totalLossDb)} dB",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 18.sp
                                )
                            }
                            Column {
                                Text("Optical Margin", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${String.format("%.2f", calculation.opticalMarginDb)} dB",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 18.sp,
                                    color = if (calculation.opticalMarginDb < 3.0) StatusAmber else StatusGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loss Breakdown: Fiber: ${String.format("%.2f", calculation.fiberLossDb)} dB | Connectors: ${calculation.connectorLossDb} dB | Splices: ${calculation.spliceLossDb} dB | Splitters: ${calculation.splitterLossDb} dB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Input fields
            item {
                SectionHeader(title = "Optical Parameter Inputs")
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
                                value = txPower,
                                onValueChange = { txPower = it },
                                label = { Text("OLT TX (dBm)") },
                                modifier = Modifier.weight(1f).testTag("input_tx_power")
                            )
                            OutlinedTextField(
                                value = distanceKm,
                                onValueChange = { distanceKm = it },
                                label = { Text("Span (km)") },
                                modifier = Modifier.weight(1f).testTag("input_distance_km")
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = connectors,
                                onValueChange = { connectors = it },
                                label = { Text("Connectors (Qty)") },
                                modifier = Modifier.weight(1f).testTag("input_connectors")
                            )
                            OutlinedTextField(
                                value = splices,
                                onValueChange = { splices = it },
                                label = { Text("Fusion Splices (Qty)") },
                                modifier = Modifier.weight(1f).testTag("input_splices")
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = splitterLoss,
                                onValueChange = { splitterLoss = it },
                                label = { Text("Splitter Loss (dB)") },
                                modifier = Modifier.weight(1f).testTag("input_splitter_loss")
                            )
                            OutlinedTextField(
                                value = measuredRx,
                                onValueChange = { measuredRx = it },
                                label = { Text("Measured RX (dBm)") },
                                modifier = Modifier.weight(1f).testTag("input_measured_rx")
                            )
                        }
                    }
                }
            }
        } else {
            // CATV RF Engineering
            val rfCalc = TopologyEngine.calculateRfBudget(
                txOutputDbm = catvTxLevel.toDoubleOrNull() ?: 10.0,
                opticalFiberLossDb = catvFiberLoss.toDoubleOrNull() ?: 3.5,
                edfaGainDb = catvEdfaGain.toDoubleOrNull() ?: 14.0,
                tapLossDb = catvTapLoss.toDoubleOrNull() ?: 14.0,
                dropCableLossDb = catvDropLoss.toDoubleOrNull() ?: 2.5,
                measuredRfDbMv = catvMeasuredRf.toDoubleOrNull() ?: 14.2
            )

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("CATV RF Signal Level Result", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            StatusPill(status = rfCalc.status)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Customer RF Level", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${String.format("%.1f", rfCalc.calculatedCustomerRfLevelDbMv)} dBmV",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 18.sp,
                                    color = StatusPurple
                                )
                            }
                            Column {
                                Text("Target Standard", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "10 - 18 dBmV",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 18.sp
                                )
                            }
                            Column {
                                Text("Optical In @ Rx", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${String.format("%.1f", rfCalc.opticalRxDbm)} dBm",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 18.sp,
                                    color = BrandBlue
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "CATV Headend & RF Drop Inputs")
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
                                value = catvTxLevel,
                                onValueChange = { catvTxLevel = it },
                                label = { Text("1550nm TX (dBm)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = catvEdfaGain,
                                onValueChange = { catvEdfaGain = it },
                                label = { Text("EDFA Gain (dB)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = catvTapLoss,
                                onValueChange = { catvTapLoss = it },
                                label = { Text("Tap Value (dB)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = catvMeasuredRf,
                                onValueChange = { catvMeasuredRf = it },
                                label = { Text("Measured (dBmV)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
