package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        SiteEntity::class,
        DeviceEntity::class,
        SwitchPortEntity::class,
        OltPonPortEntity::class,
        FiberCableEntity::class,
        FiberCoreEntity::class,
        SplitterEntity::class,
        FdbEntity::class,
        CustomerEntity::class,
        CustomerServiceEntity::class,
        ResellerEntity::class,
        FaultEntity::class,
        WorkOrderEntity::class,
        TechnicianEntity::class,
        InventoryItemEntity::class,
        IpSubnetEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun siteDao(): SiteDao
    abstract fun deviceDao(): DeviceDao
    abstract fun switchPortDao(): SwitchPortDao
    abstract fun oltPonPortDao(): OltPonPortDao
    abstract fun fiberDao(): FiberDao
    abstract fun splitterDao(): SplitterDao
    abstract fun fdbDao(): FdbDao
    abstract fun customerDao(): CustomerDao
    abstract fun resellerDao(): ResellerDao
    abstract fun faultDao(): FaultDao
    abstract fun fieldOpsDao(): FieldOpsDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun ipamDao(): IpamDao
    abstract fun auditDao(): AuditDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "digital_one_isp.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
