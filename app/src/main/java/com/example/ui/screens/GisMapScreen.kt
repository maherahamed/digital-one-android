package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.IspViewModel
import com.example.ui.components.GoogleMapsNetworkView
import com.example.ui.components.MapAssetMarker
import com.example.ui.theme.*

@Composable
fun GisMapScreen(
    viewModel: IspViewModel
) {
    val sites by viewModel.sites.collectAsState()
    val cables by viewModel.cables.collectAsState()
    var selectedAsset by remember { mutableStateOf<MapAssetMarker?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("gis_map_screen")
    ) {
        // Top Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Geographic Network Twin",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Google Maps Infrastructure Distribution (POPs, Poles, FDBs & Cable Routes)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BrandBlue.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "MAPS API V3",
                        color = BrandBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Google Maps Component
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            GoogleMapsNetworkView(
                modifier = Modifier.fillMaxSize(),
                sites = sites,
                cables = cables,
                isCompact = false,
                onAssetClick = { asset ->
                    selectedAsset = asset
                }
            )
        }

        // Bottom Telemetry Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedAsset != null) "Selected: ${selectedAsset?.title}" else "Tap any pin or cable route to inspect optical link budgets & live status",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selectedAsset != null) BrandBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selectedAsset != null) FontWeight.Bold else FontWeight.Normal
                )

                if (selectedAsset != null) {
                    TextButton(
                        onClick = { selectedAsset = null },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Clear Selection", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
