package com.example.engine

data class DeviceTelemetry(
    val deviceId: String,
    val isReachable: Boolean,
    val pingLatencyMs: Double,
    val packetLossPercent: Double,
    val cpuUsagePercent: Int,
    val memoryUsagePercent: Int,
    val temperatureCelsius: Double,
    val opticalRxPowerDbm: Double? = null,
    val opticalTxPowerDbm: Double? = null,
    val adapterType: String,
    val lastPollTime: String,
    val isMock: Boolean = true
)

interface MonitoringAdapter {
    val adapterName: String
    suspend fun pollDevice(targetIp: String, deviceId: String): DeviceTelemetry
}

class IcmpPingAdapter : MonitoringAdapter {
    override val adapterName: String = "ICMP Ping Adapter (Safe Demo)"
    override suspend fun pollDevice(targetIp: String, deviceId: String): DeviceTelemetry {
        return DeviceTelemetry(
            deviceId = deviceId,
            isReachable = true,
            pingLatencyMs = 1.84,
            packetLossPercent = 0.0,
            cpuUsagePercent = 0,
            memoryUsagePercent = 0,
            temperatureCelsius = 0.0,
            adapterType = "ICMP_PING",
            lastPollTime = "Just now",
            isMock = true
        )
    }
}

class SnmpAdapter : MonitoringAdapter {
    override val adapterName: String = "SNMP v2c/v3 Adapter (Safe Demo)"
    override suspend fun pollDevice(targetIp: String, deviceId: String): DeviceTelemetry {
        return DeviceTelemetry(
            deviceId = deviceId,
            isReachable = true,
            pingLatencyMs = 2.1,
            packetLossPercent = 0.0,
            cpuUsagePercent = 14,
            memoryUsagePercent = 38,
            temperatureCelsius = 41.5,
            adapterType = "SNMP_V3",
            lastPollTime = "Just now",
            isMock = true
        )
    }
}

class MikroTikAdapter : MonitoringAdapter {
    override val adapterName: String = "MikroTik RouterOS API Adapter (Safe Demo)"
    override suspend fun pollDevice(targetIp: String, deviceId: String): DeviceTelemetry {
        return DeviceTelemetry(
            deviceId = deviceId,
            isReachable = true,
            pingLatencyMs = 1.2,
            packetLossPercent = 0.0,
            cpuUsagePercent = 8,
            memoryUsagePercent = 29,
            temperatureCelsius = 37.0,
            adapterType = "ROUTEROS_API",
            lastPollTime = "Just now",
            isMock = true
        )
    }
}

class OltAdapter : MonitoringAdapter {
    override val adapterName: String = "Huawei/ZTE OLT Telemetry Adapter (Safe Demo)"
    override suspend fun pollDevice(targetIp: String, deviceId: String): DeviceTelemetry {
        return DeviceTelemetry(
            deviceId = deviceId,
            isReachable = true,
            pingLatencyMs = 3.4,
            packetLossPercent = 0.0,
            cpuUsagePercent = 22,
            memoryUsagePercent = 45,
            temperatureCelsius = 43.0,
            opticalRxPowerDbm = -19.6,
            opticalTxPowerDbm = +2.8,
            adapterType = "OLT_GPON_CLI",
            lastPollTime = "Just now",
            isMock = true
        )
    }
}

class SwitchAdapter : MonitoringAdapter {
    override val adapterName: String = "Cisco/Juniper Switch NETCONF Adapter (Safe Demo)"
    override suspend fun pollDevice(targetIp: String, deviceId: String): DeviceTelemetry {
        return DeviceTelemetry(
            deviceId = deviceId,
            isReachable = true,
            pingLatencyMs = 1.5,
            packetLossPercent = 0.0,
            cpuUsagePercent = 11,
            memoryUsagePercent = 32,
            temperatureCelsius = 39.2,
            adapterType = "NETCONF_SWITCH",
            lastPollTime = "Just now",
            isMock = true
        )
    }
}

object MonitoringEngine {
    val icmpAdapter = IcmpPingAdapter()
    val snmpAdapter = SnmpAdapter()
    val mikroTikAdapter = MikroTikAdapter()
    val oltAdapter = OltAdapter()
    val switchAdapter = SwitchAdapter()

    suspend fun getTelemetryForDevice(deviceType: String, deviceId: String, ip: String): DeviceTelemetry {
        return when (deviceType) {
            "OLT" -> oltAdapter.pollDevice(ip, deviceId)
            "DISTRIBUTION_SWITCH", "ACCESS_SWITCH" -> switchAdapter.pollDevice(ip, deviceId)
            "CORE_ROUTER", "BACKUP_ROUTER" -> snmpAdapter.pollDevice(ip, deviceId)
            else -> icmpAdapter.pollDevice(ip, deviceId)
        }
    }
}
