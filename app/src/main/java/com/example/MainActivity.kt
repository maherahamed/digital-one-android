package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.IspViewModel
import com.example.ui.screens.*
import com.example.ui.theme.DigitalOneTheme
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusRed
import kotlinx.coroutines.launch

data class NavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val section: String = "Core"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DigitalOneTheme {
                MainAppScaffold()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(viewModel: IspViewModel = viewModel()) {
    var currentRoute by remember { mutableStateOf("dashboard") }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val isFieldOnline by viewModel.isFieldOnline.collectAsState()
    val activeFaults by viewModel.activeFaultCount.collectAsState()

    val navItems = listOf(
        NavItem("dashboard", "Dashboard", Icons.Default.Dashboard, "Core"),
        NavItem("topology", "Topology Dashboard", Icons.Default.Hub, "Core"),
        NavItem("trace", "Route Trace", Icons.Default.Route, "Core"),
        NavItem("fiber", "Fiber & Cores", Icons.Default.Cable, "Core"),
        NavItem("map", "GIS Map", Icons.Default.Map, "Core"),
        NavItem("customers", "Subscribers", Icons.Default.People, "Operations"),
        NavItem("faults", "Fault Center", Icons.Default.Warning, "Operations"),
        NavItem("field", "Field Ops", Icons.Default.Engineering, "Operations"),
        NavItem("devices", "Equipment", Icons.Default.Dns, "Network"),
        NavItem("engineering", "Optical Budget", Icons.Default.Calculate, "Engineering"),
        NavItem("whatif", "What-If Planner", Icons.Default.Psychology, "Engineering"),
        NavItem("ipam", "IPAM & Subnets", Icons.Default.Lan, "Network"),
        NavItem("inventory", "Hardware & QR", Icons.Default.Inventory, "Logistics"),
        NavItem("reports", "Search & Reports", Icons.Default.Search, "System")
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(BrandBlue, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Hub, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Digital One",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ISP Network Management System",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Grouped Navigation Items
                val grouped = navItems.groupBy { it.section }
                grouped.forEach { (section, items) ->
                    Text(
                        text = section.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                    items.forEach { item ->
                        NavigationDrawerItem(
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(item.title, fontSize = 13.sp) },
                            badge = {
                                if (item.route == "faults" && activeFaults > 0) {
                                    Text(
                                        text = "$activeFaults",
                                        color = StatusRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            },
                            selected = currentRoute == item.route,
                            onClick = {
                                currentRoute = item.route
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier
                                .padding(NavigationDrawerItemDefaults.ItemPadding)
                                .testTag("nav_drawer_${item.route}")
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = navItems.find { it.route == currentRoute }?.title ?: "Digital One",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Digital One Broadband & CATV Twin",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("btn_open_drawer")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        // Offline Status indicator icon
                        IconButton(
                            onClick = { viewModel.toggleFieldOnlineMode() },
                            modifier = Modifier.testTag("btn_status_connectivity")
                        ) {
                            Icon(
                                imageVector = if (isFieldOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = "Status",
                                tint = if (isFieldOnline) BrandBlue else StatusAmber
                            )
                        }
                        IconButton(
                            onClick = { currentRoute = "reports" },
                            modifier = Modifier.testTag("btn_top_search")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    val bottomItems = listOf(
                        NavItem("dashboard", "Home", Icons.Default.Dashboard),
                        NavItem("trace", "Trace", Icons.Default.Route),
                        NavItem("fiber", "Fiber", Icons.Default.Cable),
                        NavItem("map", "GIS Map", Icons.Default.Map),
                        NavItem("faults", "Faults", Icons.Default.Warning)
                    )

                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title, fontSize = 11.sp) },
                            selected = currentRoute == item.route,
                            onClick = { currentRoute = item.route },
                            modifier = Modifier.testTag("bottom_nav_${item.route}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentRoute) {
                    "dashboard" -> DashboardScreen(viewModel = viewModel, onNavigate = { currentRoute = it })
                    "topology" -> NetworkTopologyDashboardScreen(viewModel = viewModel, onNavigate = { currentRoute = it })
                    "trace" -> TopologyTraceScreen(viewModel = viewModel)
                    "fiber" -> FiberManagementScreen(viewModel = viewModel)
                    "map" -> GisMapScreen(viewModel = viewModel)
                    "customers" -> CustomerScreen(
                        viewModel = viewModel,
                        onTracePath = { custId, svcType ->
                            viewModel.selectCustomer(custId, svcType)
                            currentRoute = "trace"
                        }
                    )
                    "faults" -> FaultsScreen(viewModel = viewModel)
                    "field" -> FieldOpsScreen(viewModel = viewModel)
                    "devices" -> DeviceManagementScreen(viewModel = viewModel)
                    "engineering" -> OpticalEngineeringScreen(viewModel = viewModel)
                    "whatif" -> WhatIfPlannerScreen(viewModel = viewModel)
                    "ipam" -> IpamCapacityScreen(viewModel = viewModel)
                    "inventory" -> InventoryQrScreen(viewModel = viewModel)
                    "reports" -> ReportsSearchScreen(viewModel = viewModel, onNavigate = { currentRoute = it })
                    else -> DashboardScreen(viewModel = viewModel, onNavigate = { currentRoute = it })
                }
            }
        }
    }
}
