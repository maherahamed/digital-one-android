package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {
    @Query("SELECT * FROM sites ORDER BY name ASC")
    fun getAllSites(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE siteId = :siteId LIMIT 1")
    suspend fun getSiteById(siteId: String): SiteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSites(sites: List<SiteEntity>)
}

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY name ASC")
    fun getAllDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE deviceType = :type")
    fun getDevicesByType(type: String): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getDeviceById(deviceId: String): DeviceEntity?

    @Query("SELECT COUNT(*) FROM devices WHERE deviceType = 'OLT'")
    fun getOltCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM devices WHERE deviceType IN ('CORE_ROUTER', 'BACKUP_ROUTER')")
    fun getRouterCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM devices WHERE deviceType IN ('DISTRIBUTION_SWITCH', 'ACCESS_SWITCH')")
    fun getSwitchCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<DeviceEntity>)

    @Update
    suspend fun updateDevice(device: DeviceEntity)
}

@Dao
interface SwitchPortDao {
    @Query("SELECT * FROM switch_ports WHERE deviceId = :deviceId ORDER BY portNumber ASC")
    fun getPortsForDevice(deviceId: String): Flow<List<SwitchPortEntity>>

    @Query("SELECT * FROM switch_ports ORDER BY deviceId, portNumber ASC")
    fun getAllSwitchPorts(): Flow<List<SwitchPortEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSwitchPorts(ports: List<SwitchPortEntity>)
}

@Dao
interface OltPonPortDao {
    @Query("SELECT * FROM olt_pon_ports WHERE oltDeviceId = :oltDeviceId ORDER BY ponPortNumber ASC")
    fun getPonPortsForOlt(oltDeviceId: String): Flow<List<OltPonPortEntity>>

    @Query("SELECT * FROM olt_pon_ports ORDER BY oltDeviceId, ponPortNumber ASC")
    fun getAllPonPorts(): Flow<List<OltPonPortEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPonPorts(ports: List<OltPonPortEntity>)
}

@Dao
interface FiberDao {
    @Query("SELECT * FROM fiber_cables ORDER BY name ASC")
    fun getAllCables(): Flow<List<FiberCableEntity>>

    @Query("SELECT * FROM fiber_cables WHERE cableId = :cableId LIMIT 1")
    suspend fun getCableById(cableId: String): FiberCableEntity?

    @Query("SELECT * FROM fiber_cores WHERE cableId = :cableId ORDER BY coreNumber ASC")
    fun getCoresForCable(cableId: String): Flow<List<FiberCoreEntity>>

    @Query("SELECT * FROM fiber_cores ORDER BY cableId, coreNumber ASC")
    fun getAllCores(): Flow<List<FiberCoreEntity>>

    @Query("SELECT COUNT(*) FROM fiber_cables")
    fun getCableCount(): Flow<Int>

    @Query("SELECT SUM(lengthMeters) FROM fiber_cables")
    fun getTotalLengthMeters(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM fiber_cores")
    fun getTotalCoresCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM fiber_cores WHERE status = 'ACTIVE'")
    fun getActiveCoresCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM fiber_cores WHERE status = 'SPARE'")
    fun getSpareCoresCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCables(cables: List<FiberCableEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCores(cores: List<FiberCoreEntity>)

    @Update
    suspend fun updateCore(core: FiberCoreEntity)
}

@Dao
interface SplitterDao {
    @Query("SELECT * FROM splitters ORDER BY name ASC")
    fun getAllSplitters(): Flow<List<SplitterEntity>>

    @Query("SELECT COUNT(*) FROM splitters")
    fun getSplitterCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplitters(splitters: List<SplitterEntity>)
}

@Dao
interface FdbDao {
    @Query("SELECT * FROM fdbs ORDER BY name ASC")
    fun getAllFdbs(): Flow<List<FdbEntity>>

    @Query("SELECT * FROM fdbs WHERE fdbId = :fdbId LIMIT 1")
    suspend fun getFdbById(fdbId: String): FdbEntity?

    @Query("SELECT COUNT(*) FROM fdbs")
    fun getFdbCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFdbs(fdbs: List<FdbEntity>)

    @Update
    suspend fun updateFdb(fdb: FdbEntity)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE customerId = :customerId LIMIT 1")
    suspend fun getCustomerById(customerId: String): CustomerEntity?

    @Query("SELECT * FROM customer_services WHERE customerId = :customerId")
    fun getServicesForCustomer(customerId: String): Flow<List<CustomerServiceEntity>>

    @Query("SELECT * FROM customer_services ORDER BY customerId ASC")
    fun getAllCustomerServices(): Flow<List<CustomerServiceEntity>>

    @Query("SELECT COUNT(*) FROM customer_services WHERE serviceType = 'INTERNET'")
    fun getInternetCustomerCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM customer_services WHERE serviceType = 'CATV'")
    fun getCatvCustomerCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerServices(services: List<CustomerServiceEntity>)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Update
    suspend fun updateCustomerService(service: CustomerServiceEntity)
}

@Dao
interface ResellerDao {
    @Query("SELECT * FROM resellers ORDER BY companyName ASC")
    fun getAllResellers(): Flow<List<ResellerEntity>>

    @Query("SELECT COUNT(*) FROM resellers")
    fun getResellerCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResellers(resellers: List<ResellerEntity>)
}

@Dao
interface FaultDao {
    @Query("SELECT * FROM faults ORDER BY severity = 'CRITICAL' DESC, startTime DESC")
    fun getAllFaults(): Flow<List<FaultEntity>>

    @Query("SELECT * FROM faults WHERE status != 'RESOLVED' AND status != 'CLOSED'")
    fun getActiveFaults(): Flow<List<FaultEntity>>

    @Query("SELECT COUNT(*) FROM faults WHERE status != 'RESOLVED' AND status != 'CLOSED'")
    fun getActiveFaultCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM faults WHERE severity = 'CRITICAL' AND status != 'RESOLVED'")
    fun getCriticalFaultCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaults(faults: List<FaultEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFault(fault: FaultEntity)

    @Update
    suspend fun updateFault(fault: FaultEntity)
}

@Dao
interface FieldOpsDao {
    @Query("SELECT * FROM work_orders ORDER BY priority = 'HIGH' DESC, scheduledTime ASC")
    fun getAllWorkOrders(): Flow<List<WorkOrderEntity>>

    @Query("SELECT * FROM technicians ORDER BY name ASC")
    fun getAllTechnicians(): Flow<List<TechnicianEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkOrders(workOrders: List<WorkOrderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkOrder(workOrder: WorkOrderEntity)

    @Update
    suspend fun updateWorkOrder(workOrder: WorkOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTechnicians(technicians: List<TechnicianEntity>)
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items ORDER BY category ASC, itemName ASC")
    fun getAllInventory(): Flow<List<InventoryItemEntity>>

    @Query("SELECT COUNT(*) FROM inventory_items WHERE lifecycle = 'FAULT' OR lifecycle = 'REPAIR'")
    fun getInventoryWarningCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventory(items: List<InventoryItemEntity>)

    @Update
    suspend fun updateInventory(item: InventoryItemEntity)
}

@Dao
interface IpamDao {
    @Query("SELECT * FROM ip_subnets ORDER BY subnetCidr ASC")
    fun getAllSubnets(): Flow<List<IpSubnetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubnets(subnets: List<IpSubnetEntity>)
}

@Dao
interface AuditDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)
}
