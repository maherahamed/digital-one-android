package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.IspRepository
import com.example.engine.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GlobalSearchResult(
    val entityType: String, // Customer, ONU, Device, Fiber, Core, FDB, Splitter, Reseller, Fault, WorkOrder
    val title: String,
    val subtitle: String,
    val tag: String,
    val id: String
)

class IspViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: IspRepository

    val sites: StateFlow<List<SiteEntity>>
    val devices: StateFlow<List<DeviceEntity>>
    val cables: StateFlow<List<FiberCableEntity>>
    val cores: StateFlow<List<FiberCoreEntity>>
    val fdbs: StateFlow<List<FdbEntity>>
    val splitters: StateFlow<List<SplitterEntity>>
    val customers: StateFlow<List<CustomerEntity>>
    val customerServices: StateFlow<List<CustomerServiceEntity>>
    val resellers: StateFlow<List<ResellerEntity>>
    val faults: StateFlow<List<FaultEntity>>
    val activeFaults: StateFlow<List<FaultEntity>>
    val workOrders: StateFlow<List<WorkOrderEntity>>
    val technicians: StateFlow<List<TechnicianEntity>>
    val inventory: StateFlow<List<InventoryItemEntity>>
    val subnets: StateFlow<List<IpSubnetEntity>>
    val auditLogs: StateFlow<List<AuditLogEntity>>

    // Metrics
    val oltCount: StateFlow<Int>
    val routerCount: StateFlow<Int>
    val switchCount: StateFlow<Int>
    val cableCount: StateFlow<Int>
    val totalKm: StateFlow<Double>
    val totalCores: StateFlow<Int>
    val activeCores: StateFlow<Int>
    val spareCores: StateFlow<Int>
    val internetCustCount: StateFlow<Int>
    val catvCustCount: StateFlow<Int>
    val resellerCount: StateFlow<Int>
    val fdbCount: StateFlow<Int>
    val splitterCount: StateFlow<Int>
    val activeFaultCount: StateFlow<Int>
    val criticalFaultCount: StateFlow<Int>

    // Active Selection State for Path Tracing
    private val _selectedCustomerId = MutableStateFlow<String>("CUST-1001")
    val selectedCustomerId: StateFlow<String> = _selectedCustomerId

    private val _selectedServiceType = MutableStateFlow<String>("INTERNET")
    val selectedServiceType: StateFlow<String> = _selectedServiceType

    // Field Ops Offline Simulation
    private val _isFieldOnline = MutableStateFlow<Boolean>(true)
    val isFieldOnline: StateFlow<Boolean> = _isFieldOnline

    private val _fieldSyncStatus = MutableStateFlow<String>("SYNCED") // ONLINE, OFFLINE, SYNCING, SYNCED, SYNC ERROR
    val fieldSyncStatus: StateFlow<String> = _fieldSyncStatus

    // Global search query
    private val _searchQuery = MutableStateFlow<String>("")
    val searchQuery: StateFlow<String> = _searchQuery

    // What-If Planner input state
    private val _plannerLat = MutableStateFlow<Double>(23.8780)
    val plannerLat: StateFlow<Double> = _plannerLat

    private val _plannerLng = MutableStateFlow<Double>(90.3950)
    val plannerLng: StateFlow<Double> = _plannerLng

    init {
        val db = AppDatabase.getDatabase(application)
        repository = IspRepository(db)

        sites = repository.allSites.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        devices = repository.allDevices.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        cables = repository.allCables.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        cores = repository.allCores.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        fdbs = repository.allFdbs.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        splitters = repository.allSplitters.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        customers = repository.allCustomers.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        customerServices = repository.allCustomerServices.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        resellers = repository.allResellers.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        faults = repository.allFaults.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        activeFaults = repository.activeFaults.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        workOrders = repository.allWorkOrders.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        technicians = repository.allTechnicians.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        inventory = repository.allInventory.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        subnets = repository.allSubnets.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        auditLogs = repository.recentAuditLogs.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        oltCount = repository.oltCount.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
        routerCount = repository.routerCount.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
        switchCount = repository.switchCount.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
        cableCount = repository.cableCount.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
        totalKm = repository.totalKm.map { (it ?: 0.0) / 1000.0 }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)
        totalCores = repository.totalCores.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
        activeCores = repository.activeCores.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
        spareCores = repository.spareCores.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
        internetCustCount = repository.internetCustCount.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
        catvCustCount = repository.catvCustCount.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
        resellerCount = repository.resellerCount.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
        fdbCount = repository.fdbCount.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
        splitterCount = repository.splitterCount.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
        activeFaultCount = repository.activeFaultCount.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
        criticalFaultCount = repository.criticalFaultCount.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

        // Seed initial data
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
        }
    }

    fun selectCustomer(customerId: String, serviceType: String = "INTERNET") {
        _selectedCustomerId.value = customerId
        _selectedServiceType.value = serviceType
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setPlannerCoordinates(lat: Double, lng: Double) {
        _plannerLat.value = lat
        _plannerLng.value = lng
    }

    fun toggleFieldOnlineMode() {
        val newMode = !_isFieldOnline.value
        _isFieldOnline.value = newMode
        if (newMode) {
            _fieldSyncStatus.value = "SYNCING"
            viewModelScope.launch {
                kotlinx.coroutines.delay(1200)
                _fieldSyncStatus.value = "SYNCED"
            }
        } else {
            _fieldSyncStatus.value = "OFFLINE"
        }
    }

    fun updateWorkOrderStatus(workOrderId: String, newStatus: String, notes: String = "") {
        viewModelScope.launch {
            val list = workOrders.value
            val wo = list.find { it.workOrderId == workOrderId } ?: return@launch
            val updated = wo.copy(
                status = newStatus,
                resolutionNotes = if (notes.isNotEmpty()) notes else wo.resolutionNotes,
                offlinePendingSync = !_isFieldOnline.value
            )
            repository.fieldOpsDao.updateWorkOrder(updated)
            repository.logAudit("Technician", "UPDATE_WORK_ORDER", "WORK_ORDER", workOrderId, "Status changed to $newStatus")
        }
    }

    fun createWorkOrder(title: String, technicianId: String, priority: String, address: String) {
        viewModelScope.launch {
            val tech = technicians.value.find { it.technicianId == technicianId }
            val newWo = WorkOrderEntity(
                workOrderId = "WO-${System.currentTimeMillis() % 10000}",
                title = title,
                assignedTechnicianId = technicianId,
                technicianName = tech?.name ?: "Field Tech",
                status = "ASSIGNED",
                priority = priority,
                siteAddress = address,
                offlinePendingSync = !_isFieldOnline.value
            )
            repository.fieldOpsDao.insertWorkOrder(newWo)
            repository.logAudit("Dispatcher", "CREATE_WORK_ORDER", "WORK_ORDER", newWo.workOrderId, "Created work order $title")
        }
    }

    fun updateFiberCoreStatus(cableId: String, coreNumber: Int, newStatus: String, serviceType: String) {
        viewModelScope.launch {
            val coreList = cores.value
            val core = coreList.find { it.cableId == cableId && it.coreNumber == coreNumber } ?: return@launch
            val updated = core.copy(status = newStatus, serviceType = serviceType)
            repository.fiberDao.updateCore(updated)
            repository.logAudit("Operator", "UPDATE_FIBER_CORE", "FIBER_CORE", core.coreId, "Changed core $coreNumber to $newStatus / $serviceType")
        }
    }

    fun resolveFault(faultId: String, resolutionNotes: String) {
        viewModelScope.launch {
            val f = faults.value.find { it.faultId == faultId } ?: return@launch
            val updated = f.copy(status = "RESOLVED", resolutionNotes = resolutionNotes)
            repository.faultDao.updateFault(updated)
            repository.logAudit("Engineer", "RESOLVE_FAULT", "FAULT", faultId, "Fault marked as RESOLVED. Notes: $resolutionNotes")
        }
    }

    fun updateFaultStatus(faultId: String, newStatus: String, notes: String = "") {
        viewModelScope.launch {
            val f = faults.value.find { it.faultId == faultId } ?: return@launch
            val updated = f.copy(
                status = newStatus,
                resolutionNotes = if (notes.isNotBlank()) notes else f.resolutionNotes
            )
            repository.faultDao.updateFault(updated)
            repository.logAudit("NOC Operator", "UPDATE_FAULT_STATUS", "FAULT", faultId, "Fault status changed to $newStatus")
        }
    }

    fun createFaultTicket(
        title: String,
        severity: String,
        faultType: String,
        description: String,
        affectedPop: String,
        fiberCableId: String = "",
        deviceId: String = "",
        affectedCustomers: Int = 12
    ) {
        viewModelScope.launch {
            val count = faults.value.size + 1
            val newFault = FaultEntity(
                faultId = "FLT-${System.currentTimeMillis() % 100000}",
                faultType = faultType,
                severity = severity,
                title = title,
                description = description,
                startTime = "Just now",
                detectionTime = "Immediate Telemetry Alarm",
                fiberCableId = fiberCableId,
                deviceId = deviceId,
                affectedPopName = affectedPop,
                affectedCustomersCount = affectedCustomers,
                status = "OPEN"
            )
            repository.faultDao.insertFault(newFault)
            repository.logAudit("NOC Controller", "CREATE_FAULT_TICKET", "FAULT", newFault.faultId, "Logged incident: $title ($severity)")
        }
    }

    fun addCustomer(name: String, phone: String, address: String, serviceType: String, plan: String, fdbId: String) {
        viewModelScope.launch {
            val newId = "CUST-${(customers.value.size + 1001)}"
            val cust = CustomerEntity(
                customerId = newId,
                name = name,
                phone = phone,
                email = "${name.lowercase().replace(" ", "")}@domain.com",
                address = address,
                latitude = 23.8750,
                longitude = 90.3930,
                customerType = "Residential",
                status = "ACTIVE"
            )
            val svc = CustomerServiceEntity(
                serviceId = "SVC-$newId-${if (serviceType == "INTERNET") "INT" else "CATV"}",
                customerId = newId,
                serviceType = serviceType,
                planName = plan,
                fdbId = fdbId,
                fdbPortNumber = 5,
                fiberCableId = "FBR-DIST-N2",
                fiberCoreNumber = 1,
                splitterId = "SPL-SEC-01",
                oltDeviceId = "DEV-OLT-01",
                ponId = "OLT-01-PON-01",
                ipAddress = "100.64.12.${(10..250).random()}",
                status = "ACTIVE"
            )
            repository.customerDao.insertCustomers(listOf(cust))
            repository.customerDao.insertCustomerServices(listOf(svc))
            repository.logAudit("Billing/Provisioning", "PROVISION_CUSTOMER", "CUSTOMER", newId, "Provisioned $name with $plan")
        }
    }

    // Path Trace Computation for currently selected customer
    val currentPathTrace: StateFlow<PathTraceResult?> = combine(
        _selectedCustomerId,
        _selectedServiceType,
        customers,
        customerServices
    ) { customerId, serviceType, custList, svcList ->
        val cust = custList.find { it.customerId == customerId } ?: custList.firstOrNull()
        if (cust == null) return@combine null

        val svc = svcList.find { it.customerId == cust.customerId && it.serviceType == serviceType }
            ?: svcList.find { it.customerId == cust.customerId }
            ?: return@combine null

        TopologyEngine.traceServicePath(
            customer = cust,
            service = svc,
            cables = cables.value,
            cores = cores.value,
            fdbs = fdbs.value,
            splitters = splitters.value,
            devices = devices.value,
            ports = emptyList(),
            pons = emptyList()
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Fault correlation tree
    val correlatedFaults: StateFlow<List<CorrelatedFaultTree>> = combine(
        faults,
        cables,
        devices,
        customers,
        customerServices
    ) { fList, cList, dList, custList, svcList ->
        FaultCorrelationEngine.correlateFaults(fList, cList, dList, custList, svcList)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // What-If Feasibility result
    val whatIfResult: StateFlow<WhatIfFeasibilityResult> = combine(
        _plannerLat,
        _plannerLng,
        fdbs,
        cables,
        cores
    ) { lat, lng, fdbList, cableList, coreList ->
        TopologyEngine.planNewCustomerConnection(lat, lng, fdbList, cableList, coreList)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        WhatIfFeasibilityResult(
            targetLat = 23.8780,
            targetLng = 90.3950,
            nearestFdbName = "FDB Lake View 16-Port FAT",
            nearestFdbDistanceMeters = 320.0,
            fdbTotalPorts = 16,
            fdbSparePorts = 9,
            hasAvailableFdbPort = true,
            upstreamCableCode = "FBR-08-PL101-FDB01",
            upstreamCableSpareCores = 4,
            estimatedDropLossDb = 0.71,
            estimatedTotalLossDb = 18.41,
            estimatedRxPowerDbm = -15.91,
            isFeasible = true,
            recommendation = "Feasible! Connect from FDB Lake View FAT Port #8. Estimated RX: -15.9 dBm."
        )
    )

    // Unified Global Search across multiple entity tables
    val searchResults: StateFlow<List<GlobalSearchResult>> = _searchQuery.map { query ->
        val q = query.trim().lowercase()
        if (q.isEmpty()) return@map emptyList<GlobalSearchResult>()

        val results = mutableListOf<GlobalSearchResult>()
        val custList = customers.value
        val svcList = customerServices.value
        val devList = devices.value
        val cableList = cables.value
        val fdbList = fdbs.value
        val resList = resellers.value
        val fltList = faults.value

        // Search Customers
        for (c in custList) {
            if (c.name.lowercase().contains(q) || c.customerId.lowercase().contains(q) || c.phone.contains(q)) {
                results.add(
                    GlobalSearchResult(
                        entityType = "Customer",
                        title = c.name,
                        subtitle = "${c.customerId} | ${c.phone} | ${c.address}",
                        tag = c.status,
                        id = c.customerId
                    )
                )
            }
        }

        // Search ONUs & Services
        for (s in svcList) {
            if (s.onuSerialNumber.lowercase().contains(q) || s.ipAddress.contains(q) || s.onuDeviceId.lowercase().contains(q)) {
                results.add(
                    GlobalSearchResult(
                        entityType = "ONU / Service",
                        title = "${s.serviceType}: ${s.planName}",
                        subtitle = "ONU: ${s.onuSerialNumber} | IP: ${s.ipAddress} | Cust: ${s.customerId}",
                        tag = s.opticalStatus,
                        id = s.serviceId
                    )
                )
            }
        }

        // Search Devices (OLT, Switch, Routers)
        for (d in devList) {
            if (d.name.lowercase().contains(q) || d.managementIp.contains(q) || d.serialNumber.lowercase().contains(q) || d.assetId.lowercase().contains(q)) {
                results.add(
                    GlobalSearchResult(
                        entityType = "Device",
                        title = d.name,
                        subtitle = "${d.deviceType} | IP: ${d.managementIp} | S/N: ${d.serialNumber}",
                        tag = d.status,
                        id = d.deviceId
                    )
                )
            }
        }

        // Search Fiber Cables
        for (cb in cableList) {
            if (cb.name.lowercase().contains(q) || cb.code.lowercase().contains(q) || cb.cableId.lowercase().contains(q)) {
                results.add(
                    GlobalSearchResult(
                        entityType = "Fiber Cable",
                        title = cb.name,
                        subtitle = "${cb.cableType} | Cores: ${cb.coreCount} | ${cb.lengthMeters.toInt()}m",
                        tag = cb.status,
                        id = cb.cableId
                    )
                )
            }
        }

        // Search FDBs
        for (f in fdbList) {
            if (f.name.lowercase().contains(q) || f.code.lowercase().contains(q) || f.fdbId.lowercase().contains(q)) {
                results.add(
                    GlobalSearchResult(
                        entityType = "FDB / FAT",
                        title = f.name,
                        subtitle = "${f.code} | Ports: ${f.capacityPorts} (Spare: ${f.sparePorts})",
                        tag = f.status,
                        id = f.fdbId
                    )
                )
            }
        }

        // Search Resellers
        for (r in resList) {
            if (r.companyName.lowercase().contains(q) || r.resellerId.lowercase().contains(q) || r.contactName.lowercase().contains(q)) {
                results.add(
                    GlobalSearchResult(
                        entityType = "Reseller",
                        title = r.companyName,
                        subtitle = "Bandwidth: ${r.allocatedBandwidthMbps}M | VLAN: ${r.vlanId} | Clients: ${r.activeClientsCount}",
                        tag = r.status,
                        id = r.resellerId
                    )
                )
            }
        }

        // Search Faults
        for (flt in fltList) {
            if (flt.title.lowercase().contains(q) || flt.faultId.lowercase().contains(q) || flt.description.lowercase().contains(q)) {
                results.add(
                    GlobalSearchResult(
                        entityType = "Fault",
                        title = flt.title,
                        subtitle = "${flt.faultId} | ${flt.faultType} | Affected: ${flt.affectedCustomersCount}",
                        tag = flt.severity,
                        id = flt.faultId
                    )
                )
            }
        }

        results.take(30)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Generate CSV export format for reports.
     */
    fun generateExportCsv(reportType: String): String {
        val sb = StringBuilder()
        when (reportType) {
            "FIBER_INVENTORY" -> {
                sb.append("Cable ID,Code,Name,Type,Core Count,Length (m),Status\n")
                cables.value.forEach {
                    sb.append("\"${it.cableId}\",\"${it.code}\",\"${it.name}\",\"${it.cableType}\",${it.coreCount},${it.lengthMeters},\"${it.status}\"\n")
                }
            }
            "CUSTOMER_LIST" -> {
                sb.append("Customer ID,Name,Phone,Email,Type,Address,Status\n")
                customers.value.forEach {
                    sb.append("\"${it.customerId}\",\"${it.name}\",\"${it.phone}\",\"${it.email}\",\"${it.customerType}\",\"${it.address}\",\"${it.status}\"\n")
                }
            }
            "FAULTS" -> {
                sb.append("Fault ID,Type,Severity,Title,Affected Customers,Status\n")
                faults.value.forEach {
                    sb.append("\"${it.faultId}\",\"${it.faultType}\",\"${it.severity}\",\"${it.title}\",${it.affectedCustomersCount},\"${it.status}\"\n")
                }
            }
            "IPAM_SUBNETS" -> {
                sb.append("Subnet ID,CIDR,Type,Gateway,VLAN,Total IPs,Used IPs,Reserved\n")
                subnets.value.forEach {
                    sb.append("\"${it.subnetId}\",\"${it.subnetCidr}\",\"${it.ipType}\",\"${it.gateway}\",${it.vlanId},${it.totalIps},${it.usedIps},${it.reservedIps}\"\n")
                }
            }
            else -> {
                sb.append("Item ID,Asset Tag,Category,Item Name,Model,Qty,Lifecycle,Location\n")
                inventory.value.forEach {
                    sb.append("\"${it.itemId}\",\"${it.assetTag}\",\"${it.category}\",\"${it.itemName}\",\"${it.model}\",${it.quantity},\"${it.lifecycle}\",\"${it.location}\"\n")
                }
            }
        }
        return sb.toString()
    }
}
