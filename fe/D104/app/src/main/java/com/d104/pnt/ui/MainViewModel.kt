package com.d104.pnt.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = authRepository.isLoggedIn()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,  // 즉시 시작
            initialValue = false
        )
}