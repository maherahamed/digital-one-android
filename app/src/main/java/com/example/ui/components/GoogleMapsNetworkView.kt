package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FiberCableEntity
import com.example.data.model.SiteEntity
import com.example.ui.theme.*
import kotlin.math.sqrt

enum class GoogleMapType {
    NORMAL,
    SATELLITE,
    TERRAIN
}

data class MapAssetMarker(
    val id: String,
    val title: String,
    val code: String,
    val type: String, // POP, POLE, FDB, CUSTOMER, FAULT
    val lat: Double,
    val lng: Double,
    val status: String,
    val details: String,
    val extraInfo: String = ""
)

data class MapCableRoute(
    val id: String,
    val name: String,
    val type: String,
    val points: List<Pair<Double, Double>>,
    val coreCount: Int,
    val status: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleMapsNetworkView(
    modifier: Modifier = Modifier,
    sites: List<SiteEntity> = emptyList(),
    cables: List<FiberCableEntity> = emptyList(),
    isCompact: Boolean = false,
    onAssetClick: ((MapAssetMarker) -> Unit)? = null,
    onNavigateFullMap: (() -> Unit)? = null
) {
    var mapType by remember { mutableStateOf(GoogleMapType.NORMAL) }
    var zoomScale by remember { mutableFloatStateOf(if (isCompact) 1.0f else 1.2f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedMarker by remember { mutableStateOf<MapAssetMarker?>(null) }
    var showTypeMenu by remember { mutableStateOf(false) }

    // Layer visibility states
    var showPops by remember { mutableStateOf(true) }
    var showFdbs by remember { mutableStateOf(true) }
    var showPoles by remember { mutableStateOf(true) }
    var showCustomers by remember { mutableStateOf(true) }
    var showCables by remember { mutableStateOf(true) }
    var showFaults by remember { mutableStateOf(true) }

    // Pulsing radar animation for fault and NOC location
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    // Map geographic bounds (Dhaka Metropolitan Area)
    val minLat = 23.7200
    val maxLat = 23.8950
    val minLng = 90.3550
    val maxLng = 90.4550

    // Master list of assets
    val markers = remember(sites) {
        val list = mutableListOf<MapAssetMarker>()

        // Core POPs and NOC
        list.add(
            MapAssetMarker(
                id = "SITE-HQ",
                title = "Central NOC Mohakhali",
                code = "NOC-CENTRAL-01",
                type = "POP",
                lat = 23.7925,
                lng = 90.4078,
                status = "ONLINE",
                details = "Tier-3 NOC & Data Center\nBGP Transit: 100 Gbps Dual-Homed\nEDFA 1550nm CATV Headend",
                extraInfo = "Rack Units: 42U | Huawei MA5800-X7 | MikroTik CCR2216"
            )
        )
        list.add(
            MapAssetMarker(
                id = "SITE-POP-NORTH",
                title = "North POP Hub (Uttara)",
                code = "POP-NORTH-01",
                type = "POP",
                lat = 23.8722,
                lng = 90.3980,
                status = "ONLINE",
                details = "Sector Distribution Hub\nZTE C320 GPON OLT (16 Ports Active)\nFiber Backbone: 48 Cores",
                extraInfo = "Connected Subscribers: 1,240 | Optical Rx: -18.2 dBm"
            )
        )
        list.add(
            MapAssetMarker(
                id = "SITE-POP-SOUTH",
                title = "South POP Hub (Dhanmondi)",
                code = "POP-SOUTH-01",
                type = "POP",
                lat = 23.7461,
                lng = 90.3742,
                status = "ONLINE",
                details = "Distribution Sub-Center\nFiber Backbone: 24 Cores ADSS\nCisco 3850 Aggregation Switch",
                extraInfo = "Connected Subscribers: 880 | Resellers: 3"
            )
        )
        list.add(
            MapAssetMarker(
                id = "SITE-POP-EAST",
                title = "East POP Hub (Gulshan)",
                code = "POP-EAST-01",
                type = "POP",
                lat = 23.8155,
                lng = 90.4352,
                status = "ONLINE",
                details = "Corporate & Diplomatic Zone Hub\n100G Metro Ethernet Ring\nRedundant DC Power Plant",
                extraInfo = "Connected Corporate Leased Lines: 42"
            )
        )
        list.add(
            MapAssetMarker(
                id = "SITE-SUB-MIRPUR",
                title = "Sub-Hub Mirpur-10",
                code = "HUB-MIRPUR-02",
                type = "POP",
                lat = 23.8070,
                lng = 90.3685,
                status = "ONLINE",
                details = "Mirpur Residential Feeder Node\nPassive Splice Enclosure & EDFA Node",
                extraInfo = "Splitter 1:16 Enclosures: 4"
            )
        )

        // Distribution Poles
        list.add(
            MapAssetMarker(
                id = "POLE-101",
                title = "Pole 101 (Uttara Sector 3)",
                code = "POL-UTT-101",
                type = "FAULT",
                lat = 23.8650,
                lng = 90.4010,
                status = "CRITICAL",
                details = "FIBER CUT: 48-Core Backbone Cable Severed\nDamage by Road Construction Excavator\nOTDR Distance: 2.45 km from North POP",
                extraInfo = "Dispatched Tech: Rakibul Hasan (Team Alpha)"
            )
        )
        list.add(
            MapAssetMarker(
                id = "POLE-102",
                title = "Pole 102 (Airport Road)",
                code = "POL-AIR-102",
                type = "POLE",
                lat = 23.8580,
                lng = 90.4030,
                status = "ONLINE",
                details = "Aerial Fiber Suspension & Slack Loop Enclosure",
                extraInfo = "Slack Loop: 30m Coil"
            )
        )
        list.add(
            MapAssetMarker(
                id = "POLE-201",
                title = "Pole 201 (Dhanmondi 27)",
                code = "POL-DHAN-201",
                type = "POLE",
                lat = 23.7530,
                lng = 90.3710,
                status = "ONLINE",
                details = "Feeder Cable Tension Clamp & FDB Enclosure",
                extraInfo = "Ground Resistance: 4.2 Ohm"
            )
        )

        // FDBs (Fiber Distribution Boxes)
        list.add(
            MapAssetMarker(
                id = "FDB-01",
                title = "FDB-NORTH-01 (Sector 4)",
                code = "FDB-UTT-01",
                type = "FDB",
                lat = 23.8760,
                lng = 90.3920,
                status = "ONLINE",
                details = "FAT Box: 16 Drop Ports (7 Active, 9 Spare)\nSplitter: 1:8 PLC (Loss: 10.3 dB)",
                extraInfo = "Optical In: -14.2 dBm | Drop Out: -19.4 dBm"
            )
        )
        list.add(
            MapAssetMarker(
                id = "FDB-02",
                title = "FDB-NORTH-02 (Sector 7)",
                code = "FDB-UTT-02",
                type = "FDB",
                lat = 23.8820,
                lng = 90.3995,
                status = "ONLINE",
                details = "FAT Box: 16 Drop Ports (12 Active, 4 Spare)\nSplitter: 1:16 PLC (Loss: 13.8 dB)",
                extraInfo = "Optical In: -15.1 dBm | Drop Out: -21.2 dBm"
            )
        )
        list.add(
            MapAssetMarker(
                id = "FDB-03",
                title = "FDB-DHAN-01 (Road 8A)",
                code = "FDB-DHAN-01",
                type = "FDB",
                lat = 23.7490,
                lng = 90.3780,
                status = "ONLINE",
                details = "FAT Box: 8 Drop Ports (6 Active, 2 Spare)\nSplitter: 1:8 PLC",
                extraInfo = "Optical In: -13.8 dBm"
            )
        )
        list.add(
            MapAssetMarker(
                id = "FDB-04",
                title = "FDB-GUL-01 (Road 11)",
                code = "FDB-GUL-01",
                type = "FDB",
                lat = 23.7910,
                lng = 90.4180,
                status = "ONLINE",
                details = "Commercial FDB: 24 Ports (18 Active, 6 Spare)\nDedicated Dual Core Uplink",
                extraInfo = "Optical In: -12.5 dBm"
            )
        )

        // Sample Subscriber Drop Points
        list.add(
            MapAssetMarker(
                id = "CUST-1001",
                title = "Mahmudul Hasan (ONU)",
                code = "CUST-001001",
                type = "CUSTOMER",
                lat = 23.8755,
                lng = 90.3925,
                status = "ONLINE",
                details = "Services: 50 Mbps GPON + Digital CATV\nONU Serial: HWTC-7A8B9C01\nOptical Power: -19.4 dBm",
                extraInfo = "Address: House 12, Road 5, Uttara Sec 4"
            )
        )
        list.add(
            MapAssetMarker(
                id = "CUST-1002",
                title = "Fatima Enterprise (Office)",
                code = "CUST-001002",
                type = "CUSTOMER",
                lat = 23.8768,
                lng = 90.3918,
                status = "ONLINE",
                details = "Services: 100 Mbps Dedicated IP Transit\nONU Serial: ZTEG-4D5E6F02\nOptical Power: -20.1 dBm",
                extraInfo = "Address: Plaza Level 3, Sector 4"
            )
        )
        list.add(
            MapAssetMarker(
                id = "CUST-2001",
                title = "Rahman Tech Solutions",
                code = "CUST-002001",
                type = "CUSTOMER",
                lat = 23.7485,
                lng = 90.3788,
                status = "ONLINE",
                details = "Services: 150 Mbps Static IP Leased Line\nONU Serial: HWTC-11223344",
                extraInfo = "Address: Road 8A, Dhanmondi"
            )
        )
        list
    }

    // Fiber Cable Routes with coordinates
    val cableRoutes = remember {
        listOf(
            // Route 1: Backbone HQ -> North POP (via Mohakhali -> Airport Rd -> Uttara)
            MapCableRoute(
                id = "CABLE-BACKBONE-NORTH",
                name = "Backbone-North (48 Cores)",
                type = "BACKBONE",
                points = listOf(
                    Pair(23.7925, 90.4078), // HQ
                    Pair(23.8150, 90.4150),
                    Pair(23.8350, 90.4120),
                    Pair(23.8580, 90.4030), // Pole 102
                    Pair(23.8650, 90.4010), // Pole 101 (Cut point)
                    Pair(23.8722, 90.3980)  // North POP
                ),
                coreCount = 48,
                status = "SEVERED_CUT",
                color = StatusRed
            ),
            // Route 2: Backbone HQ -> South POP (Dhanmondi via Farmgate & Mirpur Rd)
            MapCableRoute(
                id = "CABLE-BACKBONE-SOUTH",
                name = "Backbone-South (24 Cores)",
                type = "BACKBONE",
                points = listOf(
                    Pair(23.7925, 90.4078), // HQ
                    Pair(23.7750, 90.3980),
                    Pair(23.7600, 90.3850),
                    Pair(23.7530, 90.3710), // Pole 201
                    Pair(23.7461, 90.3742)  // South POP
                ),
                coreCount = 24,
                status = "ACTIVE",
                color = BrandBlue
            ),
            // Route 3: Backbone HQ -> East POP (Gulshan Ring)
            MapCableRoute(
                id = "CABLE-BACKBONE-EAST",
                name = "Backbone-East (24 Cores)",
                type = "BACKBONE",
                points = listOf(
                    Pair(23.7925, 90.4078), // HQ
                    Pair(23.7990, 90.4180),
                    Pair(23.8155, 90.4352)  // East POP
                ),
                coreCount = 24,
                status = "ACTIVE",
                color = StatusPurple
            ),
            // Route 4: Feeder North POP -> FDB-01 & FDB-02
            MapCableRoute(
                id = "CABLE-FEEDER-NORTH-1",
                name = "Feeder-Uttara-Sec4 (8 Cores)",
                type = "DISTRIBUTION",
                points = listOf(
                    Pair(23.8722, 90.3980), // North POP
                    Pair(23.8745, 90.3950),
                    Pair(23.8760, 90.3920)  // FDB-01
                ),
                coreCount = 8,
                status = "ACTIVE",
                color = StatusGreen
            ),
            MapCableRoute(
                id = "CABLE-FEEDER-NORTH-2",
                name = "Feeder-Uttara-Sec7 (8 Cores)",
                type = "DISTRIBUTION",
                points = listOf(
                    Pair(23.8722, 90.3980), // North POP
                    Pair(23.8780, 90.3990),
                    Pair(23.8820, 90.3995)  // FDB-02
                ),
                coreCount = 8,
                status = "ACTIVE",
                color = StatusGreen
            ),
            // Route 5: Drop Cable FDB-01 -> Customer 1001 & 1002
            MapCableRoute(
                id = "CABLE-DROP-1001",
                name = "Drop-Customer-1001 (1 Core)",
                type = "DROP",
                points = listOf(
                    Pair(23.8760, 90.3920), // FDB-01
                    Pair(23.8755, 90.3925)  // Customer 1001
                ),
                coreCount = 1,
                status = "ACTIVE",
                color = Color(0xFF38BDF8)
            ),
            MapCableRoute(
                id = "CABLE-DROP-1002",
                name = "Drop-Customer-1002 (1 Core)",
                type = "DROP",
                points = listOf(
                    Pair(23.8760, 90.3920), // FDB-01
                    Pair(23.8768, 90.3918)  // Customer 1002
                ),
                coreCount = 1,
                status = "ACTIVE",
                color = Color(0xFF38BDF8)
            ),
            // Route 6: Backbone HQ -> Mirpur Sub-Hub
            MapCableRoute(
                id = "CABLE-FEEDER-MIRPUR",
                name = "Feeder-Mirpur-Link (12 Cores)",
                type = "DISTRIBUTION",
                points = listOf(
                    Pair(23.7925, 90.4078),
                    Pair(23.8010, 90.3850),
                    Pair(23.8070, 90.3685)
                ),
                coreCount = 12,
                status = "ACTIVE",
                color = BrandCyan
            )
        )
    }

    // Filtered markers based on search and toggles
    val filteredMarkers = remember(markers, searchQuery, showPops, showFdbs, showPoles, showCustomers, showFaults) {
        markers.filter { m ->
            val matchesType = when (m.type) {
                "POP" -> showPops
                "FDB" -> showFdbs
                "POLE" -> showPoles
                "CUSTOMER" -> showCustomers
                "FAULT" -> showFaults
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                m.title.contains(searchQuery, ignoreCase = true) ||
                m.code.contains(searchQuery, ignoreCase = true) ||
                m.type.contains(searchQuery, ignoreCase = true)

            matchesType && matchesSearch
        }
    }

    // Card Container
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (mapType == GoogleMapType.SATELLITE) Color(0xFF142018) else Color(0xFFF1F3F4))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
    ) {
        // Map Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.7f, 4.0f)
                        panOffset += pan
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        // Hit-test against markers
                        var hitMarker: MapAssetMarker? = null
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()

                        for (marker in filteredMarkers) {
                            val normX = (marker.lng - minLng) / (maxLng - minLng)
                            val normY = 1.0 - (marker.lat - minLat) / (maxLat - minLat)
                            val rawX = (normX * w * 0.82f + w * 0.09f).toFloat()
                            val rawY = (normY * h * 0.82f + h * 0.09f).toFloat()
                            val zx = (rawX - w / 2) * zoomScale + w / 2 + panOffset.x
                            val zy = (rawY - h / 2) * zoomScale + h / 2 + panOffset.y

                            val dist = sqrt((tapOffset.x - zx) * (tapOffset.x - zx) + (tapOffset.y - zy) * (tapOffset.y - zy))
                            if (dist < 32f) {
                                hitMarker = marker
                                break
                            }
                        }

                        selectedMarker = hitMarker
                        if (hitMarker != null && onAssetClick != null) {
                            onAssetClick(hitMarker)
                        }
                    }
                }
        ) {
            val canvasW = size.width
            val canvasH = size.height

            fun geoToCanvas(lat: Double, lng: Double): Offset {
                val normX = (lng - minLng) / (maxLng - minLng)
                val normY = 1.0 - (lat - minLat) / (maxLat - minLat)
                val rawX = (normX * canvasW * 0.82f + canvasW * 0.09f).toFloat()
                val rawY = (normY * canvasH * 0.82f + canvasH * 0.09f).toFloat()
                val zx = (rawX - canvasW / 2) * zoomScale + canvasW / 2 + panOffset.x
                val zy = (rawY - canvasH / 2) * zoomScale + canvasH / 2 + panOffset.y
                return Offset(zx, zy)
            }

            // 1. Draw Google Maps Base Cartography
            drawGoogleCartography(
                mapType = mapType,
                w = canvasW,
                h = canvasH,
                geoToCanvas = ::geoToCanvas,
                zoom = zoomScale
            )

            // 2. Draw Cable Routes (Polylines)
            if (showCables) {
                cableRoutes.forEach { route ->
                    if (route.points.size >= 2) {
                        val path = Path()
                        val firstPt = geoToCanvas(route.points[0].first, route.points[0].second)
                        path.moveTo(firstPt.x, firstPt.y)

                        for (i in 1 until route.points.size) {
                            val pt = geoToCanvas(route.points[i].first, route.points[i].second)
                            path.lineTo(pt.x, pt.y)
                        }

                        // Outer halo stroke for high contrast against Google satellite/road background
                        drawPath(
                            path = path,
                            color = if (mapType == GoogleMapType.SATELLITE) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.8f),
                            style = Stroke(
                                width = when (route.type) {
                                    "BACKBONE" -> 6f * zoomScale
                                    "DISTRIBUTION" -> 5f * zoomScale
                                    else -> 3.5f * zoomScale
                                },
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Main route line
                        val isSevered = route.status.contains("CUT")
                        drawPath(
                            path = path,
                            color = if (isSevered) StatusRed else route.color,
                            style = Stroke(
                                width = when (route.type) {
                                    "BACKBONE" -> 4f * zoomScale
                                    "DISTRIBUTION" -> 3f * zoomScale
                                    else -> 2f * zoomScale
                                },
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                                pathEffect = if (isSevered) PathEffect.dashPathEffect(floatArrayOf(20f, 12f), 0f) else null
                            )
                        )
                    }
                }
            }

            // 3. Draw Active Cut Pulse Indicator
            if (showFaults) {
                val cutPos = geoToCanvas(23.8650, 90.4010) // Pole 101
                drawCircle(
                    color = StatusRed.copy(alpha = pulseAlpha * 0.4f),
                    radius = (pulseRadius * 1.8f) * zoomScale,
                    center = cutPos
                )
                drawCircle(
                    color = StatusRed.copy(alpha = pulseAlpha),
                    radius = pulseRadius * zoomScale,
                    center = cutPos,
                    style = Stroke(width = 2.5f)
                )
            }

            // 4. Draw NOC Pulse Indicator
            val nocPos = geoToCanvas(23.7925, 90.4078)
            drawCircle(
                color = BrandBlue.copy(alpha = pulseAlpha * 0.35f),
                radius = (pulseRadius * 1.5f) * zoomScale,
                center = nocPos
            )

            // 5. Draw Asset Markers (Google Pin Style)
            filteredMarkers.forEach { marker ->
                val pos = geoToCanvas(marker.lat, marker.lng)
                val isSelected = selectedMarker?.id == marker.id

                val (pinColor, iconBg) = when (marker.type) {
                    "POP" -> Pair(BrandBlue, Color(0xFF0284C7))
                    "POLE" -> Pair(StatusAmber, Color(0xFFF59E0B))
                    "FDB" -> Pair(StatusGreen, Color(0xFF10B981))
                    "CUSTOMER" -> Pair(Color(0xFF0284C7), Color(0xFF38BDF8))
                    "FAULT" -> Pair(StatusRed, Color(0xFFEF4444))
                    else -> Pair(StatusGray, Color.Gray)
                }

                val pinScale = if (isSelected) 1.35f else 1.0f
                val pinRadius = when (marker.type) {
                    "POP" -> 14f * zoomScale * pinScale
                    "FAULT" -> 15f * zoomScale * pinScale
                    "FDB" -> 11f * zoomScale * pinScale
                    "POLE" -> 9f * zoomScale * pinScale
                    else -> 8f * zoomScale * pinScale
                }

                // Marker Shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.25f),
                    radius = pinRadius * 1.15f,
                    center = Offset(pos.x + 2f, pos.y + 3f)
                )

                // Marker Pin Head
                drawCircle(
                    color = pinColor,
                    radius = pinRadius,
                    center = pos
                )

                // Pin White Border
                drawCircle(
                    color = Color.White,
                    radius = pinRadius,
                    center = pos,
                    style = Stroke(width = 2.5f)
                )

                // Pin Core Center
                drawCircle(
                    color = Color.White,
                    radius = pinRadius * 0.45f,
                    center = pos
                )

                // Selection Ring
                if (isSelected) {
                    drawCircle(
                        color = BrandCyan,
                        radius = pinRadius + 8f,
                        center = pos,
                        style = Stroke(width = 3f)
                    )
                }
            }
        }

        // --- Top Bar: Google Maps Search Card & Filter Controls ---
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Google Maps Floating Search Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = if (mapType == GoogleMapType.SATELLITE) Color(0xEA1E293B) else Color.White,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Google Logo colored "G" or Search icon
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(BrandBlue.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = BrandBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = if (isCompact) "Search assets..." else "Search POPs, poles, cables, FDBs...",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("map_search_input"),
                        singleLine = true
                    )

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }

                    // Map Layer Type Button
                    IconButton(
                        onClick = { showTypeMenu = !showTypeMenu },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("btn_map_layers")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Map Layers",
                            tint = if (mapType != GoogleMapType.NORMAL) BrandBlue else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (onNavigateFullMap != null && isCompact) {
                        IconButton(
                            onClick = onNavigateFullMap,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.OpenInFull, contentDescription = "Full Screen", tint = BrandBlue)
                        }
                    }
                }
            }

            // Map Type Dropdown / Menu
            DropdownMenu(
                expanded = showTypeMenu,
                onDismissRequest = { showTypeMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Standard Map", fontWeight = if (mapType == GoogleMapType.NORMAL) FontWeight.Bold else FontWeight.Normal) },
                    onClick = {
                        mapType = GoogleMapType.NORMAL
                        showTypeMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Map, contentDescription = null, tint = BrandBlue) }
                )
                DropdownMenuItem(
                    text = { Text("Satellite Hybrid", fontWeight = if (mapType == GoogleMapType.SATELLITE) FontWeight.Bold else FontWeight.Normal) },
                    onClick = {
                        mapType = GoogleMapType.SATELLITE
                        showTypeMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Satellite, contentDescription = null, tint = StatusGreen) }
                )
                DropdownMenuItem(
                    text = { Text("Terrain Relief", fontWeight = if (mapType == GoogleMapType.TERRAIN) FontWeight.Bold else FontWeight.Normal) },
                    onClick = {
                        mapType = GoogleMapType.TERRAIN
                        showTypeMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Terrain, contentDescription = null, tint = StatusAmber) }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(if (showCables) "Hide Fiber Cables" else "Show Fiber Cables") },
                    onClick = { showCables = !showCables }
                )
                DropdownMenuItem(
                    text = { Text(if (showFaults) "Hide Fault Alert" else "Show Fault Alert") },
                    onClick = { showFaults = !showFaults }
                )
            }

            // Quick Layer Filter Chips (Non-compact only)
            if (!isCompact) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = showPops,
                        onClick = { showPops = !showPops },
                        label = { Text("POPs (5)", fontSize = 10.sp) },
                        leadingIcon = {
                            Box(modifier = Modifier.size(8.dp).background(BrandBlue, CircleShape))
                        }
                    )
                    FilterChip(
                        selected = showFdbs,
                        onClick = { showFdbs = !showFdbs },
                        label = { Text("FDB (4)", fontSize = 10.sp) },
                        leadingIcon = {
                            Box(modifier = Modifier.size(8.dp).background(StatusGreen, CircleShape))
                        }
                    )
                    FilterChip(
                        selected = showFaults,
                        onClick = { showFaults = !showFaults },
                        label = { Text("Faults (1)", fontSize = 10.sp) },
                        leadingIcon = {
                            Box(modifier = Modifier.size(8.dp).background(StatusRed, CircleShape))
                        }
                    )
                }
            }
        }

        // --- Google Maps Floating Controls (Right Side) ---
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Compass
            Surface(
                modifier = Modifier
                    .size(38.dp)
                    .shadow(4.dp, CircleShape),
                shape = CircleShape,
                color = if (mapType == GoogleMapType.SATELLITE) Color(0xCC1E293B) else Color.White
            ) {
                IconButton(
                    onClick = {
                        panOffset = Offset.Zero
                        zoomScale = 1.15f
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "Compass / Reset North",
                        tint = StatusRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // My Location / Center on NOC
            Surface(
                modifier = Modifier
                    .size(38.dp)
                    .shadow(4.dp, CircleShape),
                shape = CircleShape,
                color = if (mapType == GoogleMapType.SATELLITE) Color(0xCC1E293B) else Color.White
            ) {
                IconButton(
                    onClick = {
                        panOffset = Offset.Zero
                        zoomScale = 1.35f
                    },
                    modifier = Modifier.testTag("btn_map_my_location")
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Center on NOC",
                        tint = BrandBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Zoom In / Out Pill
            Surface(
                modifier = Modifier
                    .width(38.dp)
                    .shadow(4.dp, RoundedCornerShape(19.dp)),
                shape = RoundedCornerShape(19.dp),
                color = if (mapType == GoogleMapType.SATELLITE) Color(0xCC1E293B) else Color.White
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { zoomScale = (zoomScale * 1.3f).coerceAtMost(4.0f) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(18.dp))
                    }
                    HorizontalDivider(modifier = Modifier.width(26.dp))
                    IconButton(
                        onClick = { zoomScale = (zoomScale / 1.3f).coerceAtLeast(0.7f) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // --- Bottom Left: Google Watermark & Scale Bar ---
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = if (selectedMarker != null) 160.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Google Maps Styled Wordmark
            Surface(
                color = Color.White.copy(alpha = 0.85f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = "Google",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = Color(0xFF4285F4),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontFamily = FontFamily.SansSerif
                )
            }

            // Scale Bar
            val scaleMeters = when {
                zoomScale >= 2.5f -> "100 m"
                zoomScale >= 1.8f -> "250 m"
                zoomScale >= 1.2f -> "500 m"
                zoomScale >= 0.9f -> "1 km"
                else -> "2 km"
            }
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(2.dp)
                            .background(Color.White)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = scaleMeters, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- Bottom Selected Marker Place Card (Google Maps Style) ---
        AnimatedVisibility(
            visible = selectedMarker != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(10.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            selectedMarker?.let { marker ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(
                                                when (marker.type) {
                                                    "POP" -> BrandBlue
                                                    "FAULT" -> StatusRed
                                                    "FDB" -> StatusGreen
                                                    else -> StatusAmber
                                                },
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = marker.type,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• ${marker.code}",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = marker.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = { selectedMarker = null },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = marker.details,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )

                        if (marker.extraInfo.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = marker.extraInfo,
                                fontSize = 10.sp,
                                color = BrandBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { selectedMarker = null },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Directions", fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    if (onAssetClick != null) onAssetClick(marker)
                                    selectedMarker = null
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Trace Core", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Draws cartographic base layer resembling Google Maps road map or satellite view
 */
private fun DrawScope.drawGoogleCartography(
    mapType: GoogleMapType,
    w: Float,
    h: Float,
    geoToCanvas: (Double, Double) -> Offset,
    zoom: Float
) {
    val isSat = mapType == GoogleMapType.SATELLITE
    val isTerrain = mapType == GoogleMapType.TERRAIN

    // Background land color
    val landColor = when {
        isSat -> Color(0xFF142018)
        isTerrain -> Color(0xFFE8E8DF)
        else -> Color(0xFFF2EFE9)
    }
    drawRect(color = landColor, size = Size(w, h))

    // 1. Parks / Greenery (Ramna, Gulshan park, Cantonment)
    val parkColor = when {
        isSat -> Color(0xFF1F3523)
        else -> Color(0xFFCEEAD6)
    }
    drawCircle(
        color = parkColor,
        radius = 70f * zoom,
        center = geoToCanvas(23.7380, 90.4000) // Ramna Park area
    )
    drawCircle(
        color = parkColor,
        radius = 90f * zoom,
        center = geoToCanvas(23.8200, 90.4050) // Cantonment green zone
    )

    // 2. Water Bodies (Hatirjheel Lake, Gulshan Lake, Buriganga River)
    val waterColor = when {
        isSat -> Color(0xFF0F1E29)
        else -> Color(0xFFA5DCFF)
    }

    // Buriganga River (South curve)
    val riverPath = Path()
    val r0 = geoToCanvas(23.7050, 90.3550)
    val r1 = geoToCanvas(23.7120, 90.3800)
    val r2 = geoToCanvas(23.7080, 90.4100)
    val r3 = geoToCanvas(23.7150, 90.4400)
    riverPath.moveTo(r0.x, r0.y)
    riverPath.quadraticBezierTo(r1.x, r1.y, r2.x, r2.y)
    riverPath.quadraticBezierTo(r2.x, r2.y, r3.x, r3.y)
    drawPath(
        path = riverPath,
        color = waterColor,
        style = Stroke(width = 16f * zoom, cap = StrokeCap.Round)
    )

    // Hatirjheel & Gulshan Lake (Central)
    val lakePath = Path()
    val l0 = geoToCanvas(23.7700, 90.4050)
    val l1 = geoToCanvas(23.7800, 90.4150)
    val l2 = geoToCanvas(23.8050, 90.4200)
    lakePath.moveTo(l0.x, l0.y)
    lakePath.lineTo(l1.x, l1.y)
    lakePath.lineTo(l2.x, l2.y)
    drawPath(
        path = lakePath,
        color = waterColor,
        style = Stroke(width = 12f * zoom, cap = StrokeCap.Round)
    )

    // 3. Primary Highways & Expressways (Google Yellow/Orange Roads)
    val highwayCasingColor = if (isSat) Color(0x66000000) else Color(0xFFE2D6C0)
    val highwayFillColor = if (isSat) Color(0xCCFFFFFF) else Color(0xFFFDE293)
    val primaryRoadFill = if (isSat) Color(0x99FFFFFF) else Color(0xFFFFFFFF)

    // Airport Road (Central Arterial N-S)
    val arterialRoads = listOf(
        // Airport Rd: Mohakhali -> Banani -> Airport -> Uttara
        listOf(
            geoToCanvas(23.7750, 90.4050),
            geoToCanvas(23.7925, 90.4078),
            geoToCanvas(23.8250, 90.4130),
            geoToCanvas(23.8550, 90.4030),
            geoToCanvas(23.8850, 90.3980)
        ),
        // Mirpur Rd: Dhanmondi -> Shyamoli -> Mirpur-10
        listOf(
            geoToCanvas(23.7380, 90.3750),
            geoToCanvas(23.7650, 90.3700),
            geoToCanvas(23.8070, 90.3685),
            geoToCanvas(23.8300, 90.3700)
        ),
        // Pragati Sarani (East Corridor)
        listOf(
            geoToCanvas(23.7700, 90.4250),
            geoToCanvas(23.8000, 90.4280),
            geoToCanvas(23.8300, 90.4250)
        ),
        // E-W Link: Mirpur-10 -> Banani -> Gulshan
        listOf(
            geoToCanvas(23.8070, 90.3685),
            geoToCanvas(23.7950, 90.4050),
            geoToCanvas(23.7910, 90.4180),
            geoToCanvas(23.7950, 90.4350)
        )
    )

    arterialRoads.forEach { roadPts ->
        val p = Path()
        p.moveTo(roadPts[0].x, roadPts[0].y)
        for (i in 1 until roadPts.size) {
            p.lineTo(roadPts[i].x, roadPts[i].y)
        }

        // Casing
        drawPath(
            path = p,
            color = highwayCasingColor,
            style = Stroke(width = 8f * zoom, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Highway fill
        drawPath(
            path = p,
            color = highwayFillColor,
            style = Stroke(width = 5.5f * zoom, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    // Secondary Grid Streets (Subtle white road lines)
    if (!isSat) {
        val streetColor = Color(0xFFFFFFFF)
        val streetStroke = 2.5f * zoom

        for (i in 0..6) {
            val yOffset = h * 0.15f + i * (h * 0.12f)
            drawLine(
                color = streetColor,
                start = Offset(0f, yOffset),
                end = Offset(w, yOffset),
                strokeWidth = streetStroke
            )
        }
        for (j in 0..5) {
            val xOffset = w * 0.15f + j * (w * 0.15f)
            drawLine(
                color = streetColor,
                start = Offset(xOffset, 0f),
                end = Offset(xOffset, h),
                strokeWidth = streetStroke
            )
        }
    }
}
