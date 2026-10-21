package com.shahab.fixadvisor.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.shahab.fixadvisor.FixAdvisorApp
import com.shahab.fixadvisor.data.models.RepairTypes
import com.shahab.fixadvisor.ui.components.FixAdvisorBottomBar
import com.shahab.fixadvisor.ui.components.SectionTitle
import com.shahab.fixadvisor.ui.navigation.Routes
import com.shahab.fixadvisor.ui.viewmodel.RegisterShopViewModel
import com.shahab.fixadvisor.ui.viewmodel.VmFactory
import kotlinx.coroutines.launch

/**
 * Shop registration: name, address, contacts, services + prices, opening
 * hours, photos. Phase 1: one shop per user, free to join.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegisterShopScreen(
    app: FixAdvisorApp,
    onRegistered: (String) -> Unit,
    onHome: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val vm: RegisterShopViewModel = viewModel(factory = VmFactory(app))
    val snackbarHostState = remember { SnackbarHostState() }

    val message by vm.message.collectAsState()
    val submitting by vm.submitting.collectAsState()
    val registeredShopId by vm.registeredShopId.collectAsState()

    val name by vm.nameState.collectAsState()
    val address by vm.addressState.collectAsState()
    val phone by vm.phoneState.collectAsState()
    val whatsapp by vm.whatsappState.collectAsState()
    val websiteUrl by vm.websiteUrlState.collectAsState()
    val googleMapsUrl by vm.googleMapsUrlState.collectAsState()
    val openingHours by vm.openingHoursState.collectAsState()
    val services by vm.services.collectAsState()
    val prices by vm.prices.collectAsState()
    val photoUris by vm.photoUris.collectAsState()

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearMessage()
        }
    }
    LaunchedEffect(registeredShopId) {
        registeredShopId?.let { onRegistered(it) }
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(RegisterShopViewModel.MAX_PHOTOS)
    ) { uris -> if (uris.isNotEmpty()) vm.addPhotos(uris) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Register your shop") }) },
        bottomBar = {
            FixAdvisorBottomBar(
                currentRoute = Routes.REGISTER_SHOP,
                onHome = onHome,
                onRegisterShop = {},
                onProfile = onOpenProfile
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        // Guest mode: no sign-in wall — the form is always available.

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "It's free to join. Customers will find you by name, area and repair type.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = name, onValueChange = vm::setName,
                label = { Text("Shop name *") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = address, onValueChange = vm::setAddress,
                label = { Text("Address *") }, minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = phone, onValueChange = vm::setPhone,
                    label = { Text("Phone") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = whatsapp, onValueChange = vm::setWhatsapp,
                    label = { Text("WhatsApp number") }, singleLine = true,
                    placeholder = { Text("60123456789") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = websiteUrl, onValueChange = vm::setWebsiteUrl,
                label = { Text("Website link") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = googleMapsUrl, onValueChange = vm::setGoogleMapsUrl,
                label = { Text("Google Maps link") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = openingHours, onValueChange = vm::setOpeningHours,
                label = { Text("Opening hours (e.g. Mon–Sat 10am–9pm)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            SectionTitle("Repair services *")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RepairTypes.ALL.forEach { type ->
                    FilterChip(
                        selected = type in services,
                        onClick = { vm.toggleService(type) },
                        label = { Text(type) }
                    )
                }
            }

            if (services.isNotEmpty()) {
                SectionTitle("Prices (RM, optional)")
                services.sorted().forEach { service ->
                    OutlinedTextField(
                        value = prices[service].orEmpty(),
                        onValueChange = { vm.setPrice(service, it) },
                        label = { Text(service) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }
            }

            SectionTitle("Shop photos (${photoUris.size}/${RegisterShopViewModel.MAX_PHOTOS})")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Card(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.AddAPhoto, contentDescription = "Add photos")
                            Spacer(Modifier.height(4.dp))
                            Text("Add", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                items(photoUris) { uri ->
                    Box {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Shop photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        IconButton(
                            onClick = { vm.removePhoto(uri) },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove photo")
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = vm::submit,
                enabled = !submitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text("Register shop")
            }
            Text(
                text = "One shop per account in this version.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
