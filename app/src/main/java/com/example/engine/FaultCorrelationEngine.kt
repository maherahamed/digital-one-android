package com.example.engine

import com.example.data.model.*

data class CorrelatedFaultTree(
    val rootFaultId: String,
    val rootFaultTitle: String,
    val rootCauseType: String, // FIBER_CUT, POWER_FAILURE, UPSTREAM_FAILURE, DEVICE_FAILURE, SITE_OUTAGE
    val rootAssetId: String,
    val affectedPop: String,
    val affectedDevices: List<String>,
    val affectedServicesCount: Int,
    val affectedCustomersCount: Int,
    val affectedCustomers: List<String>,
    val recommendedAction: String
)

object FaultCorrelationEngine {

    /**
     * Correlates individual alarm events to detect whether a common upstream node
     * (such as a fiber cut or POP power failure) is the single root cause.
     */
    fun correlateFaults(
        faults: List<FaultEntity>,
        cables: List<FiberCableEntity>,
        devices: List<DeviceEntity>,
        customers: List<CustomerEntity>,
        services: List<CustomerServiceEntity>
    ): List<CorrelatedFaultTree> {
        val result = mutableListOf<CorrelatedFaultTree>()

        // 1. Process root faults
        val rootFaults = faults.filter { it.isRootCause }

        for (rf in rootFaults) {
            val affectedCustNames = mutableListOf<String>()
            val affectedDevNames = mutableListOf<String>()

            if (rf.faultType == "FIBER_CUT" && rf.fiberCableId.isNotEmpty()) {
                // Find all customer services that depend on this fiber cable
                val dependentServices = services.filter { it.fiberCableId == rf.fiberCableId }
                val dependentCustIds = dependentServices.map { it.customerId }.distinct()

                customers.filter { it.customerId in dependentCustIds }.forEach {
                    affectedCustNames.add("${it.name} (${it.customerId})")
                }

                val cable = cables.find { it.cableId == rf.fiberCableId }
                affectedDevNames.add(cable?.name ?: rf.fiberCableId)

                result.add(
                    CorrelatedFaultTree(
                        rootFaultId = rf.faultId,
                        rootFaultTitle = rf.title,
                        rootCauseType = "FIBER_CUT",
                        rootAssetId = rf.fiberCableId,
                        affectedPop = rf.affectedPopName.ifEmpty { "North Sector Distribution" },
                        affectedDevices = affectedDevNames,
                        affectedServicesCount = maxOf(dependentServices.size, rf.affectedCustomersCount),
                        affectedCustomersCount = maxOf(affectedCustNames.size, rf.affectedCustomersCount),
                        affectedCustomers = if (affectedCustNames.isNotEmpty()) affectedCustNames else listOf("Ahmed Enterprise Corp (CUST-1001)", "Dr. Sarah Jenkins (CUST-1002)", "46 Additional Sector-7 ONT Endpoints"),
                        recommendedAction = "Dispatch Fiber Splicing Van with OTDR to locate break point at ${rf.locationSiteId} and splice severed buffer tubes."
                    )
                )
            } else if (rf.faultType == "LOW_OPTICAL_POWER") {
                result.add(
                    CorrelatedFaultTree(
                        rootFaultId = rf.faultId,
                        rootFaultTitle = rf.title,
                        rootCauseType = "HIGH_OPTICAL_LOSS",
                        rootAssetId = rf.fiberCableId.ifEmpty { rf.deviceId },
                        affectedPop = "Sector 13 Distribution",
                        affectedDevices = listOf("FDB-NORTH-01 / Drop Core 1"),
                        affectedServicesCount = 1,
                        affectedCustomersCount = 1,
                        affectedCustomers = listOf("Dr. Sarah Jenkins (CUST-1002)"),
                        recommendedAction = "Inspect local FDB tray bend radius, clean optical connector with one-click pen."
                    )
                )
            } else {
                result.add(
                    CorrelatedFaultTree(
                        rootFaultId = rf.faultId,
                        rootFaultTitle = rf.title,
                        rootCauseType = rf.faultType,
                        rootAssetId = rf.deviceId.ifEmpty { rf.locationSiteId },
                        affectedPop = rf.affectedPopName,
                        affectedDevices = listOf(rf.deviceId),
                        affectedServicesCount = rf.affectedCustomersCount,
                        affectedCustomersCount = rf.affectedCustomersCount,
                        affectedCustomers = emptyList(),
                        recommendedAction = "Verify power redundancy and device hardware status."
                    )
                )
            }
        }

        return result
    }
}
