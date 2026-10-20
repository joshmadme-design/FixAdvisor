package com.shahab.fixadvisor.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.shahab.fixadvisor.FixAdvisorApp
import com.shahab.fixadvisor.data.models.Review
import com.shahab.fixadvisor.data.util.formatPriceRm
import com.shahab.fixadvisor.ui.components.EmptyState
import com.shahab.fixadvisor.ui.components.LoadingBox
import com.shahab.fixadvisor.ui.components.ReviewCard
import com.shahab.fixadvisor.ui.components.SectionTitle
import com.shahab.fixadvisor.ui.components.StarsDisplay
import com.shahab.fixadvisor.ui.components.openDialer
import com.shahab.fixadvisor.ui.components.openDirections
import com.shahab.fixadvisor.ui.components.openUrl
import com.shahab.fixadvisor.ui.components.openWhatsApp
import com.shahab.fixadvisor.ui.viewmodel.ShopDetailViewModel
import com.shahab.fixadvisor.ui.viewmodel.VmFactory

/**
 * Shop detail: photos, contact actions, services + price list, opening hours,
 * reviews with repair details, owner replies and (for the owner) the
 * counter-QR entry point.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopDetailScreen(
    app: FixAdvisorApp,
    shopId: String,
    onBack: () -> Unit,
    onWriteReview: (String) -> Unit,
    onOpenQr: (String) -> Unit
) {
    val vm: ShopDetailViewModel = viewModel(
        key = "detail_$shopId",
        factory = VmFactory(app, shopId = shopId)
    )
    val shop by vm.shop.collectAsState()
    val reviews by vm.reviews.collectAsState()
    val isOwner by vm.isOwner.collectAsState()
    val isFavorite by vm.isFavorite.collectAsState()
    val message by vm.message.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var replyTarget by remember { mutableStateOf<Review?>(null) }
    var replyText by remember { mutableStateOf("") }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(shop?.name ?: "Shop") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = vm::toggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Save to favourites"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val currentShop = shop
        if (currentShop == null) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                // Photos
                if (currentShop.photoUrls.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        currentShop.photoUrls.forEach { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = currentShop.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 260.dp, height = 160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = currentShop.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                StarsDisplay(
                    rating = currentShop.averageRating,
                    reviewCount = currentShop.reviewCount,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.height(6.dp))
                Row {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = currentShop.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (currentShop.openingHours.isNotBlank()) {
                    Row(Modifier.padding(top = 4.dp)) {
                        Icon(
                            Icons.Filled.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = currentShop.openingHours,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Contact actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentShop.phone.isNotBlank()) {
                        AssistChip(
                            onClick = { openDialer(context, currentShop.phone) },
                            label = { Text("Call") },
                            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, Modifier.size(18.dp)) }
                        )
                    }
                    if (currentShop.whatsapp.isNotBlank()) {
                        AssistChip(
                            onClick = { openWhatsApp(context, currentShop.whatsapp, currentShop.name) },
                            label = { Text("WhatsApp") },
                            leadingIcon = { Icon(Icons.Filled.Chat, contentDescription = null, Modifier.size(18.dp)) }
                        )
                    }
                    AssistChip(
                        onClick = { openDirections(context, currentShop.address, currentShop.googleMapsUrl) },
                        label = { Text("Directions") },
                        leadingIcon = { Icon(Icons.Filled.Directions, contentDescription = null, Modifier.size(18.dp)) }
                    )
                    if (currentShop.websiteUrl.isNotBlank()) {
                        AssistChip(
                            onClick = { openUrl(context, currentShop.websiteUrl) },
                            label = { Text("Website") },
                            leadingIcon = { Icon(Icons.Filled.Language, contentDescription = null, Modifier.size(18.dp)) }
                        )
                    }
                    if (currentShop.googleMapsUrl.isNotBlank()) {
                        AssistChip(
                            onClick = { openUrl(context, currentShop.googleMapsUrl) },
                            label = { Text("Google Maps") },
                            leadingIcon = { Icon(Icons.Filled.Map, contentDescription = null, Modifier.size(18.dp)) }
                        )
                    }
                }

                // Services + price list
                if (currentShop.services.isNotEmpty()) {
                    SectionTitle("Services & prices")
                    currentShop.services.forEach { service ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Text(
                                text = service,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = currentShop.prices[service]?.let { formatPriceRm(it) } ?: "Ask in shop",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (isOwner) {
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { onOpenQr(currentShop.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.QrCode2, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Get my counter QR code")
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onWriteReview(currentShop.id) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isOwner
                ) {
                    Icon(Icons.Filled.RateReview, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isOwner) "You own this shop" else "Write a review")
                }

                SectionTitle("Reviews (${reviews.size})")
            }

            if (reviews.isEmpty()) {
                item {
                    EmptyState("No reviews yet. Be the first to review this shop.")
                }
            } else {
                items(reviews, key = { it.id }) { review ->
                    ReviewCard(
                        review = review,
                        showOwnerReplyAction = isOwner,
                        onReply = {
                            replyTarget = review
                            replyText = review.ownerReply
                        },
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    // Owner reply dialog
    if (replyTarget != null) {
        AlertDialog(
            onDismissRequest = { replyTarget = null },
            title = { Text("Reply to review") },
            text = {
                Column {
                    Text(
                        text = "Your reply is public. The review itself cannot be changed or removed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = { Text("Your reply") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = replyTarget ?: return@TextButton
                        vm.submitReply(target.id, replyText)
                        replyTarget = null
                    },
                    enabled = replyText.isNotBlank()
                ) { Text("Post reply") }
            },
            dismissButton = {
                TextButton(onClick = { replyTarget = null }) { Text("Cancel") }
            }
        )
    }
}
