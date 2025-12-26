package com.example.bikebuddy.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

@Composable
fun AppTopBar(
    title: String,
    canNavigateBack: Boolean,
    showVlmAction: Boolean,
    onNavigateBack: () -> Unit,
    onOpenVlm: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenAbout: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurueck")
                }
            }
        },
        actions = {
            if (showVlmAction) {
                TextButton(onClick = onOpenVlm) {
                    Text("VLM")
                }
            }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Menue")
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = {
                        menuExpanded = false
                        onOpenSettings()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Diagnostics") },
                    onClick = {
                        menuExpanded = false
                        onOpenDiagnostics()
                    }
                )
                DropdownMenuItem(
                    text = { Text("About") },
                    onClick = {
                        menuExpanded = false
                        onOpenAbout()
                    }
                )
            }
        }
    )
}
