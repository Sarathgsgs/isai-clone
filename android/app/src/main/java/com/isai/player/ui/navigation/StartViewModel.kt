package com.isai.player.ui.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class StartViewModel @Inject constructor() : ViewModel() {

    private val _startDestination = MutableStateFlow<NavRoute>(NavRoute.Main)
    val startDestination: StateFlow<NavRoute> = _startDestination.asStateFlow()
}

