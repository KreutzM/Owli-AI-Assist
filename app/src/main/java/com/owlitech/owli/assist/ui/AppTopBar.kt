package com.owlitech.owli.assist.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.owlitech.owli.assist.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    @StringRes titleRes: Int,
    canNavigateBack: Boolean,
    onNavigateBack: () -> Unit,
    onOpenVlmSettings: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenHelp: () -> Unit,
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
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.nav_title_privacy_policy)) },
                    onClick = {
                        menuExpanded = false
                        onOpenPrivacyPolicy()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.nav_title_help)) },
                    onClick = {
                        menuExpanded = false
                        onOpenHelp()
                    }
                )
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
