package com.shahab.fixadvisor.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shahab.fixadvisor.FixAdvisorApp
import com.shahab.fixadvisor.data.models.PartsQuality
import com.shahab.fixadvisor.data.models.RepairTypes
import com.shahab.fixadvisor.ui.components.EmptyState
import com.shahab.fixadvisor.ui.components.StarsInput
import com.shahab.fixadvisor.ui.viewmodel.VmFactory
import com.shahab.fixadvisor.ui.viewmodel.WriteReviewViewModel
import kotlinx.coroutines.launch

/**
 * Review form with the repair details that set FixAdvisor apart:
 * repair type, phone model, price paid, time taken, parts quality,
 * would-return — plus an optional shop verification code that marks
 * the review as "verified".
 *
 * Requires Google sign-in. Phase 1 rule: one review per user per shop.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WriteReviewScreen(
    app: FixAdvisorApp,
    shopId: String,
    code: String?,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val vm: WriteReviewViewModel = viewModel(
        key = "review_$shopId",
        factory = VmFactory(app, shopId = shopId, prefilledCode = code)
    )
    val snackbarHostState = remember { SnackbarHostState() }

    val shop by vm.shop.collectAsState()
    val alreadyReviewed by vm.alreadyReviewed.collectAsState()
    val submitting by vm.submitting.collectAsState()
    val done by vm.done.collectAsState()
    val message by vm.message.collectAsState()

    val stars by vm.starsState.collectAsState()
    val repairType by vm.repairTypeState.collectAsState()
    val phoneModel by vm.phoneModelState.collectAsState()
    val pricePaid by vm.pricePaidState.collectAsState()
    val timeTaken by vm.timeTakenState.collectAsState()
    val partsQuality by vm.partsQualityState.collectAsState()
    val wouldReturn by vm.wouldReturnState.collectAsState()
    val text by vm.textState.collectAsState()
    val verificationCode by vm.verificationCodeState.collectAsState()

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearMessage()
        }
    }
    LaunchedEffect(done) {
        if (done) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review ${shop?.name ?: "shop"}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        // Guest mode: no sign-in wall.
        when {
            alreadyReviewed -> {
                EmptyState(
                    "You have already reviewed this shop.\nOne review per customer per shop keeps ratings honest.",
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = "How was your repair?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    StarsInput(value = stars, onChange = vm::setStars)

                    Text("Repair type *", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        RepairTypes.ALL.forEach { type ->
                            FilterChip(
                                selected = repairType == type,
                                onClick = { vm.setRepairType(if (repairType == type) "" else type) },
                                label = { Text(type) }
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = phoneModel,
                        onValueChange = vm::setPhoneModel,
                        label = { Text("Phone model (e.g. iPhone 13)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = pricePaid,
                            onValueChange = vm::setPricePaid,
                            label = { Text("Price paid (RM)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = timeTaken,
                            onValueChange = vm::setTimeTaken,
                            label = { Text("Time taken (e.g. 1 hour)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("Parts quality", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        PartsQuality.ALL.forEach { quality ->
                            FilterChip(
                                selected = partsQuality == quality,
                                onClick = { vm.setPartsQuality(if (partsQuality == quality) "" else quality) },
                                label = { Text(quality) }
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "I would return to this shop",
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = wouldReturn, onCheckedChange = vm::setWouldReturn)
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = text,
                        onValueChange = vm::setText,
                        label = { Text("Your review *") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = verificationCode,
                        onValueChange = vm::setVerificationCode,
                        label = { Text("Shop code (optional)") },
                        placeholder = { Text("Code the shop gave you after the repair") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = {
                            Text("A matching code marks your review as verified.")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))
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
                        Text("Post review")
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
