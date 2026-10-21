package com.shahab.fixadvisor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shahab.fixadvisor.data.models.Review
import com.shahab.fixadvisor.data.models.Shop
import com.shahab.fixadvisor.data.repo.AuthManager
import com.shahab.fixadvisor.data.repo.ReviewRepository
import com.shahab.fixadvisor.data.repo.ShopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Write-a-review form for one shop.
 *
 * Phase 1 rule (approximation of "one review per repair"): one review per
 * user per shop. The check runs against the shop's review list before submit.
 */
class WriteReviewViewModel(
    private val reviewRepository: ReviewRepository,
    shopRepository: ShopRepository,
    val authManager: AuthManager,
    private val shopId: String,
    prefilledCode: String?
) : ViewModel() {

    val shop: StateFlow<Shop?> = shopRepository.observeShops()
        .map { list -> list.find { it.id == shopId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val starsFlow = MutableStateFlow(0)
    val starsState: StateFlow<Int> = starsFlow.asStateFlow()

    private val repairTypeFlow = MutableStateFlow("")
    val repairTypeState: StateFlow<String> = repairTypeFlow.asStateFlow()

    private val phoneModelFlow = MutableStateFlow("")
    val phoneModelState: StateFlow<String> = phoneModelFlow.asStateFlow()

    private val pricePaidFlow = MutableStateFlow("")
    val pricePaidState: StateFlow<String> = pricePaidFlow.asStateFlow()

    private val timeTakenFlow = MutableStateFlow("")
    val timeTakenState: StateFlow<String> = timeTakenFlow.asStateFlow()

    private val partsQualityFlow = MutableStateFlow("")
    val partsQualityState: StateFlow<String> = partsQualityFlow.asStateFlow()

    private val wouldReturnFlow = MutableStateFlow(true)
    val wouldReturnState: StateFlow<Boolean> = wouldReturnFlow.asStateFlow()

    private val textFlow = MutableStateFlow("")
    val textState: StateFlow<String> = textFlow.asStateFlow()

    private val verificationCodeFlow = MutableStateFlow(prefilledCode.orEmpty())
    val verificationCodeState: StateFlow<String> = verificationCodeFlow.asStateFlow()

    private val _alreadyReviewed = MutableStateFlow(false)
    val alreadyReviewed: StateFlow<Boolean> = _alreadyReviewed.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    fun setStars(v: Int) { starsFlow.value = v.coerceIn(0, 5) }
    fun setRepairType(v: String) { repairTypeFlow.value = v }
    fun setPhoneModel(v: String) { phoneModelFlow.value = v }
    fun setPricePaid(v: String) { pricePaidFlow.value = v }
    fun setTimeTaken(v: String) { timeTakenFlow.value = v }
    fun setPartsQuality(v: String) { partsQualityFlow.value = v }
    fun setWouldReturn(v: Boolean) { wouldReturnFlow.value = v }
    fun setText(v: String) { textFlow.value = v }
    fun setVerificationCode(v: String) { verificationCodeFlow.value = v }
    fun clearMessage() { _message.value = null }

    fun submit() {
        // Guest mode: no sign-in wall — the app runs on a per-device guest identity.
        val user = authManager.appUser
        if (_alreadyReviewed.value) {
            _message.value = "You have already reviewed this shop."
            return
        }
        if (starsFlow.value !in 1..5) {
            _message.value = "Please tap the stars to rate this shop."
            return
        }
        if (repairTypeFlow.value.isBlank()) {
            _message.value = "Please choose the repair type."
            return
        }
        if (textFlow.value.isBlank()) {
            _message.value = "Please write a few words about your repair."
            return
        }
        val shopValue = shop.value ?: run {
            _message.value = "Shop is still loading — please wait a moment."
            return
        }
        val enteredCode = verificationCodeFlow.value.trim()
        val verified = enteredCode.isNotEmpty() && enteredCode == shopValue.verificationCode
        if (enteredCode.isNotEmpty() && !verified) {
            _message.value = "That code doesn't match this shop — review saved without the verified tick."
        }
        val authorName = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@").orEmpty()
        val review = Review(
            shopId = shopId,
            authorUid = user.uid,
            authorName = authorName,
            stars = starsFlow.value,
            repairType = repairTypeFlow.value,
            phoneModel = phoneModelFlow.value.trim(),
            pricePaid = pricePaidFlow.value.trim().toDoubleOrNull()?.takeIf { it >= 0 },
            timeTaken = timeTakenFlow.value.trim(),
            partsQuality = partsQualityFlow.value,
            wouldReturn = wouldReturnFlow.value,
            text = textFlow.value.trim(),
            verified = verified
        )
        _submitting.value = true
        viewModelScope.launch {
            // One review per user per shop: check the live list once before writing.
            val existing = reviewRepository.observeReviewsForShop(shopId)
                .first()
                .any { it.authorUid == user.uid }
            if (existing) {
                _alreadyReviewed.value = true
                _submitting.value = false
                _message.value = "You have already reviewed this shop."
                return@launch
            }
            reviewRepository.writeReview(review)
                .onSuccess { _done.value = true }
                .onFailure { _message.value = it.message ?: "Could not save your review." }
            _submitting.value = false
        }
    }

    init {
        // Pre-check so the screen can immediately explain the one-review rule.
        viewModelScope.launch {
            val uid = authManager.appUser.uid
            val existing = reviewRepository.observeReviewsForShop(shopId)
                .first()
                .any { it.authorUid == uid }
            _alreadyReviewed.value = existing
        }
    }
}
