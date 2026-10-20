package com.shahab.fixadvisor.data.models

/**
 * A registered phone repair shop. Stored in the `shops` Firestore collection
 * (document id = shop id).
 *
 * Maps are read/written manually in [com.shahab.fixadvisor.data.repo.ShopRepository]
 * (safe casts instead of Firestore toObject()) so a malformed document can
 * never crash the app.
 */
data class Shop(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    /** Display phone number (optional). The Call button uses ACTION_DIAL. */
    val phone: String = "",
    /** WhatsApp number in international format without "+" (optional). */
    val whatsapp: String = "",
    val websiteUrl: String = "",
    val googleMapsUrl: String = "",
    /** Repair types offered, e.g. "Screen replacement", "Battery". */
    val services: List<String> = emptyList(),
    /** Repair type -> price in RM. Only entries with a valid price. */
    val prices: Map<String, Double> = emptyMap(),
    /** Free text, e.g. "Mon–Sat 10am–9pm". */
    val openingHours: String = "",
    /** Firebase Storage download URLs. */
    val photoUrls: List<String> = emptyList(),
    /** Firebase uid of the registering owner. */
    val ownerUid: String = "",
    val ownerEmail: String = "",
    /**
     * Stable 6-digit code (derived from the shop id). The owner shows this to
     * customers after a repair; a review entered with the matching code is
     * marked verified.
     */
    val verificationCode: String = "",
    /** Denormalized aggregates, maintained by a transaction on review write. */
    val averageRating: Double = 0.0,
    val reviewCount: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
