package com.touristapp.presentation.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.touristapp.di.ServiceLocator

class AuthViewModelFactory(
    context: Context
) : ViewModelProvider.Factory {

    init {
        ServiceLocator.initialize(context)
    }

    private val repository = ServiceLocator.authRepository

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}