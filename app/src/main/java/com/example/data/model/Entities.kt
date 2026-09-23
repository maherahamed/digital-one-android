package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey val siteId: String,
    val code: String,
    val name: String,
    val siteType: String, // POP, CENTRAL_OFFICE, RACK_LOCATION, POLE, HUB
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val description: String = "",
    val status: String = "ACTIVE",
    val contactPerson: String = "",
    val contactPhone: String = "",
    val qrCode: String = ""
)

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val deviceId: String,
    val assetId: String,
    val name: String,
    val deviceType: String, // DeviceType name
    val vendor: String,
    val model: String,
    val serialNumber: String,
    val managementIp: String,
    val macAddress: String = "",
    val siteId: String,
    val rack: String = "Rack-01",
    val status: String = "ONLINE", // OperationalStatus
    val portCount: Int = 0,
    val installationDate: String = "2024-01-15",
    val warrantyExpiry: String = "2027-01-15",
    val notes: String = "",
    val qrCode: String = ""
)

@Entity(tableName = "switch_ports")
data class SwitchPortEntity(
    @PrimaryKey val portId: String, // e.g. SW-CORE-01-P01
    val deviceId: String,
    val portNumber: Int,
    val portName: String, // e.g. "ge-0/0/1" or "Port 1"
    val portType: String = "SFP+", // RJ45, SFP, SFP+, QSFP
    val speedMbps: Int = 10000,
    val vlanId: Int = 100,
    val isUplink: Boolean = false,
    val connectedDeviceId: String = "",
    val connectedFiberCableId: String = "",
    val connectedFiberCoreNumber: Int = 0,
    val resellerId: String = "",
    val serviceType: String = "INTERNET",
    val status: String = "UP",
    val notes: String = ""
)

@Entity(tableName = "olt_pon_ports")
data class OltPonPortEntity(
    @PrimaryKey val ponId: String, // e.g. OLT-01-PON-01
    val oltDeviceId: String,
    val ponPortNumber: Int,
    val ponType: String = "GPON", // GPON, XGS-PON, EPON
    val maxOnuCapacity: Int = 128,
    val connectedOnuCount: Int = 0,
    val connectedFiberCableId: String = "",
    val connectedFiberCoreNumber: Int = 0,
    val txPowerDbm: Double = +2.5,
    val status: String = "ACTIVE"
)

@Entity(tableName = "fiber_cables")
data class FiberCableEntity(
    @PrimaryKey val cableId: String, // e.g. FBR-BB-001
    val code: String,
    val name: String,
    val cableType: String = "ADSS Aerial", // Underground Duct, Armored Direct Burial, Aerial
    val coreCount: Int, // 2, 4, 6, 8, 12, 24, 48, 72, 96, CUSTOM
    val lengthMeters: Double,
    val startSiteId: String,
    val endSiteId: String,
    val routeName: String,
    val installationDate: String = "2024-02-01",
    val status: String = "ACTIVE",
    val owner: String = "Digital One",
    val notes: String = ""
)

@Entity(tableName = "fiber_cores")
data class FiberCoreEntity(
    @PrimaryKey val coreId: String, // e.g. FBR-BB-001-C01
    val cableId: String,
    val coreNumber: Int,
    val colorName: String, // Blue, Orange, Green, Brown, Slate, White, Red, Black, Yellow, Violet, Rose, Aqua
    val colorHex: String, // e.g. "#0284C7"
    val status: String, // CoreStatus: ACTIVE, SPARE, RESERVED, FAULT, DISCONNECTED
    val serviceType: String, // ServiceType: INTERNET, CATV, CCTV, VOICE, OTHER, SPARE
    val connectedDeviceId: String = "",
    val connectedPort: String = "",
    val assignedCustomerId: String = "",
    val assignedResellerId: String = "",
    val destinationFdbId: String = "",
    val notes: String = ""
)

@Entity(tableName = "splitters")
data class SplitterEntity(
    @PrimaryKey val splitterId: String, // e.g. SPL-01
    val name: String,
    val ratio: String, // 1:2, 1:4, 1:8, 1:16, 1:32, 1:64, CUSTOM
    val lossMode: String = "AUTO", // AUTO, CUSTOM, MEASURED
    val calculatedLossDb: Double,
    val customLossDb: Double = 0.0,
    val measuredLossDb: Double = 0.0,
    val locationSiteId: String,
    val parentSplitterId: String = "", // for cascaded splitters (e.g. 1:4 -> 1:8)
    val inputCableId: String = "",
    val inputCoreNumber: Int = 0,
    val status: String = "ACTIVE"
)

@Entity(tableName = "fdbs")
data class FdbEntity(
    @PrimaryKey val fdbId: String, // e.g. FDB-NORTH-01
    val code: String,
    val name: String,
    val locationSiteId: String,
    val latitude: Double,
    val longitude: Double,
    val inputFiberCableId: String,
    val inputFiberCoreNumber: Int,
    val splitterId: String,
    val capacityPorts: Int = 16,
    val usedPorts: Int = 0,
    val sparePorts: Int = 16,
    val status: String = "ACTIVE", // ACTIVE, FAULT, FULL
    val qrCode: String = "",
    val notes: String = ""
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val customerId: String, // e.g. CUST-1001
    val name: String,
    val phone: String,
    val email: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val customerType: String = "Residential", // Residential, Enterprise, SME, ResellerClient
    val status: String = "ACTIVE", // ACTIVE, SUSPENDED, PROVISIONING
    val registeredDate: String = "2024-03-01",
    val notes: String = ""
)

@Entity(tableName = "customer_services")
data class CustomerServiceEntity(
    @PrimaryKey val serviceId: String, // e.g. SVC-1001-INT, SVC-1001-CATV
    val customerId: String,
    val serviceType: String, // INTERNET, CATV, CCTV, VOICE
    val planName: String, // e.g. "Ultra Fiber 100Mbps", "Digital HD CATV 120 Ch"
    val onuDeviceId: String = "",
    val onuSerialNumber: String = "",
    val fdbId: String = "",
    val fdbPortNumber: Int = 0,
    val fiberCableId: String = "",
    val fiberCoreNumber: Int = 0,
    val splitterId: String = "",
    val oltDeviceId: String = "",
    val ponId: String = "",
    val ipAddress: String = "",
    val vlanId: Int = 100,
    val expectedRxPowerDbm: Double = -19.5,
    val measuredRxPowerDbm: Double = -20.1,
    val opticalStatus: String = "HEALTHY", // HEALTHY, WARNING, CRITICAL
    val rfLevelDbMv: Double = 12.0, // for CATV
    val status: String = "ACTIVE"
)

@Entity(tableName = "resellers")
data class ResellerEntity(
    @PrimaryKey val resellerId: String,
    val companyName: String,
    val contactName: String,
    val phone: String,
    val location: String,
    val allocatedBandwidthMbps: Int,
    val vlanId: Int,
    val subnetCidr: String,
    val connectedSwitchPortId: String,
    val assignedFiberCableId: String,
    val assignedFiberCoreNumber: Int,
    val activeClientsCount: Int = 0,
    val status: String = "ACTIVE"
)

@Entity(tableName = "faults")
data class FaultEntity(
    @PrimaryKey val faultId: String, // e.g. FLT-2026-001
    val faultType: String, // FaultType: FIBER_CUT, DEVICE_DOWN, etc.
    val severity: String, // FaultSeverity: CRITICAL, HIGH, MEDIUM, LOW
    val title: String,
    val description: String,
    val startTime: String,
    val detectionTime: String,
    val locationSiteId: String = "",
    val deviceId: String = "",
    val fiberCableId: String = "",
    val fiberCoreNumber: Int = 0,
    val serviceType: String = "INTERNET",
    val suspectedCause: String = "Physical fiber cut or core splice failure",
    val rootCauseFaultId: String = "", // For dependency correlation: points to root fault
    val isRootCause: Boolean = true,
    val affectedCustomersCount: Int = 0,
    val affectedPopName: String = "",
    val assignedTechnicianId: String = "",
    val resolutionNotes: String = "",
    val status: String = "OPEN" // FaultStatus: OPEN, INVESTIGATING, IN_PROGRESS, RESOLVED
)

@Entity(tableName = "work_orders")
data class WorkOrderEntity(
    @PrimaryKey val workOrderId: String, // e.g. WO-8801
    val title: String,
    val faultId: String = "",
    val customerId: String = "",
    val assetId: String = "",
    val assignedTechnicianId: String,
    val technicianName: String,
    val status: String = "ASSIGNED", // WorkOrderStatus
    val priority: String = "HIGH",
    val siteAddress: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val scheduledTime: String = "Today",
    val completionTime: String = "",
    val partsUsed: String = "",
    val resolutionNotes: String = "",
    val offlinePendingSync: Boolean = false
)

@Entity(tableName = "technicians")
data class TechnicianEntity(
    @PrimaryKey val technicianId: String,
    val name: String,
    val phone: String,
    val team: String, // North Fiber Team, South Splicing Squad, CATV Headend
    val currentStatus: String = "AVAILABLE", // AVAILABLE, ON_SITE, BUSY, OFF_DUTY
    val currentLat: Double = 23.8103,
    val currentLng: Double = 90.4125
)

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey val itemId: String,
    val assetTag: String,
    val category: String, // OLT, ONU, SFP, FIBER, SPLITTER, FDB, CATV_EDFA, CATV_TAP, TOOLS
    val itemName: String,
    val model: String,
    val serialNumber: String = "",
    val quantity: Int = 1,
    val unit: String = "pcs",
    val lifecycle: String = "STOCK", // PROCURED, STOCK, RESERVED, INSTALLED, ACTIVE, FAULT, REPAIR
    val location: String = "Central Warehouse",
    val assignedToSiteOrCust: String = "",
    val qrCode: String = ""
)

@Entity(tableName = "ip_subnets")
data class IpSubnetEntity(
    @PrimaryKey val subnetId: String,
    val subnetCidr: String, // e.g. 103.145.72.0/24 or 10.200.0.0/22
    val ipType: String = "PUBLIC", // PUBLIC, PRIVATE, MANAGEMENT, RESELLER, CUSTOMER_POOL
    val gateway: String,
    val vlanId: Int,
    val totalIps: Int,
    val usedIps: Int,
    val reservedIps: Int,
    val description: String = ""
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val logId: Long = 0,
    val user: String = "Admin",
    val action: String, // e.g. "UPDATE_FIBER_CORE", "DISPATCH_WORK_ORDER", "CREATE_SPLITTER"
    val entityType: String,
    val entityId: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
