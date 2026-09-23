package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.model.InventoryItemEntity
import com.example.ui.IspViewModel
import com.example.ui.components.QrAssetDialog
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.*

@Composable
fun InventoryQrScreen(
    viewModel: IspViewModel
) {
    val inventory by viewModel.inventory.collectAsState()
    var selectedItemForQr by remember { mutableStateOf<InventoryItemEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("inventory_qr_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Hardware Inventory & Asset Logistics (${inventory.size})",
                subtitle = "ONUs, splitters, cable drums, fusion splicers with QR identity"
            )
        }

        items(inventory) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inv_item_${item.itemId}"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .padding(14.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = item.itemName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            StatusPill(status = item.lifecycle)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Model: ${item.model} • Tag: ${item.assetTag}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = BrandBlue
                        )
                        Text(
                            text = "Stock: ${item.quantity} ${item.unit} • Location: ${item.location}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { selectedItemForQr = item },
                        modifier = Modifier.testTag("btn_qr_${item.itemId}")
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = "QR Code", tint = BrandBlue, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }

    if (selectedItemForQr != null) {
        val item = selectedItemForQr!!
        QrAssetDialog(
            assetTitle = item.itemName,
            assetCode = item.qrCode.ifEmpty { "D1-QR-${item.assetTag}" },
            details = "Asset Tag: ${item.assetTag} | S/N: ${item.serialNumber}\nModel: ${item.model} | Location: ${item.location}",
            onDismiss = { selectedItemForQr = null }
        )
    }
}
