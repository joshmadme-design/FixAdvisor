package com.shahab.fixadvisor.data.repo

import android.content.Context
import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.shahab.fixadvisor.data.models.Shop
import com.shahab.fixadvisor.data.models.ShopSort
import com.shahab.fixadvisor.data.util.QrCodeUtils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore `shops` collection + Firebase Storage shop photos.
 *
 * Documents are mapped manually (safe casts) so a malformed document can
 * never crash the app. Filtering and sorting happen client-side on the
 * live snapshot — deliberate for Phase 1 so no composite Firestore indexes
 * need to be created (see README).
 */
class ShopRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db: FirebaseFirestore = Firebase.firestore
    private val shops = db.collection("shops")

    /** Live list of all shops, sorted by rating by default. */
    fun observeShops(): Flow<List<Shop>> = callbackFlow {
        val registration = shops.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { mapShop(it.id, it.data) }.orEmpty()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    suspend fun getShop(shopId: String): Shop? {
        val snap = shops.document(shopId).get().await()
        return if (snap.exists()) mapShop(snap.id, snap.data) else null
    }

    /**
     * Registers a new shop. Generates the verification code deterministically
     * from the new document id. Returns the created [Shop].
     */
    suspend fun createShop(
        ownerUid: String,
        ownerEmail: String,
        name: String,
        address: String,
        phone: String,
        whatsapp: String,
        websiteUrl: String,
        googleMapsUrl: String,
        services: List<String>,
        prices: Map<String, Double>,
        openingHours: String,
        photoUris: List<Uri>
    ): Shop {
        val docRef = shops.document()
        val now = System.currentTimeMillis()
        val photoUrls = uploadPhotos(docRef.id, photoUris)
        val shop = Shop(
            id = docRef.id,
            name = name.trim(),
            address = address.trim(),
            phone = phone.trim(),
            whatsapp = whatsapp.filter { it.isDigit() },
            websiteUrl = websiteUrl.trim(),
            googleMapsUrl = googleMapsUrl.trim(),
            services = services,
            prices = prices,
            openingHours = openingHours.trim(),
            photoUrls = photoUrls,
            ownerUid = ownerUid,
            ownerEmail = ownerEmail,
            verificationCode = QrCodeUtils.verificationCodeFor(docRef.id),
            averageRating = 0.0,
            reviewCount = 0,
            createdAt = now,
            updatedAt = now
        )
        docRef.set(shopToMap(shop)).await()
        return shop
    }

    /** Uploads shop photos to `shops/{shopId}/...`; returns download URLs. */
    private suspend fun uploadPhotos(shopId: String, uris: List<Uri>): List<String> {
        val storage = Firebase.storage
        return uris.mapIndexed { index, uri ->
            val ref = storage.reference.child("shops/$shopId/photo_$index.jpg")
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        }
    }

    // ---- Client-side search / filter / sort (pure, testable) ----

    fun filterAndSort(
        all: List<Shop>,
        query: String,
        repairType: String?,
        sort: ShopSort
    ): List<Shop> {
        val q = query.trim().lowercase()
        val filtered = all.filter { shop ->
            val matchesQuery = q.isEmpty() ||
                shop.name.lowercase().contains(q) ||
                shop.address.lowercase().contains(q) ||
                shop.services.any { it.lowercase().contains(q) }
            val matchesType = repairType == null || shop.services.contains(repairType)
            matchesQuery && matchesType
        }
        return when (sort) {
            ShopSort.TOP_RATED -> filtered.sortedWith(
                compareByDescending<Shop> { it.averageRating }.thenByDescending { it.reviewCount }
            )
            ShopSort.MOST_REVIEWED -> filtered.sortedByDescending { it.reviewCount }
            ShopSort.NEWEST -> filtered.sortedByDescending { it.createdAt }
        }
    }
}

/** Manual Firestore -> Shop mapping with safe casts. */
fun mapShop(id: String, data: Map<String, Any?>?): Shop? {
    if (data == null) return null
    fun str(key: String): String = (data[key] as? String).orEmpty()
    fun num(key: String): Double = (data[key] as? Number)?.toDouble() ?: 0.0
    fun long(key: String): Long = (data[key] as? Number)?.toLong() ?: 0L
    fun int(key: String): Int = (data[key] as? Number)?.toInt() ?: 0
    val prices = (data["prices"] as? Map<*, *>)
        ?.mapNotNull { (k, v) ->
            val key = k as? String ?: return@mapNotNull null
            val value = (v as? Number)?.toDouble() ?: return@mapNotNull null
            key to value
        }?.toMap().orEmpty()
    return Shop(
        id = id,
        name = str("name"),
        address = str("address"),
        phone = str("phone"),
        whatsapp = str("whatsapp"),
        websiteUrl = str("websiteUrl"),
        googleMapsUrl = str("googleMapsUrl"),
        services = (data["services"] as? List<*>)?.filterIsInstance<String>().orEmpty(),
        prices = prices,
        openingHours = str("openingHours"),
        photoUrls = (data["photoUrls"] as? List<*>)?.filterIsInstance<String>().orEmpty(),
        ownerUid = str("ownerUid"),
        ownerEmail = str("ownerEmail"),
        verificationCode = str("verificationCode"),
        averageRating = num("averageRating"),
        reviewCount = int("reviewCount"),
        createdAt = long("createdAt"),
        updatedAt = long("updatedAt")
    )
}

/** Shop -> Firestore map. */
fun shopToMap(shop: Shop): Map<String, Any?> = mapOf(
    "name" to shop.name,
    "address" to shop.address,
    "phone" to shop.phone,
    "whatsapp" to shop.whatsapp,
    "websiteUrl" to shop.websiteUrl,
    "googleMapsUrl" to shop.googleMapsUrl,
    "services" to shop.services,
    "prices" to shop.prices,
    "openingHours" to shop.openingHours,
    "photoUrls" to shop.photoUrls,
    "ownerUid" to shop.ownerUid,
    "ownerEmail" to shop.ownerEmail,
    "verificationCode" to shop.verificationCode,
    "averageRating" to shop.averageRating,
    "reviewCount" to shop.reviewCount,
    "createdAt" to shop.createdAt,
    "updatedAt" to shop.updatedAt
)
