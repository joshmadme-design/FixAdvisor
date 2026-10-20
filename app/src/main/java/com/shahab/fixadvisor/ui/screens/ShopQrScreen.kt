package com.shahab.fixadvisor.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shahab.fixadvisor.FixAdvisorApp
import com.shahab.fixadvisor.data.util.QrCodeUtils
import com.shahab.fixadvisor.ui.components.EmptyState
import com.shahab.fixadvisor.ui.components.LoadingBox
import com.shahab.fixadvisor.ui.viewmodel.ShopQrViewModel
import com.shahab.fixadvisor.ui.viewmodel.VmFactory

/**
 * Counter-QR screen (owner-only): a scannable QR code linking to the shop's
 * review page, plus the verification code. The owner prints this and sticks
 * it on the shop counter — customers scan it after a repair and enter the
 * code so their review is marked verified.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopQrScreen(
    app: FixAdvisorApp,
    shopId: String,
    onBack: () -> Unit
) {
    val vm: ShopQrViewModel = viewModel(
        key = "qr_$shopId",
        factory = VmFactory(app, shopId = shopId)
    )
    val shop by vm.shop.collectAsState()
    val qrBitmap by vm.qrBitmap.collectAsState()
    val user by vm.authManager.authState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Counter QR code") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val currentShop = shop
        when {
            currentShop == null -> LoadingBox(Modifier.padding(padding))
            user == null -> EmptyState(
                "Sign in as the shop owner to see the counter QR code.",
                modifier = Modifier.padding(padding)
            )
            user?.uid != currentShop.ownerUid -> EmptyState(
                "Only the shop owner can see this shop's counter QR code.",
                modifier = Modifier.padding(padding)
            )
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentShop.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Got a repair? Rate us!",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(20.dp))

                    val bitmap = qrBitmap
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR code to this shop's review page",
                            modifier = Modifier
                                .size(280.dp)
                                .background(Color.White)
                                .padding(12.dp)
                        )
                    } else {
                        LoadingBox(Modifier.size(280.dp))
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = QrCodeUtils.reviewPageUrl(shopId),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "Your verified-repair code",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currentShop.verificationCode,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = MaterialTheme.typography.displaySmall.letterSpacing
                    )

                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "Print this screen and stick it on your counter. " +
                            "After a repair, tell the customer the code — when they " +
                            "enter it with their review, the review shows a " +
                            "\"Verified repair\" tick, so everyone knows it's real.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
