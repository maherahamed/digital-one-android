package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceEntity
import com.example.data.model.FaultEntity
import com.example.data.model.FiberCableEntity
import com.example.ui.IspViewModel
import com.example.ui.components.CapacityProgressBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.*

/**
 * Main Network Topology Dashboard screen.
 * Displays:
 * 1. Visual interactive topology diagram of the ISP hierarchical tiers (Core Transit, Distribution POPs, GPON Access & Fiber Rings).
 * 2. Active Fiber Links Overview (capacity, core occupancy, active vs impaired routes, length, TIA-598 allocations).
 * 3. Device Status Counts (Routers, OLTs, Aggregation Switches, CATV transmitters, status distribution: Online, Warning, Offline).
 * 4. Pending Fault Tickets (Priority severity chips, quick status transitions, ticket creation dialog, dispatch actions).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkTopologyDashboardScreen(
    viewModel: IspViewModel,
    onNavigate: (String) -> Unit = {}
) {
    val devices by viewModel.devices.collectAsState()
    val cables by viewModel.cables.collectAsState()
    val cores by viewModel.cores.collectAsState()
    val sites by viewModel.sites.collectAsState()
    val faults by viewModel.faults.collectAsState()
    val activeFaults by viewModel.activeFaults.collectAsState()

    // Metric counts
    val oltCount by viewModel.oltCount.collectAsState()
    val routerCount by viewModel.routerCount.collectAsState()
    val switchCount by viewModel.switchCount.collectAsState()
    val cableCount by viewModel.cableCount.collectAsState()
    val totalKm by viewModel.totalKm.collectAsState()
    val totalCores by viewModel.totalCores.collectAsState()
    val activeCores by viewModel.activeCores.collectAsState()
    val spareCores by viewModel.spareCores.collectAsState()

    // Filters and state
    var selectedTopologyTier by remember { mutableStateOf("ALL") }
    var selectedDeviceFilter by remember { mutableStateOf("ALL") }
    var selectedTicketFilter by remember { mutableStateOf("OPEN") }
    var selectedCableForDetail by remember { mutableStateOf<FiberCableEntity?>(null) }
    var selectedDeviceForDetail by remember { mutableStateOf<DeviceEntity?>(null) }
    var selectedFaultForAction by remember { mutableStateOf<FaultEntity?>(null) }
    var showCreateTicketDialog by remember { mutableStateOf(false) }

    // Device counts by status
    val onlineDevices = devices.count { it.status.uppercase() == "ONLINE" || it.status.uppercase() == "ACTIVE" }
    val warningDevices = devices.count { it.status.uppercase() == "WARNING" || it.status.uppercase() == "INVESTIGATING" }
    val offlineDevices = devices.count { it.status.uppercase() == "OFFLINE" || it.status.uppercase() == "DOWN" }

    // Active vs impaired links
    val activeCables = cables.filter { it.status.uppercase() == "ACTIVE" }
    val impairedCables = cables.filter { it.status.uppercase() != "ACTIVE" }

    // Pending tickets (open / in progress)
    val pendingTickets = faults.filter {
        when (selectedTicketFilter) {
            "OPEN" -> it.status.uppercase() == "OPEN" || it.status.uppercase() == "INVESTIGATING"
            "CRITICAL" -> it.severity.uppercase() == "CRITICAL" && it.status.uppercase() != "RESOLVED"
            "IN_PROGRESS" -> it.status.uppercase() == "IN_PROGRESS"
            "RESOLVED" -> it.status.uppercase() == "RESOLVED"
            else -> it.status.uppercase() != "RESOLVED"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("network_topology_dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header: Network Topology Command Center
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("topology_hero_banner"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BrandBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Hub,
                                    contentDescription = "Topology",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Network Topology Overview",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Digital One Metro Ring, Device Telemetry & Incident Ops",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        StatusPill(
                            status = if (impairedCables.isNotEmpty() || offlineDevices > 0) "WARNING" else "HEALTHY"
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Key Overview Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TopologyHeroChip(
                            label = "Fiber Links",
                            value = "${cables.size} (${activeCables.size} Up)",
                            icon = Icons.Default.Cable,
                            tint = BrandBlue,
                            modifier = Modifier.weight(1f),
                            testTag = "chip_fiber_links"
                        )
                        TopologyHeroChip(
                            label = "Hardware",
                            value = "$onlineDevices / ${devices.size} Online",
                            icon = Icons.Default.Dns,
                            tint = if (offlineDevices > 0) StatusRed else StatusGreen,
                            modifier = Modifier.weight(1f),
                            testTag = "chip_device_status"
                        )
                        TopologyHeroChip(
                            label = "Pending Faults",
                            value = "${activeFaults.size} Tickets",
                            icon = Icons.Default.Warning,
                            tint = if (activeFaults.any { it.severity == "CRITICAL" }) StatusRed else StatusAmber,
                            modifier = Modifier.weight(1f),
                            testTag = "chip_pending_faults"
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Navigation Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { onNavigate("trace") },
                            modifier = Modifier.weight(1f).testTag("btn_quick_trace"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hop Trace", fontSize = 12.sp)
                        }
                        FilledTonalButton(
                            onClick = { onNavigate("map") },
                            modifier = Modifier.weight(1f).testTag("btn_quick_map"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GIS Twin", fontSize = 12.sp)
                        }
                        Button(
                            onClick = { showCreateTicketDialog = true },
                            modifier = Modifier.weight(1.2f).testTag("btn_new_ticket"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log Incident", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // SECTION 1: Hierarchical Network Topology Diagram
        item {
            SectionHeader(
                title = "Hierarchical Topology Tiers",
                subtitle = "Optical core ring, aggregation distribution & GPON access topology",
                actionText = "Visual Trace",
                onActionClick = { onNavigate("trace") }
            )
        }

        item {
            InteractiveTopologyTreeCard(
                sites = sites,
                devices = devices,
                cables = cables,
                activeFaults = activeFaults,
                selectedTier = selectedTopologyTier,
                onSelectTier = { selectedTopologyTier = it },
                onNavigate = onNavigate
            )
        }

        // SECTION 2: Active Fiber Links Overview
        item {
            SectionHeader(
                title = "Active Fiber Links Overview",
                subtitle = "${cables.size} metro cables spanning ${String.format("%.1f", totalKm)} km • ${activeCores} / $totalCores cores utilized",
                actionText = "Fiber Manager",
                onActionClick = { onNavigate("fiber") }
            )
        }

        item {
            FiberLinksSummaryCard(
                cables = cables,
                cores = cores,
                activeCores = activeCores,
                totalCores = totalCores,
                spareCores = spareCores,
                totalKm = totalKm,
                onSelectCable = { selectedCableForDetail = it }
            )
        }

        // List of Fiber Links with live health
        items(cables) { cable ->
            val cableCores = cores.filter { it.cableId == cable.cableId }
            val activeCoreCount = cableCores.count { it.status == "ACTIVE" }
            val faultOnCable = activeFaults.find { it.fiberCableId == cable.cableId }

            FiberLinkItemCard(
                cable = cable,
                activeCores = activeCoreCount,
                hasFault = faultOnCable != null,
                faultDetails = faultOnCable?.title,
                onClick = { selectedCableForDetail = cable }
            )
        }

        // SECTION 3: Device Status Counts & Distribution
        item {
            SectionHeader(
                title = "Device Status Counts",
                subtitle = "${devices.size} managed devices across Central NOC, POPs and Sector Hubs",
                actionText = "All Devices",
                onActionClick = { onNavigate("devices") }
            )
        }

        item {
            DeviceStatusMatrixCard(
                devices = devices,
                routerCount = routerCount,
                switchCount = switchCount,
                oltCount = oltCount,
                onlineCount = onlineDevices,
                warningCount = warningDevices,
                offlineCount = offlineDevices,
                selectedFilter = selectedDeviceFilter,
                onSelectFilter = { selectedDeviceFilter = it }
            )
        }

        // Filtered device status rows
        val displayedDevices = if (selectedDeviceFilter == "ALL") {
            devices
        } else {
            devices.filter {
                when (selectedDeviceFilter) {
                    "ONLINE" -> it.status.uppercase() == "ONLINE" || it.status.uppercase() == "ACTIVE"
                    "WARNING" -> it.status.uppercase() == "WARNING" || it.status.uppercase() == "INVESTIGATING"
                    "OFFLINE" -> it.status.uppercase() == "OFFLINE" || it.status.uppercase() == "DOWN"
                    "ROUTER" -> it.deviceType.contains("ROUTER")
                    "SWITCH" -> it.deviceType.contains("SWITCH")
                    "OLT" -> it.deviceType.contains("OLT")
                    else -> true
                }
            }
        }

        items(displayedDevices.take(6)) { device ->
            DeviceStatusRowItem(
                device = device,
                onClick = { selectedDeviceForDetail = device }
            )
        }

        if (displayedDevices.size > 6) {
            item {
                TextButton(
                    onClick = { onNavigate("devices") },
                    modifier = Modifier.fillMaxWidth().testTag("btn_view_more_devices")
                ) {
                    Text("View all ${displayedDevices.size} devices in Equipment Manager")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        // SECTION 4: Pending Fault Tickets
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = "Pending Fault Tickets",
                    subtitle = "${activeFaults.size} active network alerts requiring resolution",
                    actionText = "Fault Center",
                    onActionClick = { onNavigate("faults") }
                )
            }
        }

        // Ticket Severity & Status Filter Bar
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("ticket_filter_row")
            ) {
                val filters = listOf(
                    Triple("OPEN", "Open (${faults.count { it.status != "RESOLVED" }})", Icons.Default.Pending),
                    Triple("CRITICAL", "Critical (${faults.count { it.severity == "CRITICAL" && it.status != "RESOLVED" }})", Icons.Default.ErrorOutline),
                    Triple("IN_PROGRESS", "In Progress (${faults.count { it.status == "IN_PROGRESS" }})", Icons.Default.Engineering),
                    Triple("RESOLVED", "Resolved (${faults.count { it.status == "RESOLVED" }})", Icons.Default.CheckCircle),
                    Triple("ALL", "All (${faults.size})", Icons.Default.ListAlt)
                )

                items(filters) { (key, label, icon) ->
                    val isSelected = selectedTicketFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTicketFilter = key },
                        label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (key == "CRITICAL") StatusRed.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = if (key == "CRITICAL") StatusRed else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        if (pendingTickets.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusGreen,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No tickets in '$selectedTicketFilter' filter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "All optical links and core routes operating within normal SLA thresholds",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(pendingTickets) { fault ->
                PendingFaultTicketCard(
                    fault = fault,
                    onSelect = { selectedFaultForAction = fault },
                    onQuickResolve = {
                        viewModel.resolveFault(fault.faultId, "Auto-verified via Topology Operations Dashboard")
                    },
                    onDispatch = {
                        viewModel.updateFaultStatus(fault.faultId, "IN_PROGRESS", "Dispatched field technician")
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal: Cable Detail Dialog
    selectedCableForDetail?.let { cable ->
        CableDetailDialog(
            cable = cable,
            cores = cores.filter { it.cableId == cable.cableId },
            onDismiss = { selectedCableForDetail = null },
            onNavigateFiber = {
                selectedCableForDetail = null
                onNavigate("fiber")
            }
        )
    }

    // Modal: Device Detail Dialog
    selectedDeviceForDetail?.let { device ->
        DeviceDetailDialog(
            device = device,
            onDismiss = { selectedDeviceForDetail = null },
            onNavigateDevices = {
                selectedDeviceForDetail = null
                onNavigate("devices")
            }
        )
    }

    // Modal: Fault Action / Ticket Resolution Dialog
    selectedFaultForAction?.let { fault ->
        FaultTicketActionDialog(
            fault = fault,
            onDismiss = { selectedFaultForAction = null },
            onUpdateStatus = { newStatus, notes ->
                viewModel.updateFaultStatus(fault.faultId, newStatus, notes)
                selectedFaultForAction = null
            },
            onResolve = { notes ->
                viewModel.resolveFault(fault.faultId, notes)
                selectedFaultForAction = null
            }
        )
    }

    // Modal: Create Incident Ticket Dialog
    if (showCreateTicketDialog) {
        CreateIncidentTicketDialog(
            cables = cables,
            devices = devices,
            sites = sites,
            onDismiss = { showCreateTicketDialog = false },
            onSubmit = { title, severity, faultType, desc, pop, cableId, devId, affected ->
                viewModel.createFaultTicket(
                    title = title,
                    severity = severity,
                    faultType = faultType,
                    description = desc,
                    affectedPop = pop,
                    fiberCableId = cableId,
                    deviceId = devId,
                    affectedCustomers = affected
                )
                showCreateTicketDialog = false
            }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// COMPOSABLE COMPONENTS
// -------------------------------------------------------------------------------------------------

@Composable
fun TopologyHeroChip(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        tonalElevation = 2.dp,
        modifier = modifier.testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Visual interactive topology tree diagram representing the ISP hierarchical tiers.
 */
@Composable
fun InteractiveTopologyTreeCard(
    sites: List<com.example.data.model.SiteEntity>,
    devices: List<DeviceEntity>,
    cables: List<FiberCableEntity>,
    activeFaults: List<FaultEntity>,
    selectedTier: String,
    onSelectTier: (String) -> Unit,
    onNavigate: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("interactive_topology_tree_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "End-to-End Hierarchical Topology",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Core Transit → Metro Ring → Distribution Hubs → GPON Access",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BrandBlue.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "LIVE MESH",
                        color = BrandBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tier Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("ALL" to "All Tiers", "CORE" to "Core Transit", "DIST" to "Distribution", "ACCESS" to "GPON Access").forEach { (key, label) ->
                    val isSelected = selectedTier == key
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) BrandBlue else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clickable { onSelectTier(key) }
                            .testTag("tier_chip_$key")
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Flow Nodes Canvas / Cards
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Tier 1: Core Transit & BGP Gateways
                if (selectedTier == "ALL" || selectedTier == "CORE") {
                    TopologyTierNode(
                        tierName = "TIER 1: CORE TRANSIT & BGP EDGE",
                        nodeName = "Central NOC Mohakhali (AS139824)",
                        description = "2x 100G Upstream Transits (Tier-1 IIG) • Juniper MX480 & Cisco ASR9000",
                        badge = "99.99% SLA",
                        badgeColor = StatusGreen,
                        icon = Icons.Default.Cloud,
                        iconBg = Color(0xFF1E40AF),
                        status = "ONLINE",
                        metrics = listOf("Tx: 28.4 Gbps", "Rx: 19.8 Gbps", "Ping: 3.2 ms"),
                        onClick = { onNavigate("devices") }
                    )

                    // Connecting link line
                    TopologyLinkConnector(
                        linkName = "Dual 48-Core Redundant Backbone Ring",
                        status = "HEALTHY",
                        attenuation = "0.22 dB/km"
                    )
                }

                // Tier 2: Aggregation Distribution POPs
                if (selectedTier == "ALL" || selectedTier == "DIST") {
                    TopologyTierNode(
                        tierName = "TIER 2: AGGREGATION & METRO DISTRIBUTION",
                        nodeName = "North POP (Uttara) & South POP (Dhanmondi)",
                        description = "Cisco Catalyst 3850 10G Stacks • EDFA 1550nm CATV Optical Transmitters",
                        badge = "3 Hubs Linked",
                        badgeColor = BrandBlue,
                        icon = Icons.Default.DeviceHub,
                        iconBg = BrandBlue,
                        status = if (activeFaults.any { it.affectedPopName.contains("North") }) "WARNING" else "ONLINE",
                        metrics = listOf("Active Cores: 78", "Split Ratio: 1:4", "Loss: 6.8 dB"),
                        onClick = { onNavigate("map") }
                    )

                    // Connecting link line
                    TopologyLinkConnector(
                        linkName = "24-Core ADSS Feeder Cables to Sector Hubs",
                        status = if (activeFaults.any { it.faultType == "FIBER_CUT" }) "CUT DETECTED" else "HEALTHY",
                        attenuation = "0.35 dB/km"
                    )
                }

                // Tier 3: GPON Access & Drop Terminations
                if (selectedTier == "ALL" || selectedTier == "ACCESS") {
                    TopologyTierNode(
                        tierName = "TIER 3: GPON OLTs & SUBSCRIBER DROPS",
                        nodeName = "Huawei MA5800-X7 & ZTE C320 OLTs",
                        description = "16x GPON Class C+ Ports • 1:8 / 1:16 FDB Enclosures • 864 Active ONUs",
                        badge = "1,850 Endpoints",
                        badgeColor = StatusPurple,
                        icon = Icons.Default.Sensors,
                        iconBg = StatusPurple,
                        status = "ONLINE",
                        metrics = listOf("PON Ports: 32", "Tx Power: +2.5 dBm", "Avg Rx: -18.4 dBm"),
                        onClick = { onNavigate("trace") }
                    )
                }
            }
        }
    }
}

@Composable
fun TopologyTierNode(
    tierName: String,
    nodeName: String,
    description: String,
    badge: String,
    badgeColor: Color,
    icon: ImageVector,
    iconBg: Color,
    status: String,
    metrics: List<String>,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = borderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tierName,
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandBlue,
                    fontWeight = FontWeight.Bold
                )
                StatusPill(status = status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nodeName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = badge,
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                metrics.forEach { metric ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = metric,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopologyLinkConnector(
    linkName: String,
    status: String,
    attenuation: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Left dotted line
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(if (status == "HEALTHY") StatusGreen else StatusRed)
        )

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = borderStroke(1.dp, (if (status == "HEALTHY") StatusGreen else StatusRed).copy(alpha = 0.4f)),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cable,
                    contentDescription = null,
                    tint = if (status == "HEALTHY") StatusGreen else StatusRed,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$linkName ($attenuation)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Right dotted line
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(if (status == "HEALTHY") StatusGreen else StatusRed)
        )
    }
}

/**
 * Summary breakdown of fiber links, utilization and spare capacity.
 */
@Composable
fun FiberLinksSummaryCard(
    cables: List<FiberCableEntity>,
    cores: List<com.example.data.model.FiberCoreEntity>,
    activeCores: Int,
    totalCores: Int,
    spareCores: Int,
    totalKm: Double,
    onSelectCable: (FiberCableEntity) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("fiber_links_summary_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Fiber Core Capacity Matrix",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "TIA-598 Color Code Distribution & Core Occupancy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StatusGreen.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${String.format("%.1f", totalKm)} KM Active",
                        color = StatusGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            CapacityProgressBar(
                used = activeCores,
                total = maxOf(totalCores, 1),
                label = "Core Utilization ($activeCores Active / $spareCores Spare / $totalCores Total)"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Stat pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Active Optical Links", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${cables.count { it.status == "ACTIVE" }} / ${cables.size}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = StatusGreen)
                        Text("100% Redundant Ring", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Spare Core Pool", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$spareCores Cores", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BrandBlue)
                        Text("Ready for Provisioning", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Avg Route Loss", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("1.42 dB", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("0.24 dB/km @ 1550nm", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/**
 * Individual Card representing an active or impaired fiber cable link.
 */
@Composable
fun FiberLinkItemCard(
    cable: FiberCableEntity,
    activeCores: Int,
    hasFault: Boolean,
    faultDetails: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("fiber_link_card_${cable.cableId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasFault) StatusRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = if (hasFault) borderStroke(1.2.dp, StatusRed.copy(alpha = 0.5f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (hasFault) StatusRed.copy(alpha = 0.15f) else BrandBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (hasFault) Icons.Default.Warning else Icons.Default.Cable,
                    contentDescription = null,
                    tint = if (hasFault) StatusRed else BrandBlue,
                    modifier = Modifier.size(24.dp)
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
                        text = "${cable.code} • ${cable.name}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    StatusPill(status = if (hasFault) "FAULT" else cable.status)
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "Route: ${cable.routeName} • ${cable.cableType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Length: ${cable.lengthMeters.toInt()}m (${String.format("%.2f", cable.lengthMeters / 1000.0)} km)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Cores: $activeCores / ${cable.coreCount} Active",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeCores == cable.coreCount) StatusAmber else BrandBlue
                    )
                }

                if (hasFault && faultDetails != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = StatusRed.copy(alpha = 0.12f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = StatusRed, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Active Alarm: $faultDetails",
                                fontSize = 11.sp,
                                color = StatusRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Matrix card displaying device status counts across categories (Routers, Switches, OLTs, and Health).
 */
@Composable
fun DeviceStatusMatrixCard(
    devices: List<DeviceEntity>,
    routerCount: Int,
    switchCount: Int,
    oltCount: Int,
    onlineCount: Int,
    warningCount: Int,
    offlineCount: Int,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("device_status_matrix_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Hardware Health & Status Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Operational status telemetry across all POPs & distribution nodes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Status Distribution Row (Online / Warning / Offline)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DeviceStatusCountBox(
                    label = "Online",
                    count = onlineCount,
                    icon = Icons.Default.CheckCircle,
                    tint = StatusGreen,
                    isSelected = selectedFilter == "ONLINE",
                    onClick = { onSelectFilter(if (selectedFilter == "ONLINE") "ALL" else "ONLINE") },
                    modifier = Modifier.weight(1f),
                    testTag = "box_device_online"
                )
                DeviceStatusCountBox(
                    label = "Warning",
                    count = warningCount,
                    icon = Icons.Default.Warning,
                    tint = StatusAmber,
                    isSelected = selectedFilter == "WARNING",
                    onClick = { onSelectFilter(if (selectedFilter == "WARNING") "ALL" else "WARNING") },
                    modifier = Modifier.weight(1f),
                    testTag = "box_device_warning"
                )
                DeviceStatusCountBox(
                    label = "Offline",
                    count = offlineCount,
                    icon = Icons.Default.Cancel,
                    tint = StatusRed,
                    isSelected = selectedFilter == "OFFLINE",
                    onClick = { onSelectFilter(if (selectedFilter == "OFFLINE") "ALL" else "OFFLINE") },
                    modifier = Modifier.weight(1f),
                    testTag = "box_device_offline"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hardware Category Row (Routers, Switches, OLTs)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DeviceCategoryCountChip(
                    label = "Core Routers",
                    count = "$routerCount units",
                    icon = Icons.Default.Router,
                    isSelected = selectedFilter == "ROUTER",
                    onClick = { onSelectFilter(if (selectedFilter == "ROUTER") "ALL" else "ROUTER") },
                    modifier = Modifier.weight(1f)
                )
                DeviceCategoryCountChip(
                    label = "Switches",
                    count = "$switchCount units",
                    icon = Icons.Default.DeviceHub,
                    isSelected = selectedFilter == "SWITCH",
                    onClick = { onSelectFilter(if (selectedFilter == "SWITCH") "ALL" else "SWITCH") },
                    modifier = Modifier.weight(1f)
                )
                DeviceCategoryCountChip(
                    label = "GPON OLTs",
                    count = "$oltCount chassis",
                    icon = Icons.Default.Dns,
                    isSelected = selectedFilter == "OLT",
                    onClick = { onSelectFilter(if (selectedFilter == "OLT") "ALL" else "OLT") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun DeviceStatusCountBox(
    label: String,
    count: Int,
    icon: ImageVector,
    tint: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) tint.copy(alpha = 0.2f) else tint.copy(alpha = 0.08f),
        border = if (isSelected) borderStroke(1.5.dp, tint) else null,
        modifier = modifier
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$count",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = tint
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DeviceCategoryCountChip(
    label: String,
    count: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = count, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DeviceStatusRowItem(
    device: DeviceEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("device_row_${device.deviceId}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (device.status.uppercase()) {
                                "ONLINE", "ACTIVE" -> StatusGreen.copy(alpha = 0.15f)
                                "WARNING" -> StatusAmber.copy(alpha = 0.15f)
                                else -> StatusRed.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            device.deviceType.contains("ROUTER") -> Icons.Default.Router
                            device.deviceType.contains("SWITCH") -> Icons.Default.DeviceHub
                            device.deviceType.contains("OLT") -> Icons.Default.Dns
                            else -> Icons.Default.Sensors
                        },
                        contentDescription = null,
                        tint = when (device.status.uppercase()) {
                            "ONLINE", "ACTIVE" -> StatusGreen
                            "WARNING" -> StatusAmber
                            else -> StatusRed
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${device.vendor} ${device.model} • IP: ${device.managementIp}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            StatusPill(status = device.status)
        }
    }
}

/**
 * Card for an individual pending fault ticket with action buttons.
 */
@Composable
fun PendingFaultTicketCard(
    fault: FaultEntity,
    onSelect: () -> Unit,
    onQuickResolve: () -> Unit,
    onDispatch: () -> Unit
) {
    val isCritical = fault.severity.uppercase() == "CRITICAL"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("pending_ticket_card_${fault.faultId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCritical) StatusRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = borderStroke(
            1.dp,
            if (isCritical) StatusRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isCritical) StatusRed else StatusAmber
                    ) {
                        Text(
                            text = fault.severity.uppercase(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = fault.faultId,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(status = fault.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = fault.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = fault.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Impact telemetry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Affected: ${fault.affectedCustomersCount} Subscribers • POP: ${fault.affectedPopName}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCritical) StatusRed else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Time: ${fault.startTime}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (fault.status != "IN_PROGRESS" && fault.status != "RESOLVED") {
                    OutlinedButton(
                        onClick = onDispatch,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("btn_dispatch_${fault.faultId}")
                    ) {
                        Icon(Icons.Default.Engineering, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dispatch Tech", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (fault.status != "RESOLVED") {
                    Button(
                        onClick = onQuickResolve,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                        modifier = Modifier.testTag("btn_resolve_${fault.faultId}")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resolve", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// MODAL DIALOGS
// -------------------------------------------------------------------------------------------------

@Composable
fun CableDetailDialog(
    cable: FiberCableEntity,
    cores: List<com.example.data.model.FiberCoreEntity>,
    onDismiss: () -> Unit,
    onNavigateFiber: () -> Unit
) {
    val activeCount = cores.count { it.status == "ACTIVE" }
    val spareCount = cores.count { it.status == "SPARE" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Cable, contentDescription = null, tint = BrandBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "${cable.code} - ${cable.name}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Route Name: ${cable.routeName}", style = MaterialTheme.typography.bodyMedium)
                Text("Cable Type: ${cable.cableType}", style = MaterialTheme.typography.bodyMedium)
                Text("Length: ${cable.lengthMeters.toInt()} meters (${String.format("%.2f", cable.lengthMeters / 1000)} km)", style = MaterialTheme.typography.bodyMedium)
                Text("Core Capacity: ${cable.coreCount} Cores (TIA-598)", style = MaterialTheme.typography.bodyMedium)
                Text("Active Cores: $activeCount | Spare Cores: $spareCount", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = BrandBlue)
                Text("Owner / Operator: ${cable.owner}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Status: ${cable.status}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (cable.status == "ACTIVE") StatusGreen else StatusRed)
            }
        },
        confirmButton = {
            Button(onClick = onNavigateFiber) {
                Text("Open in Fiber Manager")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DeviceDetailDialog(
    device: DeviceEntity,
    onDismiss: () -> Unit,
    onNavigateDevices: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Dns, contentDescription = null, tint = BrandBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = device.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Type: ${device.deviceType}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("Model: ${device.vendor} ${device.model}", style = MaterialTheme.typography.bodyMedium)
                Text("Management IP: ${device.managementIp}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium, color = BrandBlue)
                Text("Rack Location: ${device.rack} (${device.siteId})", style = MaterialTheme.typography.bodyMedium)
                Text("Port Count: ${device.portCount} Ports", style = MaterialTheme.typography.bodyMedium)
                Text("Operational Status: ${device.status}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (device.status == "ONLINE") StatusGreen else StatusRed)
            }
        },
        confirmButton = {
            Button(onClick = onNavigateDevices) {
                Text("Device Telemetry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun FaultTicketActionDialog(
    fault: FaultEntity,
    onDismiss: () -> Unit,
    onUpdateStatus: (String, String) -> Unit,
    onResolve: (String) -> Unit
) {
    var notes by remember { mutableStateOf(fault.resolutionNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = if (fault.severity == "CRITICAL") StatusRed else StatusAmber)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Ticket: ${fault.faultId}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = fault.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(text = fault.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "Severity: ${fault.severity} • Status: ${fault.status}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Text(text = "Affected Subscribers: ${fault.affectedCustomersCount} at ${fault.affectedPopName}", fontSize = 11.sp, color = StatusRed)

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Resolution / NOC Action Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onResolve(notes.ifBlank { "Resolved via Topology Dashboard" }) },
                colors = ButtonDefaults.buttonColors(containerColor = StatusGreen)
            ) {
                Text("Mark Resolved")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onUpdateStatus("IN_PROGRESS", notes) }) {
                    Text("In Progress")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun CreateIncidentTicketDialog(
    cables: List<FiberCableEntity>,
    devices: List<DeviceEntity>,
    sites: List<com.example.data.model.SiteEntity>,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String, String, String, String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("CRITICAL") }
    var faultType by remember { mutableStateOf("FIBER_CUT") }
    var description by remember { mutableStateOf("") }
    var selectedPop by remember { mutableStateOf(sites.firstOrNull()?.name ?: "Mohakhali Central NOC") }
    var selectedCableId by remember { mutableStateOf(cables.firstOrNull()?.cableId ?: "") }
    var selectedDeviceId by remember { mutableStateOf(devices.firstOrNull()?.deviceId ?: "") }
    var affectedCustomers by remember { mutableStateOf("45") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddAlert, contentDescription = null, tint = StatusRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Network Incident Ticket", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Incident Title (e.g. Fiber Cut on Feeder 02)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Severity selector
                Text("Severity Level:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("CRITICAL", "HIGH", "MEDIUM", "LOW").forEach { s ->
                        val isSelected = severity == s
                        FilterChip(
                            selected = isSelected,
                            onClick = { severity = s },
                            label = { Text(s, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (s == "CRITICAL") StatusRed.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }

                // Fault type
                Text("Fault Type:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("FIBER_CUT" to "Fiber Cut", "DEVICE_DOWN" to "Device Down", "HIGH_LOSS" to "High Loss").forEach { (type, lbl) ->
                        FilterChip(
                            selected = faultType == type,
                            onClick = { faultType = type },
                            label = { Text(lbl, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Incident Description & OTDR Distance") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                OutlinedTextField(
                    value = affectedCustomers,
                    onValueChange = { affectedCustomers = it },
                    label = { Text("Estimated Affected Subscribers") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSubmit(
                            title,
                            severity,
                            faultType,
                            description.ifBlank { "Reported through Network Topology Dashboard" },
                            selectedPop,
                            selectedCableId,
                            selectedDeviceId,
                            affectedCustomers.toIntOrNull() ?: 10
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Create Ticket")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper utility for border stroke
private fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) =
    androidx.compose.foundation.BorderStroke(width, color)
