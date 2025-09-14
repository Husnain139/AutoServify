package com.hstan.autoservify.ui.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.hstan.autoservify.model.repositories.ShopRepository
import com.hstan.autoservify.ui.main.Shops.Shop
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch



class HomeFragmentViewModel : ViewModel() {
    private val shopRepository = ShopRepository()

    val failureMessage = MutableStateFlow<String?>(null)
    val data = MutableStateFlow<List<Shop>?>(null)

    init {
        readShops()
    }

    fun readShops() {
        viewModelScope.launch {
            shopRepository.getShops()
                .catch { failureMessage.value = it.message }
                .collect { data.value = it }
        }
    }
}
