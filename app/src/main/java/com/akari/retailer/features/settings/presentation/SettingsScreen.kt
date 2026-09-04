package com.akari.retailer.features.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akari.retailer.R
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.*
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.core.utils.LanguageManager
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    val activity = context as? androidx.activity.ComponentActivity
    
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(application)
    )
    
    LaunchedEffect(activity) {
        activity?.let { viewModel.setActivity(it) }
    }
    
    val state by viewModel.state.collectAsState()
    
    var showRestartDialog by remember { mutableStateOf(false) }
    var pendingLanguageCode by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.success) {
        if (state.success) {
            delay(1500)
            viewModel.handleEvent(SettingsEvent.ResetSuccess)
        }
    }

    if (showRestartDialog) {
        AppRestartDialog(
            onDismiss = {
                showRestartDialog = false
                pendingLanguageCode = null
            },
            onConfirm = {
                showRestartDialog = false
                pendingLanguageCode?.let { code ->
                    viewModel.handleEvent(SettingsEvent.LanguageSelected(code))
                }
                pendingLanguageCode = null
            }
        )
    }

    AppScreen(
        title = stringResource(R.string.settings_title),
        showBackButton = true,
        onBackClick = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Language Section
            AppSettingsSection(
                title = stringResource(R.string.language)
            ) {
                LanguageManager.getAvailableLanguages().forEach { option ->
                    val displayName = if (option.code == "en") {
                        stringResource(R.string.english)
                    } else {
                        stringResource(R.string.myanmar)
                    }
                    
                    AppListItem(
                        text = "${option.flag} $displayName",
                        isSelected = state.currentLanguage == option.code,
                        onClick = {
                            if (state.currentLanguage != option.code) {
                                pendingLanguageCode = option.code
                                showRestartDialog = true
                            }
                        }
                    )
                    
                    if (option != LanguageManager.getAvailableLanguages().last()) {
                        Divider(
                            modifier = Modifier.padding(
                                horizontal = Spacing.small
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // About Section
            AppSettingsSection(
                title = stringResource(R.string.about)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.small),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.version),
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "1.0.0",
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.large))

            if (state.success) {
                Text(
                    text = stringResource(R.string.language_updated),
                    color = MaterialTheme.colorScheme.primary,
                    style = AppTypography.body,
                    modifier = Modifier.padding(bottom = Spacing.medium)
                )
            }

            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = AppTypography.body,
                    modifier = Modifier.padding(bottom = Spacing.medium)
                )
            }

            AppPrimaryButton(
                text = stringResource(R.string.back),
                onClick = onBack,
                isLoading = state.isLoading
            )

            Spacer(modifier = Modifier.height(Spacing.xxlarge))
        }
    }
}
