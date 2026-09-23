package com.example.engine

import com.example.data.model.*
import org.junit.Assert.*
import org.junit.Test

class TopologyEngineTest {

    @Test
    fun testOpticalBudgetCalculation_Healthy() {
        val budget = TopologyEngine.calculateOpticalBudget(
            txPowerDbm = +2.8,
            distanceKm = 2.5,
            connectorCount = 4,
            spliceCount = 3,
            splitterLossDb = 17.7,
            measuredRxDbm = -20.5
        )

        // Loss = 2.5 * 0.35 (0.875) + 4 * 0.3 (1.2) + 3 * 0.05 (0.15) + 17.7 + 0.5 (patchCord) = 20.425 dB
        // Expected RX = 2.8 - 20.425 = -17.625 dBm
        // Margin = -20.5 - (-27.0) = 6.5 dB
        assertEquals(20.43, budget.totalLossDb, 0.05)
        assertEquals(-17.63, budget.expectedRxDbm, 0.05)
        assertEquals(6.5, budget.opticalMarginDb, 0.05)
        assertEquals(OpticalHealthStatus.HEALTHY, budget.status)
    }

    @Test
    fun testOpticalBudgetCalculation_Critical() {
        val budget = TopologyEngine.calculateOpticalBudget(
            txPowerDbm = +1.0,
            distanceKm = 10.0,
            connectorCount = 6,
            spliceCount = 10,
            splitterLossDb = 21.0,
            measuredRxDbm = -28.0
        )

        // Measured RX is -28.0 dBm, which is worse than sensitivity threshold (-27.0)
        assertEquals(OpticalHealthStatus.CRITICAL, budget.status)
        assertTrue(budget.opticalMarginDb <= 0.0)
    }

    @Test
    fun testRfBudgetCalculation() {
        val rfBudget = TopologyEngine.calculateRfBudget(
            txOutputDbm = 10.0,
            opticalFiberLossDb = 3.5,
            edfaGainDb = 14.0,
            tapLossDb = 14.0,
            dropCableLossDb = 2.5,
            measuredRfDbMv = 14.2
        )

        // Optical In @ Rx = 10.0 - 3.5 = 6.5 dBm
        assertEquals(6.5, rfBudget.opticalRxDbm, 0.1)
        assertEquals("HEALTHY", rfBudget.status)
    }

    @Test
    fun testWhatIfExpansionFeasibility() {
        val fdbs = listOf(
            FdbEntity(
                fdbId = "FDB-01",
                code = "FDB-NORTH-01",
                name = "North FDB",
                locationSiteId = "SITE-01",
                latitude = 23.8780,
                longitude = 90.3950,
                inputFiberCableId = "CABLE-01",
                inputFiberCoreNumber = 1,
                splitterId = "SPL-01",
                capacityPorts = 16,
                usedPorts = 7,
                sparePorts = 9
            )
        )

        val cables = listOf(
            FiberCableEntity(
                cableId = "CABLE-01",
                code = "FBR-08",
                name = "North Feeder",
                startSiteId = "POP-NORTH",
                endSiteId = "SITE-01",
                routeName = "North Sector Route",
                cableType = "ADSS Aerial",
                coreCount = 8,
                lengthMeters = 800.0
            )
        )

        val cores = (1..8).map { coreNum ->
            FiberCoreEntity(
                coreId = "CABLE-01-CORE-$coreNum",
                cableId = "CABLE-01",
                coreNumber = coreNum,
                colorName = "Blue",
                colorHex = "#0284C7",
                status = if (coreNum <= 4) "ACTIVE" else "SPARE",
                serviceType = if (coreNum <= 4) "INTERNET" else "SPARE"
            )
        }

        val result = TopologyEngine.planNewCustomerConnection(
            lat = 23.8782,
            lng = 90.3952,
            fdbs = fdbs,
            cables = cables,
            cores = cores
        )

        assertTrue(result.isFeasible)
        assertEquals("North FDB", result.nearestFdbName)
        assertTrue(result.fdbSparePorts > 0)
        assertEquals(4, result.upstreamCableSpareCores)
    }

    @Test
    fun testFaultCorrelation() {
        val faults = listOf(
            FaultEntity(
                faultId = "FLT-001",
                faultType = "FIBER_CUT",
                severity = "CRITICAL",
                title = "Backbone Fiber Cut at Pole 101",
                description = "48-core cable snapped",
                startTime = "2026-09-16 10:00",
                detectionTime = "2026-09-16 10:01",
                fiberCableId = "CABLE-01",
                affectedPopName = "North POP",
                isRootCause = true,
                affectedCustomersCount = 48
            ),
            FaultEntity(
                faultId = "FLT-002",
                faultType = "OPTICAL_POWER_LOW",
                severity = "HIGH",
                title = "ONU Loss of Signal",
                description = "Customer ONU signal lost",
                startTime = "2026-09-16 10:02",
                detectionTime = "2026-09-16 10:02",
                fiberCableId = "CABLE-01",
                isRootCause = false,
                rootCauseFaultId = "FLT-001"
            )
        )

        val correlated = FaultCorrelationEngine.correlateFaults(
            faults = faults,
            cables = emptyList(),
            devices = emptyList(),
            customers = emptyList(),
            services = emptyList()
        )

        assertEquals(1, correlated.size)
        assertEquals("FLT-001", correlated[0].rootFaultId)
        assertEquals("FIBER_CUT", correlated[0].rootCauseType)
    }
}
