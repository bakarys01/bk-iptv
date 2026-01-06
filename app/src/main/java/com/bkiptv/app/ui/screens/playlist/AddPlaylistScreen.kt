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
    val selectedTab by viewModel.selectedTab.collectAsState()
    val name by viewModel.name.collectAsState()
    val url by viewModel.url.collectAsState()
    val epgUrl by viewModel.epgUrl.collectAsState()
    val server by viewModel.server.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
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
        ) {
            // Tabs for M3U vs Xtream
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceDark,
                contentColor = Primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("M3U / URL") },
                    icon = { Icon(Icons.Filled.Link, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("Xtream Codes") },
                    icon = { Icon(Icons.Filled.Api, contentDescription = null) }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Common name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { viewModel.updateName(it) },
                    label = { Text("Nom de la playlist") },
                    placeholder = { Text("Ma playlist") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Title, contentDescription = null) }
                )

                when (selectedTab) {
                    0 -> M3UForm(
                        url = url,
                        epgUrl = epgUrl,
                        onUrlChange = { viewModel.updateUrl(it) },
                        onEpgUrlChange = { viewModel.updateEpgUrl(it) },
                        onScanQR = onScanQR
                    )
                    1 -> XtreamForm(
                        server = server,
                        username = username,
                        password = password,
                        onServerChange = { viewModel.updateServer(it) },
                        onUsernameChange = { viewModel.updateUsername(it) },
                        onPasswordChange = { viewModel.updatePassword(it) }
                    )
                }

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
                    enabled = viewModel.isFormValid() && !isLoading
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
}

@Composable
private fun M3UForm(
    url: String,
    epgUrl: String,
    onUrlChange: (String) -> Unit,
    onEpgUrlChange: (String) -> Unit,
    onScanQR: () -> Unit
) {
    OutlinedTextField(
        value = url,
        onValueChange = onUrlChange,
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
        onValueChange = onEpgUrlChange,
        label = { Text("URL EPG (optionnel)") },
        placeholder = { Text("https://example.com/epg.xml") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) }
    )
}

@Composable
private fun XtreamForm(
    server: String,
    username: String,
    password: String,
    onServerChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit
) {
    OutlinedTextField(
        value = server,
        onValueChange = onServerChange,
        label = { Text("URL du serveur") },
        placeholder = { Text("http://example.com:8080") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Dns, contentDescription = null) }
    )

    OutlinedTextField(
        value = username,
        onValueChange = onUsernameChange,
        label = { Text("Nom d'utilisateur") },
        placeholder = { Text("username") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) }
    )

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text("Mot de passe") },
        placeholder = { Text("••••••••") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) }
    )

    // Help text
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDarkVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = Primary)
            Text(
                "Utilisez vos identifiants Xtream Codes fournis par votre fournisseur IPTV.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@HiltViewModel
class AddPlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    // M3U fields
    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _epgUrl = MutableStateFlow("")
    val epgUrl: StateFlow<String> = _epgUrl.asStateFlow()

    // Xtream fields
    private val _server = MutableStateFlow("")
    val server: StateFlow<String> = _server.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success.asStateFlow()

    fun selectTab(tab: Int) { _selectedTab.value = tab }
    fun updateName(value: String) { _name.value = value }
    fun updateUrl(value: String) { _url.value = value }
    fun updateEpgUrl(value: String) { _epgUrl.value = value }
    fun updateServer(value: String) { _server.value = value }
    fun updateUsername(value: String) { _username.value = value }
    fun updatePassword(value: String) { _password.value = value }

    fun isFormValid(): Boolean {
        return when (_selectedTab.value) {
            0 -> _name.value.isNotBlank() && _url.value.isNotBlank()
            1 -> _name.value.isNotBlank() && _server.value.isNotBlank() && 
                 _username.value.isNotBlank() && _password.value.isNotBlank()
            else -> false
        }
    }

    fun addPlaylist() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val playlistId = when (_selectedTab.value) {
                    0 -> {
                        // M3U playlist
                        playlistRepository.addPlaylist(
                            name = _name.value.trim(),
                            url = _url.value.trim(),
                            epgUrl = _epgUrl.value.trim().takeIf { it.isNotEmpty() }
                        )
                    }
                    1 -> {
                        // Xtream playlist
                        playlistRepository.addXtreamPlaylist(
                            name = _name.value.trim(),
                            server = _server.value.trim(),
                            username = _username.value.trim(),
                            password = _password.value.trim()
                        )
                    }
                    else -> throw Exception("Type de playlist invalide")
                }

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
