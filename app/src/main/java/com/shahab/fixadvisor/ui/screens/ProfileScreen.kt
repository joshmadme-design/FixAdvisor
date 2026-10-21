package com.shahab.fixadvisor.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shahab.fixadvisor.FixAdvisorApp
import com.shahab.fixadvisor.data.models.Review
import com.shahab.fixadvisor.ui.components.EmptyState
import com.shahab.fixadvisor.ui.components.FixAdvisorBottomBar
import com.shahab.fixadvisor.ui.components.ReviewCard
import com.shahab.fixadvisor.ui.components.SectionTitle
import com.shahab.fixadvisor.ui.components.ShopCard
import com.shahab.fixadvisor.ui.navigation.Routes
import com.shahab.fixadvisor.ui.viewmodel.ProfileViewModel
import com.shahab.fixadvisor.ui.viewmodel.VmFactory

/**
 * Profile: Google sign-in/out, my reviews (with delete), my favourites,
 * and my shop (detail + counter QR) when registered.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    app: FixAdvisorApp,
    onHome: () -> Unit,
    onRegisterShop: () -> Unit,
    onOpenShop: (String) -> Unit,
    onOpenQr: (String) -> Unit
) {
    val vm: ProfileViewModel = viewModel(factory = VmFactory(app))
    val appUser by vm.appUser.collectAsState()
    val profile by vm.profile.collectAsState()
    val myReviews by vm.myReviews.collectAsState()
    val ownedShop by vm.ownedShop.collectAsState()
    val favoriteShops by vm.favoriteShops.collectAsState()
    val shopNames by vm.shopNames.collectAsState()
    val message by vm.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var deletingReview by remember { mutableStateOf<Review?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Profile") }) },
        bottomBar = {
            FixAdvisorBottomBar(
                currentRoute = Routes.PROFILE,
                onHome = onHome,
                onRegisterShop = onRegisterShop,
                onProfile = {}
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        // Guest mode: no sign-in wall — the profile always shows.

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = profile?.displayName?.ifBlank { appUser.displayName.orEmpty() }
                        ?: appUser.displayName.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = profile?.email ?: appUser.email.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // My shop
            item { SectionTitle("My shop") }
            val myShop = ownedShop
            if (myShop != null) {
                item {
                    ShopCard(shop = myShop, onClick = { onOpenShop(myShop.id) })
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { onOpenQr(myShop.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Counter QR code")
                    }
                }
            } else {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = "Own a phone repair shop?",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Register it free and start collecting reviews.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = onRegisterShop) { Text("Register my shop") }
                        }
                    }
                }
            }

            // Favourites
            item { SectionTitle("Favourite shops") }
            if (favoriteShops.isEmpty()) {
                item {
                    Text(
                        text = "Tap the heart on a shop to save it here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(favoriteShops, key = { "fav_${it.id}" }) { shop ->
                    ShopCard(shop = shop, onClick = { onOpenShop(shop.id) })
                }
            }

            // My reviews
            item { SectionTitle("My reviews (${myReviews.size})") }
            if (myReviews.isEmpty()) {
                item {
                    Text(
                        text = "You haven't reviewed any shop yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(myReviews, key = { "rev_${it.id}" }) { review ->
                    ReviewCard(
                        review = review,
                        shopName = shopNames[review.shopId],
                        showDeleteAction = true,
                        onDelete = { deletingReview = review }
                    )
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = vm::signOut,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign out")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (deletingReview != null) {
        AlertDialog(
            onDismissRequest = { deletingReview = null },
            title = { Text("Delete this review?") },
            text = {
                Text("Your review and its effect on the shop's rating will be removed. This can't be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingReview?.let { vm.deleteReview(it) }
                        deletingReview = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingReview = null }) { Text("Cancel") }
            }
        )
    }
}
