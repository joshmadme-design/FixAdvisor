package com.shahab.fixadvisor.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shahab.fixadvisor.data.repo.AuthManager
import com.shahab.fixadvisor.data.repo.ShopRepository
import com.shahab.fixadvisor.data.repo.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Shop registration form. Phase 1: one shop per user. */
class RegisterShopViewModel(
    private val shopRepository: ShopRepository,
    private val userRepository: UserRepository,
    val authManager: AuthManager
) : ViewModel() {

    companion object {
        const val MAX_PHOTOS = 6
    }

    private val nameFlow = MutableStateFlow("")
    val nameState: StateFlow<String> = nameFlow.asStateFlow()
    private val addressFlow = MutableStateFlow("")
    val addressState: StateFlow<String> = addressFlow.asStateFlow()
    private val phoneFlow = MutableStateFlow("")
    val phoneState: StateFlow<String> = phoneFlow.asStateFlow()
    private val whatsappFlow = MutableStateFlow("")
    val whatsappState: StateFlow<String> = whatsappFlow.asStateFlow()
    private val websiteUrlFlow = MutableStateFlow("")
    val websiteUrlState: StateFlow<String> = websiteUrlFlow.asStateFlow()
    private val googleMapsUrlFlow = MutableStateFlow("")
    val googleMapsUrlState: StateFlow<String> = googleMapsUrlFlow.asStateFlow()
    private val openingHoursFlow = MutableStateFlow("")
    val openingHoursState: StateFlow<String> = openingHoursFlow.asStateFlow()

    private val _services = MutableStateFlow<Set<String>>(emptySet())
    val services: StateFlow<Set<String>> = _services.asStateFlow()

    /** Price text per selected service (RM). */
    private val _prices = MutableStateFlow<Map<String, String>>(emptyMap())
    val prices: StateFlow<Map<String, String>> = _prices.asStateFlow()

    private val _photoUris = MutableStateFlow<List<Uri>>(emptyList())
    val photoUris: StateFlow<List<Uri>> = _photoUris.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** Set to the new shop id when registration succeeds. */
    private val _registeredShopId = MutableStateFlow<String?>(null)
    val registeredShopId: StateFlow<String?> = _registeredShopId.asStateFlow()

    fun setName(v: String) { nameFlow.value = v }
    fun setAddress(v: String) { addressFlow.value = v }
    fun setPhone(v: String) { phoneFlow.value = v }
    fun setWhatsapp(v: String) { whatsappFlow.value = v }
    fun setWebsiteUrl(v: String) { websiteUrlFlow.value = v }
    fun setGoogleMapsUrl(v: String) { googleMapsUrlFlow.value = v }
    fun setOpeningHours(v: String) { openingHoursFlow.value = v }

    fun toggleService(service: String) {
        _services.value = if (service in _services.value) {
            _services.value - service
        } else {
            _services.value + service
        }
    }

    fun setPrice(service: String, value: String) {
        _prices.value = _prices.value + (service to value)
    }

    fun addPhotos(uris: List<Uri>) {
        _photoUris.value = (_photoUris.value + uris).distinct().take(MAX_PHOTOS)
    }

    fun removePhoto(uri: Uri) {
        _photoUris.value = _photoUris.value - uri
    }

    fun clearMessage() { _message.value = null }

    fun submit() {
        // Guest mode: no sign-in wall — the app runs on a per-device guest identity.
        val user = authManager.appUser
        if (nameFlow.value.isBlank()) {
            _message.value = "Shop name is required."
            return
        }
        if (addressFlow.value.isBlank()) {
            _message.value = "Shop address is required."
            return
        }
        if (_services.value.isEmpty()) {
            _message.value = "Please select at least one repair service."
            return
        }
        val parsedPrices = _services.value.mapNotNull { service ->
            val raw = _prices.value[service]?.trim().orEmpty()
            if (raw.isEmpty()) return@mapNotNull null
            val value = raw.toDoubleOrNull()
            if (value == null || value < 0) {
                _message.value = "Price for \"$service\" is not a valid number."
                return
            }
            service to value
        }.toMap()

        _submitting.value = true
        viewModelScope.launch {
            try {
                userRepository.ensureProfile(user)
                val profile = userRepository.observeProfile(user.uid).first()
                if (profile?.ownedShopId != null) {
                    _message.value = "You have already registered a shop."
                    return@launch
                }
                val shop = shopRepository.createShop(
                    ownerUid = user.uid,
                    ownerEmail = user.email.orEmpty(),
                    name = nameFlow.value,
                    address = addressFlow.value,
                    phone = phoneFlow.value,
                    whatsapp = whatsappFlow.value,
                    websiteUrl = websiteUrlFlow.value,
                    googleMapsUrl = googleMapsUrlFlow.value,
                    services = _services.value.toList(),
                    prices = parsedPrices,
                    openingHours = openingHoursFlow.value,
                    photoUris = _photoUris.value
                )
                userRepository.setOwnedShop(user.uid, shop.id)
                _registeredShopId.value = shop.id
            } catch (e: Exception) {
                _message.value = e.message ?: "Could not register the shop."
            } finally {
                _submitting.value = false
            }
        }
    }
}
