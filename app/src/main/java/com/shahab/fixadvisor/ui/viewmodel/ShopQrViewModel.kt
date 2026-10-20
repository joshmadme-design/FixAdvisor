package com.shahab.fixadvisor.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shahab.fixadvisor.data.models.Shop
import com.shahab.fixadvisor.data.repo.AuthManager
import com.shahab.fixadvisor.data.repo.ShopRepository
import com.shahab.fixadvisor.data.util.QrCodeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Counter-QR screen: renders the scannable QR for the shop's review page plus
 * the verification code, for the owner to print as a counter sticker.
 */
class ShopQrViewModel(
    shopRepository: ShopRepository,
    val authManager: AuthManager,
    private val shopId: String
) : ViewModel() {

    val shop: StateFlow<Shop?> = shopRepository.observeShops()
        .map { list -> list.find { it.id == shopId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _qrBitmap = MutableStateFlow<Bitmap?>(null)
    val qrBitmap: StateFlow<Bitmap?> = _qrBitmap.asStateFlow()

    init {
        viewModelScope.launch {
            _qrBitmap.value = withContext(Dispatchers.Default) {
                QrCodeUtils.makeQrBitmap(QrCodeUtils.reviewPageUrl(shopId))
            }
        }
    }
}
