package com.shahab.fixadvisor.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseUser
import com.shahab.fixadvisor.data.models.Review
import com.shahab.fixadvisor.data.models.Shop
import com.shahab.fixadvisor.data.repo.AuthManager
import com.shahab.fixadvisor.data.util.formatDate
import com.shahab.fixadvisor.data.util.formatPriceRm
import com.shahab.fixadvisor.data.util.formatRating
import com.shahab.fixadvisor.data.util.normalizeUrl
import com.shahab.fixadvisor.ui.navigation.Routes
import com.shahab.fixadvisor.ui.theme.StarAmber
import com.shahab.fixadvisor.ui.theme.VerifiedGreen
import kotlinx.coroutines.launch

// ---------------- Navigation ----------------

/** Bottom bar shared by the three top-level tabs (Home / Register / Profile). */
@Composable
fun FixAdvisorBottomBar(
    currentRoute: String,
    onHome: () -> Unit,
    onRegisterShop: () -> Unit,
    onProfile: () -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Routes.HOME,
            onClick = onHome,
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = currentRoute == Routes.REGISTER_SHOP,
            onClick = onRegisterShop,
            icon = { Icon(Icons.Filled.Storefront, contentDescription = "Add shop") },
            label = { Text("Add shop") }
        )
        NavigationBarItem(
            selected = currentRoute == Routes.PROFILE,
            onClick = onProfile,
            icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
            label = { Text("Profile") }
        )
    }
}

// ---------------- Ratings ----------------

/** Read-only star row + numeric rating + review count. */
@Composable
fun StarsDisplay(rating: Double, reviewCount: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        val filled = rating.toInt().coerceIn(0, 5)
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= filled) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = StarAmber,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (reviewCount == 0) "No reviews yet"
            else "${formatRating(rating)} ($reviewCount ${if (reviewCount == 1) "review" else "reviews"})",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/** Tappable 1–5 star input. */
@Composable
fun StarsInput(value: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 1..5) {
            IconButton(onClick = { onChange(i) }, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = if (i <= value) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "$i stars",
                    tint = StarAmber,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

// ---------------- Sign-in ----------------

/**
 * "Sign in with Google" button wired to [AuthManager]. On success the caller
 * typically creates the user profile (UserRepository.ensureProfile).
 */
@Composable
fun GoogleSignInButton(
    authManager: AuthManager,
    onSignedIn: (FirebaseUser) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                authManager.handleSignInResult(result.data)
                    .onSuccess(onSignedIn)
                    .onFailure { onError(it.message ?: "Sign-in failed.") }
            }
        }
        // A cancelled sign-in needs no message.
    }
    Button(
        onClick = { launcher.launch(authManager.getSignInIntent()) },
        modifier = modifier
    ) {
        Text("Sign in with Google")
    }
}

/** Card shown when an action needs an account. */
@Composable
fun SignInPrompt(
    authManager: AuthManager,
    onSignedIn: (FirebaseUser) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Sign in to continue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "You can browse shops without an account. " +
                    "Writing reviews and registering a shop need a Google sign-in.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            GoogleSignInButton(
                authManager = authManager,
                onSignedIn = onSignedIn,
                onError = onError
            )
        }
    }
}

// ---------------- States ----------------

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

// ---------------- Shop / review cards ----------------

@Composable
fun ShopCard(shop: Shop, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row {
                if (shop.photoUrls.isNotEmpty()) {
                    AsyncImage(
                        model = shop.photoUrls.first(),
                        contentDescription = shop.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = shop.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    StarsDisplay(rating = shop.averageRating, reviewCount = shop.reviewCount)
                    Text(
                        text = shop.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
            if (shop.services.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = shop.services.take(4).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ReviewCard(
    review: Review,
    modifier: Modifier = Modifier,
    shopName: String? = null,
    showOwnerReplyAction: Boolean = false,
    showDeleteAction: Boolean = false,
    onReply: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = review.authorName.ifBlank { "Anonymous" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (review.verified) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Verified repair",
                        tint = VerifiedGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Verified repair",
                        style = MaterialTheme.typography.labelSmall,
                        color = VerifiedGreen
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatDate(review.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (shopName != null) {
                Text(
                    text = shopName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(4.dp))
            Row {
                for (i in 1..5) {
                    Icon(
                        imageVector = if (i <= review.stars) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = null,
                        tint = StarAmber,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = review.repairType,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
            val detailParts = buildList {
                if (review.phoneModel.isNotBlank()) add(review.phoneModel)
                review.pricePaid?.let { add("Paid ${formatPriceRm(it)}") }
                if (review.timeTaken.isNotBlank()) add("Took ${review.timeTaken}")
                if (review.partsQuality.isNotBlank()) add("Parts: ${review.partsQuality}")
                add(if (review.wouldReturn) "Would return" else "Would NOT return")
            }
            Text(
                text = detailParts.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (review.text.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(text = review.text, style = MaterialTheme.typography.bodyMedium)
            }
            if (review.ownerReply.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text = "Owner's reply",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(text = review.ownerReply, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if ((showOwnerReplyAction && onReply != null) || (showDeleteAction && onDelete != null)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (showDeleteAction && onDelete != null) {
                        TextButton(onClick = onDelete) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Delete")
                        }
                    }
                    if (showOwnerReplyAction && onReply != null) {
                        TextButton(onClick = onReply) {
                            Icon(Icons.Filled.Reply, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (review.ownerReply.isBlank()) "Reply" else "Edit reply")
                        }
                    }
                }
            }
        }
    }
}

// ---------------- External intents ----------------

/** Opens the dialer with the number filled in (no CALL_PHONE permission). */
fun openDialer(context: Context, phone: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.trim()}"))
    runCatching { context.startActivity(intent) }
}

/** Opens WhatsApp chat for the shop (number must be intl format, digits only). */
fun openWhatsApp(context: Context, number: String, shopName: String) {
    val text = Uri.encode("Hi $shopName, I found you on FixAdvisor. I need a phone repair.")
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$number?text=$text"))
    runCatching { context.startActivity(intent) }
}

/** Opens a maps app with directions to the shop address. */
fun openDirections(context: Context, address: String, fallbackMapsUrl: String) {
    val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(address)}"))
    val started = runCatching { context.startActivity(geoIntent) }.isSuccess
    if (!started && fallbackMapsUrl.isNotBlank()) {
        openUrl(context, fallbackMapsUrl)
    }
}

/** Opens any http(s) link in the browser. */
fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalizeUrl(url)))
    runCatching { context.startActivity(intent) }
}
