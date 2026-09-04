package com.travellikepro.opsleader.data.model.vendor

import java.util.UUID

enum class ServiceType {
    HOTEL,
    TRANSPORT,
    GUIDE,
    ACTIVITY_OPERATOR,
    RESTAURANT,
    OTHER
}

enum class VendorAvailability {
    AVAILABLE,
    AT_CAPACITY,
    UNAVAILABLE
}

data class Vendor(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val contactPhone: String,
    val serviceType: ServiceType,
    val regionCoverage: List<String>, // States/UTs from AppConfig
    val availabilityStatus: VendorAvailability,
    val qualityScore: Double = 5.0,
    val totalAssignments: Int = 0
)

enum class AssignmentStatus {
    ASSIGNED,
    CONFIRMED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

data class VendorAssignment(
    val id: String = UUID.randomUUID().toString(),
    val vendorId: String,
    val vendorName: String,
    val touristId: String,
    val touristName: String,
    val tripId: String,
    val serviceType: ServiceType,
    val assignedTimestamp: Long, // Epoch millis
    val assignedByOpsLeader: String,
    val status: AssignmentStatus = AssignmentStatus.ASSIGNED,
    val remarks: String? = null
)
