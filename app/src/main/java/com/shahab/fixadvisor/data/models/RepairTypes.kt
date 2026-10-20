package com.shahab.fixadvisor.data.models

/** Fixed list of repair types used in filters, registration and reviews. */
object RepairTypes {
    val ALL = listOf(
        "Screen replacement",
        "Battery replacement",
        "Charging port",
        "Water damage",
        "Back glass",
        "Camera",
        "Speaker / Microphone",
        "Software / Unlock",
        "Other"
    )
}

/** Fixed list of parts-quality options shown when writing a review. */
object PartsQuality {
    val ALL = listOf("Original", "OEM", "Copy", "Not sure")
}

/** Sort orders for the shop list. */
enum class ShopSort {
    TOP_RATED,
    MOST_REVIEWED,
    NEWEST
}
