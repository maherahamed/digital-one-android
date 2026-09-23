package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerEntity
import com.example.ui.IspViewModel
import com.example.ui.components.SectionHeader
import com.example.ui.components.ServiceTypeBadge
import com.example.ui.components.StatusPill
import com.example.ui.theme.*

@Composable
fun CustomerScreen(
    viewModel: IspViewModel,
    onTracePath: (String, String) -> Unit
) {
    val customers by viewModel.customers.collectAsState()
    val services by viewModel.customerServices.collectAsState()
    val fdbs by viewModel.fdbs.collectAsState()

    var filterType by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredCustomers = if (filterType == "ALL") {
        customers
    } else {
        customers.filter { it.customerType.equals(filterType, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("customer_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Action Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ALL", "Residential", "Enterprise", "SME").forEach { typ ->
                        item {
                            FilterChip(
                                selected = filterType == typ,
                                onClick = { filterType = typ },
                                label = { Text(typ, fontSize = 12.sp) },
                                modifier = Modifier.testTag("filter_cust_$typ")
                            )
                        }
                    }
                }

                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_add_customer")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Client", fontSize = 12.sp)
                }
            }
        }

        item {
            SectionHeader(
                title = "Subscriber Profiles (${filteredCustomers.size})",
                subtitle = "Unified customer entities with multi-service bundles (Internet + CATV)"
            )
        }

        items(filteredCustomers) { customer ->
            val customerServices = services.filter { it.customerId == customer.customerId }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_card_${customer.customerId}"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = customer.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${customer.customerId} • ${customer.customerType} • ${customer.phone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(status = customer.status)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = customer.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Active Subscriptions (${customerServices.size}):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    customerServices.forEach { svc ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    ServiceTypeBadge(serviceType = svc.serviceType)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = svc.planName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(
                                            text = if (svc.serviceType == "INTERNET") "IP: ${svc.ipAddress} | VLAN ${svc.vlanId}" else "RF: ${svc.rfLevelDbMv} dBmV",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${svc.measuredRxPowerDbm} dBm",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (svc.measuredRxPowerDbm < -24.0) StatusAmber else StatusGreen
                                        )
                                        Text(text = svc.opticalStatus, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            onTracePath(customer.customerId, svc.serviceType)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Route, contentDescription = "Trace Route", tint = BrandBlue, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Customer Dialog
    if (showAddDialog) {
        var newName by remember { mutableStateOf("") }
        var newPhone by remember { mutableStateOf("") }
        var newAddress by remember { mutableStateOf("") }
        var newServiceType by remember { mutableStateOf("INTERNET") }
        var newPlan by remember { mutableStateOf("Fiber High Speed 50 Mbps") }
        var selectedFdb by remember { mutableStateOf(fdbs.firstOrNull()?.fdbId ?: "FDB-NORTH-01") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Provision New Customer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Customer / Enterprise Name") },
                        modifier = Modifier.fillMaxWidth().testTag("input_cust_name")
                    )
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth().testTag("input_cust_phone")
                    )
                    OutlinedTextField(
                        value = newAddress,
                        onValueChange = { newAddress = it },
                        label = { Text("Street Address") },
                        modifier = Modifier.fillMaxWidth().testTag("input_cust_address")
                    )

                    Text("Initial Service:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("INTERNET", "CATV").forEach { srv ->
                            FilterChip(
                                selected = newServiceType == srv,
                                onClick = {
                                    newServiceType = srv
                                    newPlan = if (srv == "INTERNET") "Fiber High Speed 50 Mbps" else "Digital CATV HD 120 Ch"
                                },
                                label = { Text(srv) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newPhone.isNotBlank()) {
                            viewModel.addCustomer(newName, newPhone, newAddress, newServiceType, newPlan, selectedFdb)
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("btn_save_customer")
                ) {
                    Text("Provision")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
