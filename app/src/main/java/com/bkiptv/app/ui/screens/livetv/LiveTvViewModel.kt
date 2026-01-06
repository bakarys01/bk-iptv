package com.bkiptv.app.ui.screens.livetv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bkiptv.app.data.repository.ChannelRepository
import com.bkiptv.app.db.entity.ChannelEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiveTvViewModel @Inject constructor(
    private val channelRepository: ChannelRepository
) : ViewModel() {

    val channels: StateFlow<List<ChannelEntity>> = channelRepository.getAllChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val countries: StateFlow<List<String>> = channelRepository.getAllCountries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = channelRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCountry = MutableStateFlow<String?>(null)
    val selectedCountry: StateFlow<String?> = _selectedCountry.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.GRID)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val filteredChannels: StateFlow<List<ChannelEntity>> = combine(
        channels,
        _selectedCountry,
        _selectedCategory
    ) { allChannels, country, category ->
        allChannels.filter { channel ->
            (country == null || channel.country == country) &&
            (category == null || channel.category == category || channel.groupTitle == category)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCountry(country: String?) {
        _selectedCountry.value = country
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun toggleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            ViewMode.GRID -> ViewMode.LIST
            ViewMode.LIST -> ViewMode.GRID
        }
    }

    fun toggleFavorite(channelId: Long) {
        viewModelScope.launch {
            channelRepository.toggleFavorite(channelId)
        }
    }
}
