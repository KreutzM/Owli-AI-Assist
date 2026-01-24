package com.owlitech.owli.assist.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.annotation.StringRes
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import com.owlitech.owli.assist.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    @StringRes titleRes: Int,
    canNavigateBack: Boolean,
    showVlmAction: Boolean,
    detectorModeEnabled: Boolean,
    onNavigateBack: () -> Unit,
    onOpenVlm: () -> Unit,
    onOpenDetector: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenVlmSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenAbout: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(stringResource(titleRes)) },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.menu_back_content_description)
                    )
                }
            }
        },
        actions = {
            if (showVlmAction) {
                TextButton(onClick = onOpenVlm) {
                    Text(stringResource(R.string.menu_vlm))
                }
            }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.menu_more_content_description)
                )
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.nav_title_vlm_settings)) },
                    onClick = {
                        menuExpanded = false
                        onOpenVlmSettings()
                    }
                )
                if (detectorModeEnabled) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.nav_title_offline_detector)) },
                        onClick = {
                            menuExpanded = false
                            onOpenDetector()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.nav_title_settings)) },
                        onClick = {
                            menuExpanded = false
                            onOpenSettings()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.nav_title_diagnostics)) },
                        onClick = {
                            menuExpanded = false
                            onOpenDiagnostics()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.nav_title_about)) },
                    onClick = {
                        menuExpanded = false
                        onOpenAbout()
                    }
                )
            }
        }
    )
}
