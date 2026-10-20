package com.shahab.fixadvisor.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shahab.fixadvisor.FixAdvisorApp
import com.shahab.fixadvisor.data.models.RepairTypes
import com.shahab.fixadvisor.data.models.ShopSort
import com.shahab.fixadvisor.ui.components.EmptyState
import com.shahab.fixadvisor.ui.components.FixAdvisorBottomBar
import com.shahab.fixadvisor.ui.components.ShopCard
import com.shahab.fixadvisor.ui.navigation.Routes
import com.shahab.fixadvisor.ui.viewmodel.HomeViewModel
import com.shahab.fixadvisor.ui.viewmodel.VmFactory

/**
 * Home: search shops by text, filter by repair type, sort by rating.
 * Browsing is fully anonymous — no account needed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    app: FixAdvisorApp,
    onOpenShop: (String) -> Unit,
    onRegisterShop: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val vm: HomeViewModel = viewModel(factory = VmFactory(app))
    val shops by vm.shops.collectAsState()
    val query by vm.query.collectAsState()
    val repairType by vm.repairType.collectAsState()
    val sort by vm.sort.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("FixAdvisor — phone repair reviews") }) },
        bottomBar = {
            FixAdvisorBottomBar(
                currentRoute = Routes.HOME,
                onHome = {},
                onRegisterShop = onRegisterShop,
                onProfile = onOpenProfile
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                placeholder = { Text("Search shop, area or repair…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Repair-type filter
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = repairType == null,
                        onClick = { vm.setRepairType(null) },
                        label = { Text("All repairs") }
                    )
                }
                items(RepairTypes.ALL) { type ->
                    FilterChip(
                        selected = repairType == type,
                        onClick = { vm.setRepairType(if (repairType == type) null else type) },
                        label = { Text(type) }
                    )
                }
            }

            // Sort order
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = sort == ShopSort.TOP_RATED,
                    onClick = { vm.setSort(ShopSort.TOP_RATED) },
                    label = { Text("Top rated") }
                )
                FilterChip(
                    selected = sort == ShopSort.MOST_REVIEWED,
                    onClick = { vm.setSort(ShopSort.MOST_REVIEWED) },
                    label = { Text("Most reviewed") }
                )
                FilterChip(
                    selected = sort == ShopSort.NEWEST,
                    onClick = { vm.setSort(ShopSort.NEWEST) },
                    label = { Text("Newest") }
                )
            }

            if (shops.isEmpty()) {
                EmptyState(
                    message = if (query.isBlank() && repairType == null) {
                        "No shops registered yet.\nKnow a good repair shop? Be the first to add one."
                    } else {
                        "No shops match your search."
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(shops, key = { it.id }) { shop ->
                        ShopCard(shop = shop, onClick = { onOpenShop(shop.id) })
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}
