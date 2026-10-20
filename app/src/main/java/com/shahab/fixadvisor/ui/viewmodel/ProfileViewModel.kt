package com.shahab.fixadvisor.ui.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shahab.fixadvisor.FixAdvisorApp
import com.shahab.fixadvisor.data.models.Review
import com.shahab.fixadvisor.data.models.Shop
import com.shahab.fixadvisor.data.models.UserProfile
import com.shahab.fixadvisor.data.repo.ShopRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Profile: sign-in state, my reviews (with delete), my favourites, my shop.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val app: FixAdvisorApp
) : ViewModel() {

    private val authManager = app.container.authManager
    private val userRepository = app.container.userRepository
    private val reviewRepository = app.container.reviewRepository
    private val shopRepository: ShopRepository = app.container.shopRepository

    val authState = authManager.authState

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val profile: StateFlow<UserProfile?> = authState.flatMapLatest { user ->
        if (user == null) flowOf(null) else userRepository.observeProfile(user.uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val myReviews: StateFlow<List<Review>> = authState.flatMapLatest { user ->
        if (user == null) flowOf(emptyList())
        else reviewRepository.observeMyReviews(user.uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Shop id -> shop name, so "My reviews" can say which shop each review is for. */
    val shopNames: StateFlow<Map<String, String>> = shopRepository.observeShops()
        .map { list -> list.associate { it.id to it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val ownedShop: StateFlow<Shop?> = profile.flatMapLatest { p ->
        val id = p?.ownedShopId
        if (id == null) flowOf(null)
        else shopRepository.observeShops().map { list -> list.find { it.id == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val favoriteShops: StateFlow<List<Shop>> = profile.flatMapLatest { p ->
        val ids = p?.favoriteShopIds.orEmpty()
        if (ids.isEmpty()) flowOf(emptyList())
        else shopRepository.observeShops().map { list -> list.filter { it.id in ids } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clearMessage() { _message.value = null }

    fun getSignInIntent(): Intent = authManager.getSignInIntent()

    /** Completes Google sign-in and creates the user profile on first login. */
    fun handleSignInResult(data: Intent?) {
        viewModelScope.launch {
            val result = authManager.handleSignInResult(data)
            result.onSuccess { user ->
                runCatching { userRepository.ensureProfile(user) }
                    .onFailure { _message.value = "Signed in, but profile setup failed." }
            }.onFailure {
                _message.value = it.message ?: "Sign-in failed."
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching { authManager.signOut() }
                .onFailure { _message.value = "Sign-out failed." }
        }
    }

    fun deleteReview(review: Review) {
        viewModelScope.launch {
            reviewRepository.deleteReview(review)
                .onSuccess { _message.value = "Review deleted." }
                .onFailure { _message.value = it.message ?: "Could not delete the review." }
        }
    }

    fun removeFavorite(shopId: String) {
        val uid = authManager.currentUid ?: return
        viewModelScope.launch {
            runCatching { userRepository.removeFavorite(uid, shopId) }
                .onFailure { _message.value = "Could not remove favourite." }
        }
    }
}
