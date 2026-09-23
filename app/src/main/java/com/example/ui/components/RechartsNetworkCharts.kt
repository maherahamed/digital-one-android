package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import com.example.ui.theme.*
import kotlin.math.roundToInt

data class BandwidthDataPoint(
    val timeLabel: String,
    val inboundGbps: Float,
    val outboundGbps: Float,
    val timestampFormatted: String
)

data class LatencyDataPoint(
    val timeLabel: String,
    val coreLatencyMs: Float,
    val dnsLatencyMs: Float,
    val packetLossPct: Float,
    val timestampFormatted: String
)

enum class ChartTimeframe(val label: String) {
    ONE_HOUR("1H"),
    SIX_HOURS("6H"),
    TWENTY_FOUR_HOURS("24H"),
    SEVEN_DAYS("7D"),
    THIRTY_DAYS("30D")
}

/**
 * Recharts-inspired Bandwidth Usage Area & Line Chart
 * Displays real-time inbound & outbound traffic trends with interactive scrubbing tooltip
 */
@Composable
fun RechartsBandwidthChart(
    modifier: Modifier = Modifier,
    initialTimeframe: ChartTimeframe = ChartTimeframe.TWENTY_FOUR_HOURS
) {
    var selectedTimeframe by remember { mutableStateOf(initialTimeframe) }
    var scrubbedIndex by remember { mutableStateOf<Int?>(null) }
    var scrubbedX by remember { mutableFloatStateOf(0f) }

    val dataPoints = remember(selectedTimeframe) {
        generateBandwidthData(selectedTimeframe)
    }

    val maxVal = 70f // 70 Gbps max scale

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_bandwidth_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Recharts Header & Timeframe Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Bandwidth Usage Trends",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Recharts Real-Time Dual-Stream Telemetry (Inbound Rx / Outbound Tx)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Timeframe Selector
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ChartTimeframe.values().forEach { tf ->
                        val isSelected = tf == selectedTimeframe
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) BrandBlue else Color.Transparent,
                            modifier = Modifier.clickable {
                                selectedTimeframe = tf
                                scrubbedIndex = null
                            }
                        ) {
                            Text(
                                text = tf.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Recharts Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(BrandCyan))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Inbound Traffic (Rx)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(StatusPurple))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Outbound Traffic (Tx)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.weight(1f))
                val latestPoint = dataPoints.lastOrNull()
                if (latestPoint != null) {
                    Text(
                        text = "Current: ${String.format("%.1f", latestPoint.inboundGbps)} Gbps",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Canvas Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(dataPoints) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val idx = ((offset.x / size.width) * (dataPoints.size - 1))
                                        .roundToInt()
                                        .coerceIn(0, dataPoints.size - 1)
                                    scrubbedIndex = idx
                                    scrubbedX = offset.x
                                },
                                onDrag = { change, _ ->
                                    val idx = ((change.position.x / size.width) * (dataPoints.size - 1))
                                        .roundToInt()
                                        .coerceIn(0, dataPoints.size - 1)
                                    scrubbedIndex = idx
                                    scrubbedX = change.position.x
                                },
                                onDragEnd = {
                                    // Keep latest point or dismiss on delay
                                }
                            )
                        }
                        .pointerInput(dataPoints) {
                            detectTapGestures { offset ->
                                val idx = ((offset.x / size.width) * (dataPoints.size - 1))
                                    .roundToInt()
                                    .coerceIn(0, dataPoints.size - 1)
                                scrubbedIndex = if (scrubbedIndex == idx) null else idx
                                scrubbedX = offset.x
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val bottomPadding = 24f
                    val chartH = h - bottomPadding
                    val leftPadding = 32f
                    val chartW = w - leftPadding

                    // 1. Recharts Cartesian Grid (Horizontal dashed lines)
                    val gridSteps = 4
                    for (i in 0..gridSteps) {
                        val y = chartH * (1f - i.toFloat() / gridSteps)
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.35f),
                            start = Offset(leftPadding, y),
                            end = Offset(w, y),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        )
                    }

                    if (dataPoints.size >= 2) {
                        val stepX = chartW / (dataPoints.size - 1)

                        // Path 1: Inbound (Download) Area Fill & Line
                        val inboundPath = Path()
                        val inboundArea = Path()
                        inboundArea.moveTo(leftPadding, chartH)

                        val firstInboundY = chartH * (1f - (dataPoints[0].inboundGbps / maxVal).coerceIn(0f, 1f))
                        inboundPath.moveTo(leftPadding, firstInboundY)
                        inboundArea.lineTo(leftPadding, firstInboundY)

                        for (i in 1 until dataPoints.size) {
                            val x = leftPadding + i * stepX
                            val y = chartH * (1f - (dataPoints[i].inboundGbps / maxVal).coerceIn(0f, 1f))
                            inboundPath.lineTo(x, y)
                            inboundArea.lineTo(x, y)
                        }

                        inboundArea.lineTo(leftPadding + (dataPoints.size - 1) * stepX, chartH)
                        inboundArea.close()

                        // Gradient Area Fill
                        drawPath(
                            path = inboundArea,
                            brush = Brush.verticalGradient(
                                colors = listOf(BrandCyan.copy(alpha = 0.45f), BrandCyan.copy(alpha = 0.02f)),
                                startY = 0f,
                                endY = chartH
                            )
                        )

                        // Top Stroke Line
                        drawPath(
                            path = inboundPath,
                            color = BrandCyan,
                            style = Stroke(width = 2.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // Path 2: Outbound (Upload) Area Fill & Line
                        val outboundPath = Path()
                        val outboundArea = Path()
                        outboundArea.moveTo(leftPadding, chartH)

                        val firstOutboundY = chartH * (1f - (dataPoints[0].outboundGbps / maxVal).coerceIn(0f, 1f))
                        outboundPath.moveTo(leftPadding, firstOutboundY)
                        outboundArea.lineTo(leftPadding, firstOutboundY)

                        for (i in 1 until dataPoints.size) {
                            val x = leftPadding + i * stepX
                            val y = chartH * (1f - (dataPoints[i].outboundGbps / maxVal).coerceIn(0f, 1f))
                            outboundPath.lineTo(x, y)
                            outboundArea.lineTo(x, y)
                        }

                        outboundArea.lineTo(leftPadding + (dataPoints.size - 1) * stepX, chartH)
                        outboundArea.close()

                        drawPath(
                            path = outboundArea,
                            brush = Brush.verticalGradient(
                                colors = listOf(StatusPurple.copy(alpha = 0.35f), StatusPurple.copy(alpha = 0.02f)),
                                startY = 0f,
                                endY = chartH
                            )
                        )

                        drawPath(
                            path = outboundPath,
                            color = StatusPurple,
                            style = Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // 3. Draw Scrub Reference Line & Marker Dots
                        scrubbedIndex?.let { idx ->
                            val currentX = leftPadding + idx * stepX
                            val inY = chartH * (1f - (dataPoints[idx].inboundGbps / maxVal).coerceIn(0f, 1f))
                            val outY = chartH * (1f - (dataPoints[idx].outboundGbps / maxVal).coerceIn(0f, 1f))

                            // Vertical Cursor Line
                            drawLine(
                                color = BrandBlue.copy(alpha = 0.7f),
                                start = Offset(currentX, 0f),
                                end = Offset(currentX, chartH),
                                strokeWidth = 1.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                            )

                            // Dot on Inbound
                            drawCircle(color = Color.White, radius = 5f, center = Offset(currentX, inY))
                            drawCircle(color = BrandCyan, radius = 3.5f, center = Offset(currentX, inY))

                            // Dot on Outbound
                            drawCircle(color = Color.White, radius = 5f, center = Offset(currentX, outY))
                            drawCircle(color = StatusPurple, radius = 3.5f, center = Offset(currentX, outY))
                        }
                    }
                }

                // Y-Axis Labels
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxHeight()
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("60G", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("45G", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("30G", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("15G", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("0G", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // X-Axis Time Labels
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val step = (dataPoints.size / 5).coerceAtLeast(1)
                    for (i in 0 until dataPoints.size step step) {
                        Text(
                            text = dataPoints[i].timeLabel,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Recharts Floating Interactive Tooltip
                if (scrubbedIndex != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                    ) {
                        scrubbedIndex?.let { idx ->
                            val pt = dataPoints.getOrNull(idx)
                            if (pt != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                    shadowElevation = 8.dp,
                                    border = borderStroke(1.dp, BrandBlue.copy(alpha = 0.4f)),
                                    modifier = Modifier.testTag("recharts_tooltip")
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = pt.timestampFormatted,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(8.dp).background(BrandCyan, CircleShape))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Inbound (Rx): ${String.format("%.2f", pt.inboundGbps)} Gbps",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(8.dp).background(StatusPurple, CircleShape))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Outbound (Tx): ${String.format("%.2f", pt.outboundGbps)} Gbps",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val total = pt.inboundGbps + pt.outboundGbps
                                        Text(
                                            text = "Total Throughput: ${String.format("%.2f", total)} Gbps (Peak 58.4G)",
                                            fontSize = 10.sp,
                                            color = BrandBlue,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Recharts-inspired Real-time Latency & Packet Loss Trend Chart
 */
@Composable
fun RechartsLatencyTrendChart(
    modifier: Modifier = Modifier
) {
    var scrubbedIndex by remember { mutableStateOf<Int?>(null) }
    val latencyData = remember { generateLatencyData() }
    val maxLatency = 20f // 20 ms scale

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_latency_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Real-Time Latency & Jitter Trends",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "BGP Transit Core Ping + DNS Resolver + Packet Loss Rate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StatusGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "SLA 99.98% PASS",
                        color = StatusGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legends
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(StatusGreen, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Core BGP Ping (ms)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(BrandBlue, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("DNS Anycast (ms)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(StatusAmber, RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Packet Loss (%)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(latencyData) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val idx = ((offset.x / size.width) * (latencyData.size - 1))
                                        .roundToInt()
                                        .coerceIn(0, latencyData.size - 1)
                                    scrubbedIndex = idx
                                },
                                onDrag = { change, _ ->
                                    val idx = ((change.position.x / size.width) * (latencyData.size - 1))
                                        .roundToInt()
                                        .coerceIn(0, latencyData.size - 1)
                                    scrubbedIndex = idx
                                }
                            )
                        }
                        .pointerInput(latencyData) {
                            detectTapGestures { offset ->
                                val idx = ((offset.x / size.width) * (latencyData.size - 1))
                                    .roundToInt()
                                    .coerceIn(0, latencyData.size - 1)
                                scrubbedIndex = if (scrubbedIndex == idx) null else idx
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val bottomPadding = 20f
                    val chartH = h - bottomPadding
                    val leftPadding = 30f
                    val chartW = w - leftPadding

                    // Grid lines
                    for (i in 0..3) {
                        val y = chartH * (1f - i / 3f)
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            start = Offset(leftPadding, y),
                            end = Offset(w, y),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }

                    // SLA Threshold Line (12 ms)
                    val slaY = chartH * (1f - 12f / maxLatency)
                    drawLine(
                        color = StatusRed.copy(alpha = 0.5f),
                        start = Offset(leftPadding, slaY),
                        end = Offset(w, slaY),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                    )

                    if (latencyData.size >= 2) {
                        val stepX = chartW / (latencyData.size - 1)

                        // 1. Packet Loss Bars at bottom
                        latencyData.forEachIndexed { i, pt ->
                            if (pt.packetLossPct > 0f) {
                                val x = leftPadding + i * stepX
                                val barH = (pt.packetLossPct * 40f).coerceAtMost(30f)
                                drawRect(
                                    color = if (pt.packetLossPct > 0.05f) StatusRed.copy(alpha = 0.7f) else StatusAmber.copy(alpha = 0.7f),
                                    topLeft = Offset(x - 3f, chartH - barH),
                                    size = Size(6f, barH)
                                )
                            }
                        }

                        // 2. Core Latency Path (Green Line)
                        val corePath = Path()
                        val firstY = chartH * (1f - (latencyData[0].coreLatencyMs / maxLatency).coerceIn(0f, 1f))
                        corePath.moveTo(leftPadding, firstY)

                        for (i in 1 until latencyData.size) {
                            val x = leftPadding + i * stepX
                            val y = chartH * (1f - (latencyData[i].coreLatencyMs / maxLatency).coerceIn(0f, 1f))
                            corePath.lineTo(x, y)
                        }

                        drawPath(
                            path = corePath,
                            color = StatusGreen,
                            style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // 3. DNS Latency Path (Blue Line)
                        val dnsPath = Path()
                        val firstDnsY = chartH * (1f - (latencyData[0].dnsLatencyMs / maxLatency).coerceIn(0f, 1f))
                        dnsPath.moveTo(leftPadding, firstDnsY)

                        for (i in 1 until latencyData.size) {
                            val x = leftPadding + i * stepX
                            val y = chartH * (1f - (latencyData[i].dnsLatencyMs / maxLatency).coerceIn(0f, 1f))
                            dnsPath.lineTo(x, y)
                        }

                        drawPath(
                            path = dnsPath,
                            color = BrandBlue,
                            style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // Cursor
                        scrubbedIndex?.let { idx ->
                            val cx = leftPadding + idx * stepX
                            val cy = chartH * (1f - (latencyData[idx].coreLatencyMs / maxLatency).coerceIn(0f, 1f))

                            drawLine(
                                color = BrandBlue.copy(alpha = 0.7f),
                                start = Offset(cx, 0f),
                                end = Offset(cx, chartH),
                                strokeWidth = 1.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                            )
                            drawCircle(color = Color.White, radius = 5f, center = Offset(cx, cy))
                            drawCircle(color = StatusGreen, radius = 3.5f, center = Offset(cx, cy))
                        }
                    }
                }

                // Y-Axis Labels
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxHeight()
                        .padding(bottom = 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("20ms", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("12ms SLA", fontSize = 8.sp, color = StatusRed, fontWeight = FontWeight.Bold)
                    Text("6ms", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("0ms", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // X-Axis Labels
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val step = (latencyData.size / 5).coerceAtLeast(1)
                    for (i in 0 until latencyData.size step step) {
                        Text(
                            text = latencyData[i].timeLabel,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Tooltip
                if (scrubbedIndex != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 4.dp)
                    ) {
                        scrubbedIndex?.let { idx ->
                            val pt = latencyData.getOrNull(idx)
                            if (pt != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                    shadowElevation = 8.dp,
                                    border = borderStroke(1.dp, StatusGreen.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = pt.timestampFormatted,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Core Latency: ${String.format("%.2f", pt.coreLatencyMs)} ms",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusGreen
                                        )
                                        Text(
                                            text = "DNS Ping: ${String.format("%.2f", pt.dnsLatencyMs)} ms",
                                            fontSize = 10.sp,
                                            color = BrandBlue
                                        )
                                        Text(
                                            text = "Packet Loss: ${String.format("%.3f", pt.packetLossPct)}% • Jitter: 0.4 ms",
                                            fontSize = 10.sp,
                                            color = if (pt.packetLossPct > 0f) StatusAmber else StatusGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Summary telemetry bar with SLA, Core Latency, Optical Health, and Peak Bandwidth
 */
@Composable
fun RechartsOperationsSummaryBar(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Peak Throughput", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("58.4 Gbps", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandCyan)
            }
            VerticalDivider(modifier = Modifier.height(26.dp))
            Column {
                Text("Avg Core Ping", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("3.8 ms", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
            }
            VerticalDivider(modifier = Modifier.height(26.dp))
            Column {
                Text("Packet Loss", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("0.001%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
            }
            VerticalDivider(modifier = Modifier.height(26.dp))
            Column {
                Text("Network SLA", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("99.99%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
            }
        }
    }
}

// Helpers to generate realistic Recharts telemetry datasets
private fun generateBandwidthData(timeframe: ChartTimeframe): List<BandwidthDataPoint> {
    return when (timeframe) {
        ChartTimeframe.ONE_HOUR -> {
            listOf(
                BandwidthDataPoint("14:00", 34.2f, 12.1f, "14:00:00 UTC"),
                BandwidthDataPoint("14:10", 37.8f, 14.2f, "14:10:00 UTC"),
                BandwidthDataPoint("14:20", 41.5f, 15.6f, "14:20:00 UTC"),
                BandwidthDataPoint("14:30", 45.2f, 17.8f, "14:30:00 UTC"),
                BandwidthDataPoint("14:40", 49.6f, 19.1f, "14:40:00 UTC"),
                BandwidthDataPoint("14:50", 53.2f, 21.4f, "14:50:00 UTC"),
                BandwidthDataPoint("15:00", 48.7f, 18.9f, "15:00:00 UTC")
            )
        }
        ChartTimeframe.SIX_HOURS -> {
            listOf(
                BandwidthDataPoint("09:00", 22.4f, 8.5f, "09:00:00 UTC"),
                BandwidthDataPoint("10:00", 31.2f, 11.8f, "10:00:00 UTC"),
                BandwidthDataPoint("11:00", 42.0f, 16.4f, "11:00:00 UTC"),
                BandwidthDataPoint("12:00", 44.8f, 17.2f, "12:00:00 UTC"),
                BandwidthDataPoint("13:00", 39.5f, 15.1f, "13:00:00 UTC"),
                BandwidthDataPoint("14:00", 48.6f, 19.3f, "14:00:00 UTC"),
                BandwidthDataPoint("15:00", 53.2f, 21.4f, "15:00:00 UTC")
            )
        }
        ChartTimeframe.TWENTY_FOUR_HOURS -> {
            listOf(
                BandwidthDataPoint("00:00", 18.5f, 6.2f, "00:00 UTC (Night Low)"),
                BandwidthDataPoint("03:00", 12.1f, 4.1f, "03:00 UTC (Off-Peak)"),
                BandwidthDataPoint("06:00", 15.4f, 5.8f, "06:00 UTC (Morning Ramp)"),
                BandwidthDataPoint("09:00", 36.8f, 14.2f, "09:00 UTC (Corporate Peak)"),
                BandwidthDataPoint("12:00", 42.5f, 16.8f, "12:00 UTC (Midday Traffic)"),
                BandwidthDataPoint("15:00", 46.2f, 18.4f, "15:00 UTC (Afternoon High)"),
                BandwidthDataPoint("18:00", 54.8f, 22.1f, "18:00 UTC (Evening Residential Peak)"),
                BandwidthDataPoint("21:00", 58.4f, 24.5f, "21:00 UTC (Prime Streaming Peak)"),
                BandwidthDataPoint("23:59", 42.1f, 16.2f, "23:59 UTC (Late Evening)")
            )
        }
        ChartTimeframe.SEVEN_DAYS -> {
            listOf(
                BandwidthDataPoint("Mon", 52.4f, 20.8f, "Monday Peak: 52.4 Gbps"),
                BandwidthDataPoint("Tue", 54.1f, 21.6f, "Tuesday Peak: 54.1 Gbps"),
                BandwidthDataPoint("Wed", 56.8f, 22.9f, "Wednesday Peak: 56.8 Gbps"),
                BandwidthDataPoint("Thu", 55.2f, 22.1f, "Thursday Peak: 55.2 Gbps"),
                BandwidthDataPoint("Fri", 59.4f, 25.1f, "Friday Evening Peak: 59.4 Gbps"),
                BandwidthDataPoint("Sat", 62.1f, 26.8f, "Saturday CATV & Stream Peak: 62.1 Gbps"),
                BandwidthDataPoint("Sun", 60.5f, 25.4f, "Sunday Weekend Peak: 60.5 Gbps")
            )
        }
        ChartTimeframe.THIRTY_DAYS -> {
            listOf(
                BandwidthDataPoint("W1", 48.2f, 19.1f, "Week 1 Avg: 48.2 Gbps"),
                BandwidthDataPoint("W2", 51.5f, 20.4f, "Week 2 Avg: 51.5 Gbps"),
                BandwidthDataPoint("W3", 55.1f, 22.3f, "Week 3 Avg: 55.1 Gbps"),
                BandwidthDataPoint("W4", 58.4f, 24.1f, "Week 4 Avg: 58.4 Gbps")
            )
        }
    }
}

private fun generateLatencyData(): List<LatencyDataPoint> {
    return listOf(
        LatencyDataPoint("14:00", 3.2f, 6.5f, 0.000f, "14:00 UTC"),
        LatencyDataPoint("14:10", 3.5f, 7.1f, 0.000f, "14:10 UTC"),
        LatencyDataPoint("14:20", 3.8f, 6.8f, 0.000f, "14:20 UTC"),
        LatencyDataPoint("14:30", 4.9f, 8.4f, 0.005f, "14:30 UTC"),
        LatencyDataPoint("14:40", 4.1f, 7.2f, 0.000f, "14:40 UTC"),
        LatencyDataPoint("14:50", 3.6f, 6.9f, 0.000f, "14:50 UTC"),
        LatencyDataPoint("15:00", 3.8f, 7.0f, 0.000f, "15:00 UTC")
    )
}

private fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) =
    androidx.compose.foundation.BorderStroke(width, color)
