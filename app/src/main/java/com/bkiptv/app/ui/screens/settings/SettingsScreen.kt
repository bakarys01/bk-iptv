package com.bkiptv.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.bkiptv.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onNavigateToProfiles: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres") },
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
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Général",
                    style = MaterialTheme.typography.titleMedium,
                    color = Primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Filled.PlaylistPlay,
                    title = "Playlists",
                    subtitle = "Gérer vos playlists IPTV",
                    onClick = onNavigateToPlaylists
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Filled.Person,
                    title = "Profils",
                    subtitle = "Gérer les profils utilisateurs",
                    onClick = onNavigateToProfiles
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Apparence",
                    style = MaterialTheme.typography.titleMedium,
                    color = Primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                var darkMode by remember { mutableStateOf(true) }
                SettingsSwitch(
                    icon = Icons.Filled.DarkMode,
                    title = "Mode sombre",
                    subtitle = "Activer le thème sombre",
                    checked = darkMode,
                    onCheckedChange = { darkMode = it }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Lecture",
                    style = MaterialTheme.typography.titleMedium,
                    color = Primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                var autoPlay by remember { mutableStateOf(true) }
                SettingsSwitch(
                    icon = Icons.Filled.PlayArrow,
                    title = "Lecture automatique",
                    subtitle = "Démarrer la lecture automatiquement",
                    checked = autoPlay,
                    onCheckedChange = { autoPlay = it }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "À propos",
                    style = MaterialTheme.typography.titleMedium,
                    color = Primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Filled.Info,
                    title = "Version",
                    subtitle = "1.0.0",
                    onClick = {}
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        ListItem(
            headlineContent = { Text(title, color = TextPrimary) },
            supportingContent = { Text(subtitle, color = TextSecondary) },
            leadingContent = { Icon(icon, contentDescription = null, tint = Primary) },
            trailingContent = {
                Icon(
                    Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextTertiary
                )
            },
            colors = ListItemDefaults.colors(containerColor = SurfaceDark)
        )
    }
}

@Composable
private fun SettingsSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
        ListItem(
            headlineContent = { Text(title, color = TextPrimary) },
            supportingContent = { Text(subtitle, color = TextSecondary) },
            leadingContent = { Icon(icon, contentDescription = null, tint = Primary) },
            trailingContent = {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                )
            },
            colors = ListItemDefaults.colors(containerColor = SurfaceDark)
        )
    }
}
