package com.hstan.autoservify.ui.main.Shops

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hstan.autoservify.model.repositories.ShopRepository
import com.hstan.autoservify.ui.main.Shops.Shop
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AddShopViewModel : ViewModel() {

    private val shopRepository = ShopRepository()

    val isSuccessfullySaved = MutableStateFlow<Boolean?>(null)
    val failureMessage = MutableStateFlow<String?>(null)

    fun saveShop(shop: Shop) {
        viewModelScope.launch {
            val result = shopRepository.saveShop(shop)
            if (result.isSuccess) {
                isSuccessfullySaved.value = true
            } else {
                failureMessage.value = result.exceptionOrNull()?.message
            }
        }
    }
}