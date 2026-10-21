package com.shahab.fixadvisor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shahab.fixadvisor.data.models.Review
import com.shahab.fixadvisor.data.models.Shop
import com.shahab.fixadvisor.data.repo.AuthManager
import com.shahab.fixadvisor.data.repo.ReviewRepository
import com.shahab.fixadvisor.data.repo.ShopRepository
import com.shahab.fixadvisor.data.repo.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Shop detail: live shop, its reviews, favourite state and owner actions. */
@OptIn(ExperimentalCoroutinesApi::class)
class ShopDetailViewModel(
    private val shopRepository: ShopRepository,
    private val reviewRepository: ReviewRepository,
    private val userRepository: UserRepository,
    val authManager: AuthManager,
    private val shopId: String
) : ViewModel() {

    val shop: StateFlow<Shop?> = shopRepository.observeShops()
        .map { list -> list.find { it.id == shopId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val reviews: StateFlow<List<Review>> = reviewRepository.observeReviewsForShop(shopId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isOwner: StateFlow<Boolean> = combine(authManager.authState, shop) { user, s ->
        user != null && s != null && user.uid == s.ownerUid
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val favoriteIds: Flow<Set<String>> = authManager.authState.flatMapLatest { user ->
        if (user == null) flowOf(emptySet())
        else userRepository.observeProfile(user.uid).map { it?.favoriteShopIds?.toSet().orEmpty() }
    }

    val isFavorite: StateFlow<Boolean> = favoriteIds
        .map { shopId in it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() { _message.value = null }

    fun toggleFavorite() {
        val uid = authManager.appUser.uid
        viewModelScope.launch {
            runCatching {
                if (isFavorite.value) userRepository.removeFavorite(uid, shopId)
                else userRepository.addFavorite(uid, shopId)
            }.onFailure { _message.value = it.message ?: "Could not update favourites." }
        }
    }

    fun submitReply(reviewId: String, reply: String) {
        viewModelScope.launch {
            reviewRepository.replyToReview(reviewId, reply)
                .onSuccess { _message.value = "Reply posted." }
                .onFailure { _message.value = it.message ?: "Could not post reply." }
        }
    }
}
