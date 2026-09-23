package com.example.engine

import com.example.data.model.*
import kotlin.math.*

/**
 * Node in the visual path trace diagram.
 */
data class TraceNode(
    val stepOrder: Int,
    val nodeType: String, // CUSTOMER, ONU, FDB, SPLITTER, FIBER_CORE, FIBER_CABLE, PON_PORT, OLT, SWITCH, CORE_ROUTER, UPSTREAM_ISP, CATV_HEADEND, etc.
    val title: String,
    val subtitle: String,
    val details: String,
    val opticalLevelDbm: Double? = null,
    val status: String = "ACTIVE"
)

data class PathTraceResult(
    val customerName: String,
    val serviceType: String,
    val planName: String,
    val overallStatus: String,
    val totalPathDistanceMeters: Double,
    val totalCalculatedLossDb: Double,
    val expectedRxDbm: Double,
    val measuredRxDbm: Double,
    val nodes: List<TraceNode>
)

data class OpticalBudgetCalculation(
    val txPowerDbm: Double,
    val distanceKm: Double,
    val fiberAttenuationDbPerKm: Double, // 0.35 at 1310nm, 0.22 at 1490/1550nm
    val fiberLossDb: Double,
    val connectorCount: Int,
    val connectorLossDb: Double,
    val spliceCount: Int,
    val spliceLossDb: Double,
    val splitterLossDb: Double,
    val patchCordLossDb: Double,
    val totalLossDb: Double,
    val expectedRxDbm: Double,
    val measuredRxDbm: Double,
    val opticalMarginDb: Double,
    val status: OpticalHealthStatus
)

data class RfBudgetCalculation(
    val transmitterOutputDbMv: Double, // e.g. +10 dBm converted to RF dBmV ~ 20-30 dBmV
    val opticalLossDb: Double,
    val opticalRxDbm: Double,
    val edfaGainDb: Double,
    val splitterLossDb: Double,
    val tapLossDb: Double,
    val coaxialDropLossDb: Double,
    val calculatedCustomerRfLevelDbMv: Double,
    val measuredCustomerRfLevelDbMv: Double,
    val status: String // HEALTHY (10 to 18 dBmV), WARNING, CRITICAL
)

data class WhatIfFeasibilityResult(
    val targetLat: Double,
    val targetLng: Double,
    val nearestFdbName: String,
    val nearestFdbDistanceMeters: Double,
    val fdbTotalPorts: Int,
    val fdbSparePorts: Int,
    val hasAvailableFdbPort: Boolean,
    val upstreamCableCode: String,
    val upstreamCableSpareCores: Int,
    val estimatedDropLossDb: Double,
    val estimatedTotalLossDb: Double,
    val estimatedRxPowerDbm: Double,
    val isFeasible: Boolean,
    val recommendation: String
)

object TopologyEngine {

    /**
     * Traces the physical and logical path of a customer service.
     */
    fun traceServicePath(
        customer: CustomerEntity,
        service: CustomerServiceEntity,
        cables: List<FiberCableEntity>,
        cores: List<FiberCoreEntity>,
        fdbs: List<FdbEntity>,
        splitters: List<SplitterEntity>,
        devices: List<DeviceEntity>,
        ports: List<SwitchPortEntity>,
        pons: List<OltPonPortEntity>
    ): PathTraceResult {
        val nodes = mutableListOf<TraceNode>()
        var step = 1

        if (service.serviceType == "INTERNET") {
            // Step 1: Customer Terminal
            nodes.add(
                TraceNode(
                    stepOrder = step++,
                    nodeType = "CUSTOMER",
                    title = customer.name,
                    subtitle = "Client ID: ${customer.customerId} (${customer.customerType})",
                    details = "Address: ${customer.address}\nGPS: (${customer.latitude}, ${customer.longitude})",
                    status = customer.status
                )
            )

            // Step 2: Customer ONU / ONT
            nodes.add(
                TraceNode(
                    stepOrder = step++,
                    nodeType = "ONU",
                    title = service.onuDeviceId.ifEmpty { "Customer ONU Terminal" },
                    subtitle = "Serial: ${service.onuSerialNumber} | IP: ${service.ipAddress}",
                    details = "Expected RX: ${service.expectedRxPowerDbm} dBm | Measured RX: ${service.measuredRxPowerDbm} dBm",
                    opticalLevelDbm = service.measuredRxPowerDbm,
                    status = service.opticalStatus
                )
            )

            // Step 3: FDB / FAT & Splitter
            val fdb = fdbs.find { it.fdbId == service.fdbId }
            val splitter = splitters.find { it.splitterId == service.splitterId }
            nodes.add(
                TraceNode(
                    stepOrder = step++,
                    nodeType = "FDB",
                    title = fdb?.name ?: "Fiber Distribution Box (FAT)",
                    subtitle = "Port #${service.fdbPortNumber} | Splitter ${splitter?.ratio ?: "1:8"}",
                    details = "Location: ${fdb?.locationSiteId ?: "Pole Mount"}\nSplitter Loss: ${splitter?.calculatedLossDb ?: 10.5} dB",
                    opticalLevelDbm = -9.0,
                    status = fdb?.status ?: "ACTIVE"
                )
            )

            // Step 4: Drop/Access Fiber Cable & Core
            val cable = cables.find { it.cableId == service.fiberCableId }
            val core = cores.find { it.cableId == service.fiberCableId && it.coreNumber == service.fiberCoreNumber }
            nodes.add(
                TraceNode(
                    stepOrder = step++,
                    nodeType = "FIBER_CORE",
                    title = "Core #${service.fiberCoreNumber} (${core?.colorName ?: "Blue"})",
                    subtitle = cable?.name ?: "Access Fiber Cable",
                    details = "Cable Type: ${cable?.cableType ?: "8-Core Drop"} | Length: ${cable?.lengthMeters ?: 640}m\nAttenuation @ 1490nm: 0.22 dB/km",
                    opticalLevelDbm = +2.5,
                    status = core?.status ?: "ACTIVE"
                )
            )

            // Step 5: OLT & PON Port
            val pon = pons.find { it.ponId == service.ponId }
            val olt = devices.find { it.deviceId == service.oltDeviceId }
            nodes.add(
                TraceNode(
                    stepOrder = step++,
                    nodeType = "PON_PORT",
                    title = "${olt?.name ?: "OLT"} [${pon?.ponId ?: "PON-01"}]",
                    subtitle = "PON Port ${pon?.ponPortNumber ?: 1} (${pon?.ponType ?: "GPON"})",
                    details = "Vendor: ${olt?.vendor ?: "Huawei"} ${olt?.model ?: "MA5800"}\nTX Output Power: +${pon?.txPowerDbm ?: 2.8} dBm | Connected ONUs: ${pon?.connectedOnuCount ?: 48}/128",
                    opticalLevelDbm = pon?.txPowerDbm ?: +2.8,
                    status = olt?.status ?: "ONLINE"
                )
            )

            // Step 6: Distribution Switch
            val switch = devices.find { it.deviceType == "DISTRIBUTION_SWITCH" }
            val switchPort = ports.find { it.deviceId == switch?.deviceId && it.vlanId == service.vlanId }
            nodes.add(
                TraceNode(
                    stepOrder = step++,
                    nodeType = "SWITCH",
                    title = switch?.name ?: "D1-SW-DIST-HQ-01",
                    subtitle = "Port: ${switchPort?.portName ?: "xe-0/0/3"} | VLAN ${service.vlanId}",
                    details = "Speed: ${switchPort?.speedMbps ?: 10000} Mbps (10G SFP+)\nUplink to Core Gateway",
                    status = switch?.status ?: "ONLINE"
                )
            )

            // Step 7: Core Router
            val coreRouter = devices.find { it.deviceType == "CORE_ROUTER" }
            nodes.add(
                TraceNode(
                    stepOrder = step++,
                    nodeType = "CORE_ROUTER",
                    title = coreRouter?.name ?: "D1-CORE-RTR-01",
                    subtitle = "BGP Autonomous System AS139100",
                    details = "Management IP: ${coreRouter?.managementIp ?: "10.200.0.1"}\nHardware: ${coreRouter?.vendor} ${coreRouter?.model}",
                    status = coreRouter?.status ?: "ONLINE"
                )
            )

            // Step 8: Upstream Global Internet Transit
            nodes.add(
                TraceNode(
                    stepOrder = step++,
                    nodeType = "UPSTREAM_ISP",
                    title = "Tier-1 Upstream Transits",
                    subtitle = "Lumen (AS3356) + Cogent (AS174)",
                    details = "Dual 100Gbps BGP Full Table Peering Gateway",
                    status = "ONLINE"
                )
            )
        } else {
            // CATV Path Trace
            nodes.add(
                TraceNode(
                    stepOrder = step++,
                    nodeType = "CUSTOMER",
                    title = customer.name,
                    subtitle = "CATV Subscriber: ${customer.customerId}",
                    details = "Address: ${customer.address}\nService: ${service.planName}",
                    status = customer.status
                )
            )

            nodes.add(
                TraceNode(
                    stepOrder = step++,
                    nodeType = "CATV_TAP",
                    title = "Outdoor CATV 4-Way Tap (14dB)",
                    subtitle = "FDB Mount @ ${service.fdbId}",
                    details = "Customer Coaxial RF Output: ${service.rfLevelDbMv} dBmV (Target: 10-18 dBmV)",
                    status = "HEALTHY"
                )
            )

            nodes.add(
                TraceNode(
                    stepOrder = step++,
                    nodeType = "EDFA_AMPLIFIER",
                    title = "High-Power EDFA 22dBm Repeater",
                    subtitle = "Location: North Sector Hub POP",
                    details = "Optical Gain: +22 dBm Output | Wavelength: 1550nm Broadcast Overlay",
                    opticalLevelDbm = +22.0,
                    status = "ONLINE"
                )
            )

            nodes.add(
                TraceNode(
                    stepOrder = step++,
                    nodeType = "OPTICAL_TRANSMITTER",
                    title = "1550nm Optical Transmitter 10dBm",
                    subtitle = "Headend Broadcast Rack-CATV-01",
                    details = "Direct Modulated 1550nm Laser with AGC & CNR > 52dB",
                    opticalLevelDbm = +10.0,
                    status = "ONLINE"
                )
            )

            nodes.add(
                TraceNode(
                    stepOrder = step++,
                    nodeType = "RECEIVER_IRD",
                    title = "Harmonic ProView 8100 IRD",
                    subtitle = "MPEG-4 AVC / HEVC Multi-Channel Demodulator",
                    details = "Output: 120 Clear QAM HD/SD Channels into RF Matrix",
                    status = "ONLINE"
                )
            )

            nodes.add(
                TraceNode(
                    stepOrder = step++,
                    nodeType = "SATELLITE_DISH",
                    title = "3.8m C-Band Commercial Satellite Dish",
                    subtitle = "Bangabandhu-1 (119.1°E) / Apstar-7",
                    details = "Dual Circular Polarization C-Band LNB with Ultra-Low Phase Noise",
                    status = "ONLINE"
                )
            )
        }

        val totalLoss = abs((service.expectedRxPowerDbm) - 2.8)
        return PathTraceResult(
            customerName = customer.name,
            serviceType = service.serviceType,
            planName = service.planName,
            overallStatus = service.opticalStatus,
            totalPathDistanceMeters = 2490.0,
            totalCalculatedLossDb = totalLoss,
            expectedRxDbm = service.expectedRxPowerDbm,
            measuredRxDbm = service.measuredRxPowerDbm,
            nodes = nodes
        )
    }

    /**
     * Optical Power Budget calculation for fiber engineering.
     */
    fun calculateOpticalBudget(
        txPowerDbm: Double = +2.5,
        distanceKm: Double = 2.5,
        fiberAttenuationDbPerKm: Double = 0.35, // 0.35 at 1310nm, 0.22 at 1490nm
        connectorCount: Int = 4,
        connectorLossDb: Double = 0.3,
        spliceCount: Int = 3,
        spliceLossDb: Double = 0.05,
        splitterLossDb: Double = 17.7, // 1:4 (7.2dB) + 1:8 (10.5dB) = 17.7 dB
        patchCordLossDb: Double = 0.5,
        measuredRxDbm: Double = -20.5
    ): OpticalBudgetCalculation {
        val fiberLoss = distanceKm * fiberAttenuationDbPerKm
        val totalConnectorLoss = connectorCount * connectorLossDb
        val totalSpliceLoss = spliceCount * spliceLossDb
        val totalLoss = fiberLoss + totalConnectorLoss + totalSpliceLoss + splitterLossDb + patchCordLossDb
        val expectedRx = txPowerDbm - totalLoss
        val margin = measuredRxDbm - (-27.0) // sensitivity cutoff at -27 dBm

        val status = when {
            measuredRxDbm in -24.0..-14.0 -> OpticalHealthStatus.HEALTHY
            measuredRxDbm in -27.0..-24.0 -> OpticalHealthStatus.WARNING
            else -> OpticalHealthStatus.CRITICAL
        }

        return OpticalBudgetCalculation(
            txPowerDbm = txPowerDbm,
            distanceKm = distanceKm,
            fiberAttenuationDbPerKm = fiberAttenuationDbPerKm,
            fiberLossDb = fiberLoss,
            connectorCount = connectorCount,
            connectorLossDb = totalConnectorLoss,
            spliceCount = spliceCount,
            spliceLossDb = totalSpliceLoss,
            splitterLossDb = splitterLossDb,
            patchCordLossDb = patchCordLossDb,
            totalLossDb = totalLoss,
            expectedRxDbm = expectedRx,
            measuredRxDbm = measuredRxDbm,
            opticalMarginDb = margin,
            status = status
        )
    }

    /**
     * CATV RF Engineering Budget calculation.
     */
    fun calculateRfBudget(
        txOutputDbm: Double = 10.0,
        opticalFiberLossDb: Double = 3.5,
        edfaGainDb: Double = 14.0,
        splitterLossDb: Double = 7.2,
        tapLossDb: Double = 14.0,
        dropCableLossDb: Double = 2.5,
        measuredRfDbMv: Double = 14.2
    ): RfBudgetCalculation {
        val opticalRx = txOutputDbm - opticalFiberLossDb
        // RF Level in dBmV: Optical Receiver produces ~ 30 dBmV at 0 dBm optical in
        val rfBase = 30.0 + (opticalRx) + edfaGainDb * 0.4
        val totalRfLoss = splitterLossDb + tapLossDb + dropCableLossDb
        val calculatedRf = rfBase - totalRfLoss

        val status = when {
            measuredRfDbMv in 10.0..18.0 -> "HEALTHY"
            measuredRfDbMv in 6.0..10.0 || measuredRfDbMv in 18.0..22.0 -> "WARNING"
            else -> "CRITICAL"
        }

        return RfBudgetCalculation(
            transmitterOutputDbMv = 30.0,
            opticalLossDb = opticalFiberLossDb,
            opticalRxDbm = opticalRx,
            edfaGainDb = edfaGainDb,
            splitterLossDb = splitterLossDb,
            tapLossDb = tapLossDb,
            coaxialDropLossDb = dropCableLossDb,
            calculatedCustomerRfLevelDbMv = calculatedRf,
            measuredCustomerRfLevelDbMv = measuredRfDbMv,
            status = status
        )
    }

    /**
     * What-If Planner for evaluating new customer connection feasibility.
     */
    fun planNewCustomerConnection(
        lat: Double,
        lng: Double,
        fdbs: List<FdbEntity>,
        cables: List<FiberCableEntity>,
        cores: List<FiberCoreEntity>
    ): WhatIfFeasibilityResult {
        if (fdbs.isEmpty()) {
            return WhatIfFeasibilityResult(
                targetLat = lat,
                targetLng = lng,
                nearestFdbName = "None Available",
                nearestFdbDistanceMeters = 0.0,
                fdbTotalPorts = 0,
                fdbSparePorts = 0,
                hasAvailableFdbPort = false,
                upstreamCableCode = "None",
                upstreamCableSpareCores = 0,
                estimatedDropLossDb = 0.0,
                estimatedTotalLossDb = 0.0,
                estimatedRxPowerDbm = 0.0,
                isFeasible = false,
                recommendation = "No FDBs registered in the area. Fiber route extension required."
            )
        }

        // Find nearest FDB using haversine
        var nearestFdb = fdbs.first()
        var minDistance = Double.MAX_VALUE
        for (fdb in fdbs) {
            val dist = calculateDistanceMeters(lat, lng, fdb.latitude, fdb.longitude)
            if (dist < minDistance) {
                minDistance = dist
                nearestFdb = fdb
            }
        }

        val fdbSparePorts = nearestFdb.sparePorts
        val upstreamCable = cables.find { it.cableId == nearestFdb.inputFiberCableId }
        val spareCoresCount = cores.count { it.cableId == nearestFdb.inputFiberCableId && it.status == "SPARE" }

        // Drop cable estimated loss (assuming standard 2-core drop @ 0.35dB/km + 2 SC/APC mechanical connectors)
        val dropDistKm = minDistance / 1000.0
        val dropLoss = (dropDistKm * 0.35) + 0.6 // connectors
        val estimatedTotalLoss = 17.7 + dropLoss
        val estimatedRx = +2.5 - estimatedTotalLoss

        val isFeasible = fdbSparePorts > 0 && minDistance <= 500.0 && estimatedRx >= -25.0

        val recommendation = when {
            minDistance > 500.0 -> "Distance exceeds standard drop limit of 500m (${minDistance.toInt()}m). Intermediate distribution FAT recommended."
            fdbSparePorts == 0 -> "FDB ${nearestFdb.name} is at full capacity (0 spare ports). Splitter upgrade or second FAT needed."
            estimatedRx < -25.0 -> "Estimated optical level is marginal (${String.format("%.1f", estimatedRx)} dBm). Check upstream splitter cascade."
            else -> "Feasible! Connect from ${nearestFdb.name} Port #${nearestFdb.capacityPorts - fdbSparePorts + 1}. Estimated RX: ${String.format("%.1f", estimatedRx)} dBm."
        }

        return WhatIfFeasibilityResult(
            targetLat = lat,
            targetLng = lng,
            nearestFdbName = nearestFdb.name,
            nearestFdbDistanceMeters = minDistance,
            fdbTotalPorts = nearestFdb.capacityPorts,
            fdbSparePorts = fdbSparePorts,
            hasAvailableFdbPort = fdbSparePorts > 0,
            upstreamCableCode = upstreamCable?.code ?: "N/A",
            upstreamCableSpareCores = spareCoresCount,
            estimatedDropLossDb = dropLoss,
            estimatedTotalLossDb = estimatedTotalLoss,
            estimatedRxPowerDbm = estimatedRx,
            isFeasible = isFeasible,
            recommendation = recommendation
        )
    }

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
