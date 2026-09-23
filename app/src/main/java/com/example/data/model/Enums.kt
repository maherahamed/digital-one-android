package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Service Types supported across Digital One ISP infrastructure.
 */
enum class ServiceType {
    INTERNET,
    CATV,
    CCTV,
    VOICE,
    OTHER,
    SPARE
}

/**
 * Core Status within a physical fiber cable.
 */
enum class CoreStatus {
    ACTIVE,
    SPARE,
    RESERVED,
    FAULT,
    DISCONNECTED
}

/**
 * Device Category in Digital One Network Twin.
 */
enum class DeviceType {
    CORE_ROUTER,
    BACKUP_ROUTER,
    DISTRIBUTION_SWITCH,
    ACCESS_SWITCH,
    OLT,
    ONU,
    SFP_MODULE,
    OPTICAL_TRANSMITTER,
    OPTICAL_RECEIVER,
    EDFA_AMPLIFIER,
    CATV_AMPLIFIER,
    RECEIVER_IRD,
    SATELLITE_DISH,
    OTHER
}

/**
 * Operational Status for devices and links.
 */
enum class OperationalStatus {
    ONLINE,
    OFFLINE,
    DEGRADED,
    MAINTENANCE,
    SUSPECTED_OUTAGE
}

/**
 * Fault Types supported by Fault Management & Correlation Engine.
 */
enum class FaultType {
    DEVICE_DOWN,
    LINK_DOWN,
    FIBER_CUT,
    HIGH_OPTICAL_LOSS,
    LOW_OPTICAL_POWER,
    OLT_FAILURE,
    PON_FAILURE,
    SWITCH_FAILURE,
    POWER_FAILURE,
    SITE_OUTAGE,
    UPSTREAM_FAILURE,
    CUSTOMER_FAULT,
    UNKNOWN
}

enum class FaultSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class FaultStatus {
    OPEN,
    INVESTIGATING,
    IN_PROGRESS,
    RESOLVED,
    CLOSED
}

enum class WorkOrderStatus {
    NEW,
    ASSIGNED,
    ACCEPTED,
    ON_WAY,
    REACHED,
    WORKING,
    COMPLETED,
    CANCELLED
}

enum class InventoryLifecycle {
    PROCURED,
    STOCK,
    RESERVED,
    INSTALLED,
    ACTIVE,
    FAULT,
    REPAIR,
    REPLACED,
    RETIRED
}

enum class SplitterRatio {
    RATIO_1_2,
    RATIO_1_4,
    RATIO_1_8,
    RATIO_1_16,
    RATIO_1_32,
    RATIO_1_64,
    CUSTOM
}

enum class OpticalHealthStatus {
    HEALTHY,
    WARNING,
    CRITICAL,
    UNKNOWN
}
