package com.hstan.autoservify.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.hstan.autoservify.model.repositories.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel: ViewModel() {

    private val authRepository = AuthRepository()
    private val db = FirebaseFirestore.getInstance()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> get() = _currentUser

    private val _userName = MutableStateFlow<String>("")
    val userName: StateFlow<String> get() = _userName

    val failureMessage = MutableStateFlow<String?>(null)
    val resetResponse = MutableStateFlow<Boolean?>(null)

    fun login(email: String, password: String) {
        viewModelScope.launch {
            val result = authRepository.login(email, password)
            if (result.isSuccess) {
                _currentUser.value = result.getOrThrow()
                fetchUserName()
            } else {
                failureMessage.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            val result = authRepository.resetPassword(email)
            if (result.isSuccess) {
                resetResponse.value = result.getOrThrow()
            } else {
                failureMessage.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun signUp(email: String, password: String, name: String) {
        viewModelScope.launch {
            val result = authRepository.signup(email, password, name)
            if (result.isSuccess) {
                _currentUser.value = result.getOrThrow()
                _userName.value = name  // Store name after signup
                fetchUserName()
            } else {
                failureMessage.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun checkUser() {
        _currentUser.value = authRepository.getCurrentUser()
        if (_currentUser.value != null) {
            fetchUserName()
        }
    }

    private fun fetchUserName() {
        val user = _currentUser.value
        if (user != null) {
            _userName.value = user.displayName ?: "Unknown"
        }
    }
}

