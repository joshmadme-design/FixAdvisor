package com.shahab.fixadvisor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shahab.fixadvisor.data.models.Shop
import com.shahab.fixadvisor.data.models.ShopSort
import com.shahab.fixadvisor.data.repo.ShopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Home: live shop list with search text, repair-type filter and sort order. */
class HomeViewModel(private val repository: ShopRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _repairType = MutableStateFlow<String?>(null)
    val repairType: StateFlow<String?> = _repairType.asStateFlow()

    private val _sort = MutableStateFlow(ShopSort.TOP_RATED)
    val sort: StateFlow<ShopSort> = _sort.asStateFlow()

    val shops: StateFlow<List<Shop>> = combine(
        repository.observeShops(), _query, _repairType, _sort
    ) { all, q, type, sortOrder ->
        repository.filterAndSort(all, q, type, sortOrder)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) { _query.value = value }
    fun setRepairType(value: String?) { _repairType.value = value }
    fun setSort(value: ShopSort) { _sort.value = value }
}
