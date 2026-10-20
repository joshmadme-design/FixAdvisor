package com.shahab.fixadvisor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.shahab.fixadvisor.FixAdvisorApp

/**
 * Single factory for all ViewModels. Pass [shopId] for shop-scoped screens.
 */
class VmFactory(
    private val app: FixAdvisorApp,
    private val shopId: String? = null,
    private val prefilledCode: String? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val container = app.container
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(container.shopRepository)
            modelClass.isAssignableFrom(ShopDetailViewModel::class.java) ->
                ShopDetailViewModel(
                    container.shopRepository,
                    container.reviewRepository,
                    container.userRepository,
                    container.authManager,
                    requireNotNull(shopId) { "shopId required" }
                )
            modelClass.isAssignableFrom(WriteReviewViewModel::class.java) ->
                WriteReviewViewModel(
                    container.reviewRepository,
                    container.shopRepository,
                    container.authManager,
                    requireNotNull(shopId) { "shopId required" },
                    prefilledCode
                )
            modelClass.isAssignableFrom(RegisterShopViewModel::class.java) ->
                RegisterShopViewModel(
                    container.shopRepository,
                    container.userRepository,
                    container.authManager
                )
            modelClass.isAssignableFrom(ShopQrViewModel::class.java) ->
                ShopQrViewModel(
                    container.shopRepository,
                    container.authManager,
                    requireNotNull(shopId) { "shopId required" }
                )
            modelClass.isAssignableFrom(ProfileViewModel::class.java) ->
                ProfileViewModel(app)
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        } as T
    }
}
