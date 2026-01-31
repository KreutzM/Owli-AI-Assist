package com.owlitech.owli.assist.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.owlitech.owli.assist.R
import com.owlitech.owli.assist.BuildConfig
import androidx.compose.ui.platform.LocalClipboardManager
import kotlinx.coroutines.launch

@Composable
fun AboutScreen() {
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val versionText = stringResource(
        R.string.about_version_format,
        BuildConfig.VERSION_NAME,
        BuildConfig.VERSION_CODE
    )
    val buildText = stringResource(R.string.about_build_format, BuildConfig.BUILD_TYPE)
    val versionInfo = "$versionText\n$buildText"
    val aboutTitle = stringResource(R.string.about_title)
    val aboutAppName = stringResource(R.string.about_app_name)
    val aboutSubtitle = stringResource(R.string.about_subtitle)
    val sectionVersion = stringResource(R.string.about_section_version)
    val sectionShortDescription = stringResource(R.string.about_section_short_description)
    val sectionDevelopment = stringResource(R.string.about_section_development)
    val sectionSupporters = stringResource(R.string.about_section_supporters)
    val sectionPrivacy = stringResource(R.string.about_section_privacy)
    val sectionFeedback = stringResource(R.string.about_section_feedback)
    val shortDescriptionP1 = stringResource(R.string.about_short_description_p1)
    val shortDescriptionP2 = stringResource(R.string.about_short_description_p2)
    val developmentIntro = stringResource(R.string.about_development_intro)
    val developmentTeamItem1 = stringResource(R.string.about_development_team_item_1)
    val feedbackContact = stringResource(R.string.about_feedback_contact)
    val supportersIntro = stringResource(R.string.about_supporters_intro)
    val privacyP1 = stringResource(R.string.about_privacy_p1)
    val privacyP2 = stringResource(R.string.about_privacy_p2)
    val feedbackIntro = stringResource(R.string.about_feedback_intro)
    val copyVersionLabel = stringResource(R.string.about_copy_version_label)
    val copyVersionCopied = stringResource(R.string.about_copy_version_copied)
    val supporters = stringArrayResource(R.array.about_supporters_list)
    val feedbackItems = stringArrayResource(R.array.about_feedback_list)

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            headingItem(aboutTitle)
            item {
                Text(
                    text = aboutAppName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                Text(
                    text = aboutSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic
                )
            }
            sectionHeading(sectionVersion)
            paragraph(versionText)
            paragraph(buildText)
            sectionHeading(sectionShortDescription)
            paragraph(shortDescriptionP1)
            paragraph(shortDescriptionP2)
            sectionHeading(sectionDevelopment)
            paragraph(developmentIntro)
            bullet(developmentTeamItem1)
            paragraph(feedbackContact)
            sectionHeading(sectionSupporters)
            paragraph(supportersIntro)
            items(supporters) { item ->
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            sectionHeading(sectionPrivacy)
            paragraph(privacyP1)
            paragraph(privacyP2)
            sectionHeading(sectionFeedback)
            paragraph(feedbackIntro)
            items(feedbackItems) { item ->
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            item {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(versionInfo))
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = copyVersionCopied
                            )
                        }
                    }
                ) {
                    Text(copyVersionLabel)
                }
            }
        }
    }
}

private fun LazyListScope.headingItem(text: String) {
    item {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() }
        )
    }
}

private fun LazyListScope.sectionHeading(text: String) {
    item {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() }
        )
    }
}

private fun LazyListScope.paragraph(text: String) {
    item {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun LazyListScope.bullet(text: String) {
    item {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
