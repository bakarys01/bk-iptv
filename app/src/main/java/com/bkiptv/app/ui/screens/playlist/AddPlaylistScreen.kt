package com.bkiptv.app.ui.screens.playlist

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bkiptv.app.data.repository.PlaylistRepository
import com.bkiptv.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlaylistScreen(
    viewModel: AddPlaylistViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onScanQR: () -> Unit
) {
    val name by viewModel.name.collectAsState()
    val url by viewModel.url.collectAsState()
    val epgUrl by viewModel.epgUrl.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val success by viewModel.success.collectAsState()

    LaunchedEffect(success) {
        if (success) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajouter une playlist") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("Nom de la playlist") },
                placeholder = { Text("Ma playlist") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Title, contentDescription = null) }
            )

            OutlinedTextField(
                value = url,
                onValueChange = { viewModel.updateUrl(it) },
                label = { Text("URL de la playlist (M3U/M3U8)") },
                placeholder = { Text("https://example.com/playlist.m3u") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = onScanQR) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scanner QR")
                    }
                }
            )

            OutlinedTextField(
                value = epgUrl,
                onValueChange = { viewModel.updateEpgUrl(it) },
                label = { Text("URL EPG (optionnel)") },
                placeholder = { Text("https://example.com/epg.xml") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) }
            )

            error?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Error, contentDescription = null, tint = Error)
                        Text(it, color = Error)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.addPlaylist() },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && url.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = TextOnPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ajouter et synchroniser")
                }
            }
        }
    }
}

@HiltViewModel
class AddPlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _epgUrl = MutableStateFlow("")
    val epgUrl: StateFlow<String> = _epgUrl.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success.asStateFlow()

    fun updateName(value: String) { _name.value = value }
    fun updateUrl(value: String) { _url.value = value }
    fun updateEpgUrl(value: String) { _epgUrl.value = value }

    fun addPlaylist() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val playlistId = playlistRepository.addPlaylist(
                    name = _name.value.trim(),
                    url = _url.value.trim(),
                    epgUrl = _epgUrl.value.trim().takeIf { it.isNotEmpty() }
                )

                // Sync the new playlist
                val result = playlistRepository.syncPlaylist(playlistId)
                if (result.isSuccess) {
                    _success.value = true
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Erreur de synchronisation"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Erreur inconnue"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
