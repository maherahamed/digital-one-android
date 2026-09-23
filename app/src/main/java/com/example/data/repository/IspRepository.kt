package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class IspRepository(private val db: AppDatabase) {

    // DAOs
    val siteDao = db.siteDao()
    val deviceDao = db.deviceDao()
    val switchPortDao = db.switchPortDao()
    val oltPonPortDao = db.oltPonPortDao()
    val fiberDao = db.fiberDao()
    val splitterDao = db.splitterDao()
    val fdbDao = db.fdbDao()
    val customerDao = db.customerDao()
    val resellerDao = db.resellerDao()
    val faultDao = db.faultDao()
    val fieldOpsDao = db.fieldOpsDao()
    val inventoryDao = db.inventoryDao()
    val ipamDao = db.ipamDao()
    val auditDao = db.auditDao()

    // Observable flows
    val allSites: Flow<List<SiteEntity>> = siteDao.getAllSites()
    val allDevices: Flow<List<DeviceEntity>> = deviceDao.getAllDevices()
    val allCables: Flow<List<FiberCableEntity>> = fiberDao.getAllCables()
    val allCores: Flow<List<FiberCoreEntity>> = fiberDao.getAllCores()
    val allFdbs: Flow<List<FdbEntity>> = fdbDao.getAllFdbs()
    val allSplitters: Flow<List<SplitterEntity>> = splitterDao.getAllSplitters()
    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()
    val allCustomerServices: Flow<List<CustomerServiceEntity>> = customerDao.getAllCustomerServices()
    val allResellers: Flow<List<ResellerEntity>> = resellerDao.getAllResellers()
    val allFaults: Flow<List<FaultEntity>> = faultDao.getAllFaults()
    val activeFaults: Flow<List<FaultEntity>> = faultDao.getActiveFaults()
    val allWorkOrders: Flow<List<WorkOrderEntity>> = fieldOpsDao.getAllWorkOrders()
    val allTechnicians: Flow<List<TechnicianEntity>> = fieldOpsDao.getAllTechnicians()
    val allInventory: Flow<List<InventoryItemEntity>> = inventoryDao.getAllInventory()
    val allSubnets: Flow<List<IpSubnetEntity>> = ipamDao.getAllSubnets()
    val recentAuditLogs: Flow<List<AuditLogEntity>> = auditDao.getRecentAuditLogs()

    // Dashboard metrics
    val oltCount = deviceDao.getOltCount()
    val routerCount = deviceDao.getRouterCount()
    val switchCount = deviceDao.getSwitchCount()
    val cableCount = fiberDao.getCableCount()
    val totalKm = fiberDao.getTotalLengthMeters()
    val totalCores = fiberDao.getTotalCoresCount()
    val activeCores = fiberDao.getActiveCoresCount()
    val spareCores = fiberDao.getSpareCoresCount()
    val internetCustCount = customerDao.getInternetCustomerCount()
    val catvCustCount = customerDao.getCatvCustomerCount()
    val resellerCount = resellerDao.getResellerCount()
    val fdbCount = fdbDao.getFdbCount()
    val splitterCount = splitterDao.getSplitterCount()
    val activeFaultCount = faultDao.getActiveFaultCount()
    val criticalFaultCount = faultDao.getCriticalFaultCount()

    suspend fun checkAndSeedInitialData() = withContext(Dispatchers.IO) {
        val existingSites = siteDao.getAllSites().first()
        if (existingSites.isNotEmpty()) return@withContext

        // 1. Seed Sites (Digital One Network Locations)
        val sites = listOf(
            SiteEntity(
                siteId = "SITE-HQ",
                code = "D1-HQ",
                name = "Digital One Central NOC & Headend",
                siteType = "CENTRAL_OFFICE",
                address = "Level 7, Cyber Tower, Gulshan Ave, Dhaka",
                latitude = 23.7925,
                longitude = 90.4078,
                description = "Primary Core Datacenter, BGP Gateway & CATV Satellite Uplink Headend",
                status = "ACTIVE",
                contactPerson = "Farhan Rahman (NOC Head)",
                contactPhone = "+880-1711-000001",
                qrCode = "D1-LOC-HQ-001"
            ),
            SiteEntity(
                siteId = "SITE-POP-N",
                code = "POP-NORTH",
                name = "North Sector Hub POP",
                siteType = "POP",
                address = "Plot 14, Sector 7, Uttara, Dhaka",
                latitude = 23.8722,
                longitude = 90.3980,
                description = "Distribution Hub with MA5800 OLT and EDFA Optical Repeaters",
                status = "ACTIVE",
                contactPerson = "Kamrul Islam (Hub Supervisor)",
                contactPhone = "+880-1711-000002",
                qrCode = "D1-LOC-POPN-002"
            ),
            SiteEntity(
                siteId = "SITE-POP-E",
                code = "POP-EAST",
                name = "East Tech Park POP",
                siteType = "POP",
                address = "Block C, Bashundhara R/A, Dhaka",
                latitude = 23.8155,
                longitude = 90.4352,
                description = "Distribution POP serving Enterprise and Reseller clusters",
                status = "ACTIVE",
                contactPerson = "Nusrat Jahan (Zone Engineer)",
                contactPhone = "+880-1711-000003",
                qrCode = "D1-LOC-POPE-003"
            ),
            SiteEntity(
                siteId = "SITE-POP-S",
                code = "POP-SOUTH",
                name = "South Bay Residential POP",
                siteType = "POP",
                address = "Road 27, Dhanmondi, Dhaka",
                latitude = 23.7461,
                longitude = 90.3742,
                description = "High-density GPON & CATV distribution shelter",
                status = "ACTIVE",
                contactPerson = "Tanvir Hasan",
                contactPhone = "+880-1711-000004",
                qrCode = "D1-LOC-POPS-004"
            ),
            SiteEntity(
                siteId = "SITE-POLE-101",
                code = "POLE-101",
                name = "DPDC Utility Pole #101",
                siteType = "POLE",
                address = "Corner of Lake Road & Ave 3, Uttara",
                latitude = 23.8650,
                longitude = 90.4010,
                description = "Aerial Splice Closure Joint #01 (FBR-DIST-N1 / N2)",
                status = "ACTIVE",
                qrCode = "D1-LOC-PL101"
            ),
            SiteEntity(
                siteId = "SITE-POLE-102",
                code = "POLE-102",
                name = "DPDC Utility Pole #102",
                siteType = "POLE",
                address = "Gausul Azam Ave, Sector 13",
                latitude = 23.8760,
                longitude = 90.3920,
                description = "Mounting for FDB-NORTH-01",
                status = "ACTIVE",
                qrCode = "D1-LOC-PL102"
            )
        )
        siteDao.insertSites(sites)

        // 2. Seed Network Devices (Core Routers, Switches, OLTs, CATV Headend)
        val devices = listOf(
            DeviceEntity(
                deviceId = "DEV-CR-01",
                assetId = "D1-RTR-001",
                name = "D1-CORE-RTR-01",
                deviceType = "CORE_ROUTER",
                vendor = "Cisco",
                model = "ASR 9006",
                serialNumber = "FOX24180A91",
                managementIp = "10.200.0.1",
                macAddress = "00:1E:13:88:41:01",
                siteId = "SITE-HQ",
                rack = "Rack-A01",
                status = "ONLINE",
                portCount = 16,
                notes = "Dual 100G Upstream to Lumen (AS3356) and Cogent (AS174)",
                qrCode = "D1-QR-CR01"
            ),
            DeviceEntity(
                deviceId = "DEV-CR-02",
                assetId = "D1-RTR-002",
                name = "D1-BACKUP-RTR-02",
                deviceType = "BACKUP_ROUTER",
                vendor = "Juniper",
                model = "MX204",
                serialNumber = "JN12440938",
                managementIp = "10.200.0.2",
                macAddress = "00:1E:13:88:41:02",
                siteId = "SITE-HQ",
                rack = "Rack-A01",
                status = "ONLINE",
                portCount = 8,
                notes = "VRRP Backup Core Gateway",
                qrCode = "D1-QR-CR02"
            ),
            DeviceEntity(
                deviceId = "DEV-SW-DIST-01",
                assetId = "D1-SW-001",
                name = "D1-SW-DIST-HQ-01",
                deviceType = "DISTRIBUTION_SWITCH",
                vendor = "Cisco",
                model = "Nexus 93180YC-FX",
                serialNumber = "FOC2241R901",
                managementIp = "10.200.0.10",
                macAddress = "00:1E:13:99:50:10",
                siteId = "SITE-HQ",
                rack = "Rack-A02",
                status = "ONLINE",
                portCount = 48,
                notes = "Aggregates POP links and Reseller wholesale VLANs",
                qrCode = "D1-QR-SW01"
            ),
            DeviceEntity(
                deviceId = "DEV-OLT-01",
                assetId = "D1-OLT-001",
                name = "D1-OLT-NORTH-MA5800",
                deviceType = "OLT",
                vendor = "Huawei",
                model = "SmartAX MA5800-X17",
                serialNumber = "2102350YTN10001",
                managementIp = "10.200.1.1",
                macAddress = "48:8E:EF:01:22:01",
                siteId = "SITE-POP-N",
                rack = "Rack-N01",
                status = "ONLINE",
                portCount = 16,
                notes = "16-Port GPON Board (GPUF) with Class C+ SFP modules (+3.0 dBm)",
                qrCode = "D1-QR-OLT01"
            ),
            DeviceEntity(
                deviceId = "DEV-OLT-02",
                assetId = "D1-OLT-002",
                name = "D1-OLT-SOUTH-C320",
                deviceType = "OLT",
                vendor = "ZTE",
                model = "ZXA10 C320",
                serialNumber = "ZTEGC3200941",
                managementIp = "10.200.2.1",
                macAddress = "34:E0:CF:88:11:02",
                siteId = "SITE-POP-S",
                rack = "Rack-S01",
                status = "ONLINE",
                portCount = 16,
                notes = "GTGH 16-Port Board serving residential South Bay",
                qrCode = "D1-QR-OLT02"
            ),
            DeviceEntity(
                deviceId = "DEV-CATV-DISH-01",
                assetId = "D1-CATV-001",
                name = "CATV Satellite Dish C-Band 3.8m",
                deviceType = "SATELLITE_DISH",
                vendor = "Prodelin",
                model = "Series 1383 3.8m",
                serialNumber = "PD380-4921",
                managementIp = "N/A",
                siteId = "SITE-HQ",
                rack = "Rooftop Mast",
                status = "ONLINE",
                portCount = 4,
                notes = "Receives Bangabandhu-1 and Apstar-7 downlink signals",
                qrCode = "D1-QR-DISH01"
            ),
            DeviceEntity(
                deviceId = "DEV-CATV-IRD-01",
                assetId = "D1-CATV-002",
                name = "Harmonic ProView 8100 IRD Receiver",
                deviceType = "RECEIVER_IRD",
                vendor = "Harmonic",
                model = "ProView 8100",
                serialNumber = "HMC8100-291",
                managementIp = "10.200.0.50",
                siteId = "SITE-HQ",
                rack = "Rack-CATV-01",
                status = "ONLINE",
                portCount = 8,
                notes = "Decodes 120 digital HD RF channels to IP/ASI multicast stream",
                qrCode = "D1-QR-IRD01"
            ),
            DeviceEntity(
                deviceId = "DEV-CATV-TX-01",
                assetId = "D1-CATV-003",
                name = "1550nm Optical Transmitter 10dBm",
                deviceType = "OPTICAL_TRANSMITTER",
                vendor = "Finisar / Cisco",
                model = "Prisma II 1550nm TX",
                serialNumber = "TX1550-9831",
                managementIp = "10.200.0.51",
                siteId = "SITE-HQ",
                rack = "Rack-CATV-01",
                status = "ONLINE",
                portCount = 2,
                notes = "Transmits CATV broadcast signals over dedicated 1550nm fiber cores (+10 dBm)",
                qrCode = "D1-QR-TX01"
            ),
            DeviceEntity(
                deviceId = "DEV-CATV-EDFA-01",
                assetId = "D1-CATV-004",
                name = "High-Power Multi-Port EDFA 22dBm",
                deviceType = "EDFA_AMPLIFIER",
                vendor = "Accelink",
                model = "EYDFA-16P-22dBm",
                serialNumber = "EDFA22-491",
                managementIp = "10.200.1.52",
                siteId = "SITE-POP-N",
                rack = "Rack-N02",
                status = "ONLINE",
                portCount = 16,
                notes = "Optical amplification for North Sector CATV distribution",
                qrCode = "D1-QR-EDFA01"
            )
        )
        deviceDao.insertDevices(devices)

        // 3. Switch Ports
        val switchPorts = listOf(
            SwitchPortEntity("SW-HQ-P01", "DEV-SW-DIST-01", 1, "xe-0/0/1", "SFP+", 10000, 10, true, "DEV-CR-01", status = "UP", notes = "Primary Core Router Trunk"),
            SwitchPortEntity("SW-HQ-P02", "DEV-SW-DIST-01", 2, "xe-0/0/2", "SFP+", 10000, 10, true, "DEV-CR-02", status = "UP", notes = "Backup Core Router Trunk"),
            SwitchPortEntity("SW-HQ-P03", "DEV-SW-DIST-01", 3, "xe-0/0/3", "SFP+", 10000, 100, false, "DEV-OLT-01", "FBR-BB-01", 1, status = "UP", notes = "North POP OLT Uplink (Core 1)"),
            SwitchPortEntity("SW-HQ-P04", "DEV-SW-DIST-01", 4, "xe-0/0/4", "SFP+", 10000, 200, false, "DEV-OLT-02", "FBR-BB-02", 1, status = "UP", notes = "South POP OLT Uplink (Core 1)"),
            SwitchPortEntity("SW-HQ-P05", "DEV-SW-DIST-01", 5, "ge-0/0/5", "SFP", 1000, 300, false, resellerId = "RES-01", serviceType = "INTERNET", status = "UP", notes = "Metro NetLink Reseller Handover"),
            SwitchPortEntity("SW-HQ-P06", "DEV-SW-DIST-01", 6, "ge-0/0/6", "SFP", 1000, 301, false, resellerId = "RES-02", serviceType = "INTERNET", status = "UP", notes = "FastWave Wholesale Handover")
        )
        switchPortDao.insertSwitchPorts(switchPorts)

        // 4. OLT PON Ports
        val ponPorts = listOf(
            OltPonPortEntity("OLT-01-PON-01", "DEV-OLT-01", 1, "GPON", 128, 48, "FBR-DIST-N1", 1, +2.8, "ACTIVE"),
            OltPonPortEntity("OLT-01-PON-02", "DEV-OLT-01", 2, "GPON", 128, 36, "FBR-DIST-N1", 2, +2.6, "ACTIVE"),
            OltPonPortEntity("OLT-01-PON-03", "DEV-OLT-01", 3, "GPON", 128, 22, "FBR-DIST-N1", 3, +2.5, "ACTIVE"),
            OltPonPortEntity("OLT-01-PON-04", "DEV-OLT-01", 4, "GPON", 128, 0, "", 0, +2.7, "ACTIVE"),
            OltPonPortEntity("OLT-02-PON-01", "DEV-OLT-02", 1, "GPON", 128, 52, "FBR-BB-02", 5, +2.4, "ACTIVE")
        )
        oltPonPortDao.insertPonPorts(ponPorts)

        // 5. Physical Fiber Cables (Multi-Core Physical Model)
        val cables = listOf(
            FiberCableEntity(
                cableId = "FBR-BB-01",
                code = "FBR-48-HQ-NORTH",
                name = "Backbone Fiber Cable 01 (HQ -> North POP)",
                cableType = "48-Core ADSS Aerial",
                coreCount = 48,
                lengthMeters = 5200.0,
                startSiteId = "SITE-HQ",
                endSiteId = "SITE-POP-N",
                routeName = "Gulshan - Airport Highway Backbone",
                status = "ACTIVE",
                notes = "Carries 10G IP Uplinks, CATV 1550nm Broadcast & Enterprise dark fibers"
            ),
            FiberCableEntity(
                cableId = "FBR-BB-02",
                code = "FBR-24-HQ-SOUTH",
                name = "Backbone Fiber Cable 02 (HQ -> South Bay POP)",
                cableType = "24-Core Armored Duct",
                coreCount = 24,
                lengthMeters = 7800.0,
                startSiteId = "SITE-HQ",
                endSiteId = "SITE-POP-S",
                routeName = "Mirpur Road Underground Duct Route",
                status = "ACTIVE",
                notes = "High-protection armored cable"
            ),
            FiberCableEntity(
                cableId = "FBR-DIST-N1",
                code = "FBR-12-NORTH-PL101",
                name = "Distribution Cable N1 (North POP -> Pole 101)",
                cableType = "12-Core Figure-8 Aerial",
                coreCount = 12,
                lengthMeters = 1850.0,
                startSiteId = "SITE-POP-N",
                endSiteId = "SITE-POLE-101",
                routeName = "Sector 7 Feeder Route",
                status = "ACTIVE",
                notes = "Feed to Splice Closure Joint #01"
            ),
            FiberCableEntity(
                cableId = "FBR-DIST-N2",
                code = "FBR-08-PL101-FDB01",
                name = "Access Cable N2 (Pole 101 -> FDB-NORTH-01)",
                cableType = "8-Core Drop ADSS",
                coreCount = 8,
                lengthMeters = 640.0,
                startSiteId = "SITE-POLE-101",
                endSiteId = "SITE-POLE-102",
                routeName = "Lake View Access Feeder",
                status = "ACTIVE",
                notes = "Last-mile feeder to Customer FDB"
            )
        )
        fiberDao.insertCables(cables)

        // 6. Individual Fiber Cores (TIA-598 Color Standard)
        // Standard color palette:
        val colorNames = listOf(
            "Blue", "Orange", "Green", "Brown", "Slate", "White",
            "Red", "Black", "Yellow", "Violet", "Rose", "Aqua"
        )
        val colorHexes = listOf(
            "#0284C7", "#EA580C", "#16A34A", "#854D0E", "#64748B", "#F8FAFC",
            "#DC2626", "#1E293B", "#CA8A04", "#7C3AED", "#E11D48", "#0891B2"
        )

        val cores = mutableListOf<FiberCoreEntity>()
        // Cores for FBR-BB-01 (48 Cores)
        for (i in 1..48) {
            val colorIdx = (i - 1) % 12
            val service = when {
                i in 1..4 -> "INTERNET"
                i in 5..8 -> "CATV"
                i in 9..10 -> "CCTV"
                i in 11..12 -> "VOICE"
                i in 13..24 -> "SPARE"
                i in 25..36 -> "RESERVED"
                else -> "SPARE"
            }
            val status = when {
                i in 1..12 -> "ACTIVE"
                i in 13..24 -> "SPARE"
                i in 25..36 -> "RESERVED"
                else -> "SPARE"
            }
            cores.add(
                FiberCoreEntity(
                    coreId = "FBR-BB-01-C%02d".format(i),
                    cableId = "FBR-BB-01",
                    coreNumber = i,
                    colorName = colorNames[colorIdx],
                    colorHex = colorHexes[colorIdx],
                    status = status,
                    serviceType = service,
                    connectedDeviceId = if (i == 1) "DEV-SW-DIST-01" else if (i == 5) "DEV-CATV-TX-01" else "",
                    connectedPort = if (i == 1) "xe-0/0/3" else if (i == 5) "OPT-OUT-1" else "",
                    destinationFdbId = "",
                    notes = if (i == 1) "Primary North POP 10G IP Uplink" else if (i == 5) "1550nm CATV Optical Main Carrier" else "Standard core"
                )
            )
        }

        // Cores for FBR-DIST-N1 (12 Cores)
        for (i in 1..12) {
            val colorIdx = (i - 1) % 12
            val service = when (i) {
                1 -> "INTERNET"
                2 -> "INTERNET"
                3 -> "CATV"
                4 -> "CCTV"
                5 -> "SPARE"
                6 -> "SPARE"
                else -> "RESERVED"
            }
            val status = if (i <= 4) "ACTIVE" else if (i <= 6) "SPARE" else "RESERVED"
            cores.add(
                FiberCoreEntity(
                    coreId = "FBR-DIST-N1-C%02d".format(i),
                    cableId = "FBR-DIST-N1",
                    coreNumber = i,
                    colorName = colorNames[colorIdx],
                    colorHex = colorHexes[colorIdx],
                    status = status,
                    serviceType = service,
                    connectedDeviceId = if (i == 1) "DEV-OLT-01" else if (i == 3) "DEV-CATV-EDFA-01" else "",
                    connectedPort = if (i == 1) "PON-01" else if (i == 3) "EDFA-CH1" else "",
                    notes = if (i == 1) "GPON-01 feeder" else if (i == 3) "CATV RF sub-feed" else ""
                )
            )
        }

        // Cores for FBR-DIST-N2 (8 Cores)
        for (i in 1..8) {
            val colorIdx = (i - 1) % 12
            val service = when (i) {
                1 -> "INTERNET"
                2 -> "CATV"
                3 -> "SPARE"
                else -> "RESERVED"
            }
            val status = if (i <= 2) "ACTIVE" else if (i == 3) "SPARE" else "RESERVED"
            cores.add(
                FiberCoreEntity(
                    coreId = "FBR-DIST-N2-C%02d".format(i),
                    cableId = "FBR-DIST-N2",
                    coreNumber = i,
                    colorName = colorNames[colorIdx],
                    colorHex = colorHexes[colorIdx],
                    status = status,
                    serviceType = service,
                    destinationFdbId = if (i == 1) "FDB-NORTH-01" else "",
                    notes = if (i == 1) "FDB-NORTH-01 Splitter Input" else if (i == 2) "CATV Tap Feed" else ""
                )
            )
        }
        fiberDao.insertCores(cores)

        // 7. Splitters
        val splitters = listOf(
            SplitterEntity(
                splitterId = "SPL-PRI-01",
                name = "Pole-101 1:4 Primary Cassette",
                ratio = "1:4",
                lossMode = "AUTO",
                calculatedLossDb = 7.2,
                customLossDb = 7.2,
                measuredLossDb = 7.35,
                locationSiteId = "SITE-POLE-101",
                parentSplitterId = "",
                inputCableId = "FBR-DIST-N1",
                inputCoreNumber = 1,
                status = "ACTIVE"
            ),
            SplitterEntity(
                splitterId = "SPL-SEC-01",
                name = "FDB-N01 1:8 Secondary PLC Splitter",
                ratio = "1:8",
                lossMode = "AUTO",
                calculatedLossDb = 10.5,
                customLossDb = 10.5,
                measuredLossDb = 10.62,
                locationSiteId = "SITE-POLE-102",
                parentSplitterId = "SPL-PRI-01",
                inputCableId = "FBR-DIST-N2",
                inputCoreNumber = 1,
                status = "ACTIVE"
            ),
            SplitterEntity(
                splitterId = "SPL-CATV-TAP-01",
                name = "CATV 4-Way Tap 14dB",
                ratio = "1:4",
                lossMode = "CUSTOM",
                calculatedLossDb = 14.0,
                customLossDb = 14.0,
                measuredLossDb = 14.2,
                locationSiteId = "SITE-POLE-102",
                inputCableId = "FBR-DIST-N2",
                inputCoreNumber = 2,
                status = "ACTIVE"
            )
        )
        splitterDao.insertSplitters(splitters)

        // 8. FDB / FAT (Fiber Distribution Box)
        val fdbs = listOf(
            FdbEntity(
                fdbId = "FDB-NORTH-01",
                code = "FAT-N-01",
                name = "FDB Lake View 16-Port FAT",
                locationSiteId = "SITE-POLE-102",
                latitude = 23.8760,
                longitude = 90.3920,
                inputFiberCableId = "FBR-DIST-N2",
                inputFiberCoreNumber = 1,
                splitterId = "SPL-SEC-01",
                capacityPorts = 16,
                usedPorts = 7,
                sparePorts = 9,
                status = "ACTIVE",
                qrCode = "D1-QR-FDB-N01",
                notes = "Waterproof IP68 outdoor FAT enclosure with 1:8 PLC splitter"
            ),
            FdbEntity(
                fdbId = "FDB-NORTH-02",
                code = "FAT-N-02",
                name = "FDB Sector 13 Plaza FAT",
                locationSiteId = "SITE-POLE-101",
                latitude = 23.8655,
                longitude = 90.4015,
                inputFiberCableId = "FBR-DIST-N1",
                inputFiberCoreNumber = 2,
                splitterId = "SPL-PRI-01",
                capacityPorts = 16,
                usedPorts = 12,
                sparePorts = 4,
                status = "ACTIVE",
                qrCode = "D1-QR-FDB-N02",
                notes = "Serves Sector 13 commercial buildings"
            )
        )
        fdbDao.insertFdbs(fdbs)

        // 9. Customers (Single customer entity with MULTIPLE services: Internet + CATV)
        val customers = listOf(
            CustomerEntity(
                customerId = "CUST-1001",
                name = "Ahmed Enterprise Corp",
                phone = "+880-1819-112233",
                email = "it@ahmedcorp.com",
                address = "House 12, Lake View Road, Sector 7, Uttara",
                latitude = 23.8755,
                longitude = 90.3925,
                customerType = "Enterprise",
                status = "ACTIVE",
                registeredDate = "2024-01-20",
                notes = "Dual Service: 200Mbps Dedicated Internet + Premium HD CATV"
            ),
            CustomerEntity(
                customerId = "CUST-1002",
                name = "Dr. Sarah Jenkins",
                phone = "+880-1712-445566",
                email = "sarah.jenkins@hospital.org",
                address = "Apt 4B, Green Villa, Road 14, Sector 7",
                latitude = 23.8768,
                longitude = 90.3918,
                customerType = "Residential",
                status = "ACTIVE",
                registeredDate = "2024-02-10",
                notes = "Dual Service: 60Mbps Home Fiber + 120-Channel CATV"
            ),
            CustomerEntity(
                customerId = "CUST-1003",
                name = "TechHub Coworking Space",
                phone = "+880-1911-778899",
                email = "operations@techhub.bd",
                address = "Level 3, Sector 13 Commercial Plaza",
                latitude = 23.8660,
                longitude = 90.4020,
                customerType = "SME",
                status = "ACTIVE",
                registeredDate = "2024-03-05",
                notes = "500Mbps Dedicated Bandwidth with Static Public IP"
            ),
            CustomerEntity(
                customerId = "CUST-1004",
                name = "Skyline Heights Apartments (Bulk)",
                phone = "+880-1610-998877",
                email = "secretary@skylineheights.org",
                address = "Plot 88, Lake Side Road, Uttara",
                latitude = 23.8740,
                longitude = 90.3935,
                customerType = "Residential",
                status = "ACTIVE",
                registeredDate = "2024-03-12",
                notes = "Bulk CATV connection + 100Mbps Broadband for club house"
            )
        )
        customerDao.insertCustomers(customers)

        // 10. Customer Services (Internet & CATV under the same customer!)
        val services = listOf(
            CustomerServiceEntity(
                serviceId = "SVC-1001-INT",
                customerId = "CUST-1001",
                serviceType = "INTERNET",
                planName = "Enterprise Fiber 200 Mbps",
                onuDeviceId = "ONU-HW-001",
                onuSerialNumber = "48575443B1A2C301",
                fdbId = "FDB-NORTH-01",
                fdbPortNumber = 1,
                fiberCableId = "FBR-DIST-N2",
                fiberCoreNumber = 1,
                splitterId = "SPL-SEC-01",
                oltDeviceId = "DEV-OLT-01",
                ponId = "OLT-01-PON-01",
                ipAddress = "103.145.72.15",
                vlanId = 100,
                expectedRxPowerDbm = -19.4,
                measuredRxPowerDbm = -19.8,
                opticalStatus = "HEALTHY",
                status = "ACTIVE"
            ),
            CustomerServiceEntity(
                serviceId = "SVC-1001-CATV",
                customerId = "CUST-1001",
                serviceType = "CATV",
                planName = "Digital HD Broadcast 140 Ch",
                fdbId = "FDB-NORTH-01",
                fdbPortNumber = 2,
                fiberCableId = "FBR-DIST-N2",
                fiberCoreNumber = 2,
                splitterId = "SPL-CATV-TAP-01",
                expectedRxPowerDbm = -3.2,
                measuredRxPowerDbm = -3.5,
                opticalStatus = "HEALTHY",
                rfLevelDbMv = 14.5,
                status = "ACTIVE"
            ),
            CustomerServiceEntity(
                serviceId = "SVC-1002-INT",
                customerId = "CUST-1002",
                serviceType = "INTERNET",
                planName = "Ultra Fast Home 60 Mbps",
                onuDeviceId = "ONU-ZTE-002",
                onuSerialNumber = "ZTEG00921B55",
                fdbId = "FDB-NORTH-01",
                fdbPortNumber = 3,
                fiberCableId = "FBR-DIST-N2",
                fiberCoreNumber = 1,
                splitterId = "SPL-SEC-01",
                oltDeviceId = "DEV-OLT-01",
                ponId = "OLT-01-PON-01",
                ipAddress = "100.64.10.42",
                vlanId = 100,
                expectedRxPowerDbm = -20.2,
                measuredRxPowerDbm = -24.8, // Slightly high loss
                opticalStatus = "WARNING",
                status = "ACTIVE"
            ),
            CustomerServiceEntity(
                serviceId = "SVC-1002-CATV",
                customerId = "CUST-1002",
                serviceType = "CATV",
                planName = "Standard Digital CATV 100 Ch",
                fdbId = "FDB-NORTH-01",
                fdbPortNumber = 4,
                fiberCableId = "FBR-DIST-N2",
                fiberCoreNumber = 2,
                splitterId = "SPL-CATV-TAP-01",
                expectedRxPowerDbm = -4.0,
                measuredRxPowerDbm = -4.2,
                opticalStatus = "HEALTHY",
                rfLevelDbMv = 11.8,
                status = "ACTIVE"
            ),
            CustomerServiceEntity(
                serviceId = "SVC-1003-INT",
                customerId = "CUST-1003",
                serviceType = "INTERNET",
                planName = "Dedicated Leased Line 500 Mbps",
                onuDeviceId = "ONU-HW-003",
                onuSerialNumber = "48575443E9911002",
                fdbId = "FDB-NORTH-02",
                fdbPortNumber = 1,
                fiberCableId = "FBR-DIST-N1",
                fiberCoreNumber = 2,
                splitterId = "SPL-PRI-01",
                oltDeviceId = "DEV-OLT-01",
                ponId = "OLT-01-PON-02",
                ipAddress = "103.145.72.2",
                vlanId = 200,
                expectedRxPowerDbm = -17.5,
                measuredRxPowerDbm = -17.8,
                opticalStatus = "HEALTHY",
                status = "ACTIVE"
            )
        )
        customerDao.insertCustomerServices(services)

        // 11. Resellers
        val resellers = listOf(
            ResellerEntity(
                resellerId = "RES-01",
                companyName = "Metro NetLink ISP",
                contactName = "Shamsul Arefin",
                phone = "+880-1715-998822",
                location = "Uttara Sector 3 Sub-Station",
                allocatedBandwidthMbps = 1000,
                vlanId = 300,
                subnetCidr = "103.145.73.0/24",
                connectedSwitchPortId = "SW-HQ-P05",
                assignedFiberCableId = "FBR-BB-01",
                assignedFiberCoreNumber = 3,
                activeClientsCount = 180,
                status = "ACTIVE"
            ),
            ResellerEntity(
                resellerId = "RES-02",
                companyName = "FastWave Broadband Ltd",
                contactName = "Rezaul Karim",
                phone = "+880-1811-332211",
                location = "Bashundhara Gate 2",
                allocatedBandwidthMbps = 500,
                vlanId = 301,
                subnetCidr = "103.145.74.0/25",
                connectedSwitchPortId = "SW-HQ-P06",
                assignedFiberCableId = "FBR-BB-01",
                assignedFiberCoreNumber = 4,
                activeClientsCount = 95,
                status = "ACTIVE"
            )
        )
        resellerDao.insertResellers(resellers)

        // 12. Faults & Correlation Engine Seed Data
        val faults = listOf(
            FaultEntity(
                faultId = "FLT-2026-001",
                faultType = "FIBER_CUT",
                severity = "CRITICAL",
                title = "Major Road Excavation Fiber Cut on Feeder FBR-DIST-N1",
                description = "12-Core Aerial Fiber severed near Pole-101 during municipal sewer excavation",
                startTime = "2026-09-16 06:15:00",
                detectionTime = "2026-09-16 06:15:22",
                locationSiteId = "SITE-POLE-101",
                fiberCableId = "FBR-DIST-N1",
                fiberCoreNumber = 1,
                serviceType = "INTERNET",
                suspectedCause = "External utility excavator cut aerial tension wire and fiber tube",
                rootCauseFaultId = "", // Is Root Cause
                isRootCause = true,
                affectedCustomersCount = 48,
                affectedPopName = "North Sector Hub POP",
                assignedTechnicianId = "TECH-01",
                resolutionNotes = "Emergency splicing van dispatched with OTDR and Fujikura 90S splicer",
                status = "IN_PROGRESS"
            ),
            FaultEntity(
                faultId = "FLT-2026-002",
                faultType = "LOW_OPTICAL_POWER",
                severity = "MEDIUM",
                title = "High Micro-Bend Loss on FDB-NORTH-01 Drop Core 1",
                description = "Customer Dr. Sarah Jenkins RX Power degraded to -24.8 dBm (normal -20 dBm)",
                startTime = "2026-09-15 18:30:00",
                detectionTime = "2026-09-15 18:32:10",
                locationSiteId = "SITE-POLE-102",
                fiberCableId = "FBR-DIST-N2",
                fiberCoreNumber = 1,
                serviceType = "INTERNET",
                suspectedCause = "Tight cable bend radius or dirty SC/APC adapter in FDB tray",
                rootCauseFaultId = "",
                isRootCause = true,
                affectedCustomersCount = 1,
                assignedTechnicianId = "TECH-02",
                resolutionNotes = "Work order scheduled to inspect FDB tray and clean optical connector",
                status = "OPEN"
            )
        )
        faultDao.insertFaults(faults)

        // 13. Technicians & Work Orders
        val technicians = listOf(
            TechnicianEntity("TECH-01", "Mohammad Tariqul", "+880-1722-100200", "North Fiber Splicing Unit", "ON_SITE", 23.8650, 90.4010),
            TechnicianEntity("TECH-02", "Rashedul Haque", "+880-1722-100300", "Customer Last-Mile Care", "AVAILABLE", 23.8760, 90.3920),
            TechnicianEntity("TECH-03", "Anisur Rahman", "+880-1722-100400", "CATV & RF Engineering Squad", "AVAILABLE", 23.7925, 90.4078)
        )
        fieldOpsDao.insertTechnicians(technicians)

        val workOrders = listOf(
            WorkOrderEntity(
                workOrderId = "WO-9901",
                title = "Emergency Splice Joint Repair @ Pole 101",
                faultId = "FLT-2026-001",
                assetId = "FBR-DIST-N1",
                assignedTechnicianId = "TECH-01",
                technicianName = "Mohammad Tariqul",
                status = "WORKING",
                priority = "CRITICAL",
                siteAddress = "Corner of Lake Road & Ave 3, Uttara",
                latitude = 23.8650,
                longitude = 90.4010,
                scheduledTime = "06:45 AM Today",
                partsUsed = "1x 12-Core Inline Dome Enclosure, 12x Heat Shrink Sleeves",
                resolutionNotes = "Cleaving cores 1 through 6. OTDR test ready.",
                offlinePendingSync = false
            ),
            WorkOrderEntity(
                workOrderId = "WO-9902",
                title = "Clean and Re-seat SC/APC Adapter @ FDB-NORTH-01",
                faultId = "FLT-2026-002",
                customerId = "CUST-1002",
                assetId = "FDB-NORTH-01",
                assignedTechnicianId = "TECH-02",
                technicianName = "Rashedul Haque",
                status = "ASSIGNED",
                priority = "MEDIUM",
                siteAddress = "Gausul Azam Ave, Sector 13 (Pole 102)",
                latitude = 23.8760,
                longitude = 90.3920,
                scheduledTime = "11:00 AM Today",
                partsUsed = "Optical One-Click Cleaner 2.5mm",
                resolutionNotes = "",
                offlinePendingSync = false
            )
        )
        fieldOpsDao.insertWorkOrders(workOrders)

        // 14. Inventory
        val inventory = listOf(
            InventoryItemEntity("INV-001", "D1-AST-401", "OLT", "Huawei GPON Board GPUF 16-Port", "H808GPUF", "HW0921401", 3, "pcs", "STOCK", "North Hub Depot", qrCode = "D1-INV-401"),
            InventoryItemEntity("INV-002", "D1-AST-402", "ONU", "Huawei EchoLife GPON Terminal", "HG8546M", "48575443A900", 45, "pcs", "STOCK", "Central Warehouse", qrCode = "D1-INV-402"),
            InventoryItemEntity("INV-003", "D1-AST-403", "ONU", "ZTE Dual Band AC ONU", "F670L", "ZTEGF670001", 30, "pcs", "STOCK", "Central Warehouse", qrCode = "D1-INV-403"),
            InventoryItemEntity("INV-004", "D1-AST-404", "SPLITTER", "PLC Optical Splitter 1:8 SC/APC", "PLC-1x8-APC", "SPL8-2026", 24, "pcs", "STOCK", "North Splicing Van", qrCode = "D1-INV-404"),
            InventoryItemEntity("INV-005", "D1-AST-405", "SPLITTER", "PLC Optical Splitter 1:16 SC/APC", "PLC-1x16-APC", "SPL16-2026", 15, "pcs", "STOCK", "Central Warehouse", qrCode = "D1-INV-405"),
            InventoryItemEntity("INV-006", "D1-AST-406", "FDB", "Outdoor Waterproof FAT 16 Ports", "FAT-16P-IP68", "FAT16-90", 12, "pcs", "STOCK", "North Hub Depot", qrCode = "D1-INV-406"),
            InventoryItemEntity("INV-007", "D1-AST-407", "FIBER", "ADSS 24-Core Single Mode Drum 4km", "ADSS-24C-G652D", "FBR-DRUM-99", 2, "drums", "STOCK", "Central Yard", qrCode = "D1-INV-407"),
            InventoryItemEntity("INV-008", "D1-AST-408", "CATV_EDFA", "High Power 1550nm EDFA Amplifier 22dBm", "EYDFA-22-16P", "EDFA22-10", 1, "pcs", "STOCK", "Headend Lab", qrCode = "D1-INV-408"),
            InventoryItemEntity("INV-009", "D1-AST-409", "TOOLS", "Fujikura Core Alignment Fusion Splicer", "90S+", "FJK90S-7721", 2, "units", "ACTIVE", "Van #1 (Tariqul)", qrCode = "D1-INV-409")
        )
        inventoryDao.insertInventory(inventory)

        // 15. IPAM Subnets
        val subnets = listOf(
            IpSubnetEntity("SUB-01", "103.145.72.0/24", "PUBLIC", "103.145.72.1", 100, 254, 180, 20, "Public IPv4 pool for Enterprise clients"),
            IpSubnetEntity("SUB-02", "103.145.73.0/24", "RESELLER", "103.145.73.1", 300, 254, 210, 10, "Metro NetLink Reseller dedicated range"),
            IpSubnetEntity("SUB-03", "10.200.0.0/22", "MANAGEMENT", "10.200.0.1", 10, 1022, 142, 60, "Out-of-band & in-band network equipment management"),
            IpSubnetEntity("SUB-04", "100.64.0.0/18", "CUSTOMER_POOL", "100.64.0.1", 100, 16382, 4210, 500, "CGNAT Carrier Grade IP pool for residential PPPoE")
        )
        ipamDao.insertSubnets(subnets)

        // 16. Audit Log Initial Entry
        auditDao.insertAuditLog(
            AuditLogEntity(
                user = "System Architect",
                action = "INITIALIZE_DIGITAL_TWIN",
                entityType = "SYSTEM",
                entityId = "DIGITAL_ONE_ISP",
                details = "Initialized baseline Digital Twin network infrastructure for Digital One Broadband Internet + CATV."
            )
        )
    }

    suspend fun logAudit(user: String, action: String, entityType: String, entityId: String, details: String) {
        auditDao.insertAuditLog(
            AuditLogEntity(
                user = user,
                action = action,
                entityType = entityType,
                entityId = entityId,
                details = details
            )
        )
    }
}
