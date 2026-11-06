package com.example.tabatatimer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.systemBarsPadding

@Composable
internal fun SettingsScreen(
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    onSoundChange: (Boolean) -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit
) {
    var showLanguageDialog by remember {
        mutableStateOf(false)
    }

    val backDescription = stringResource(
        R.string.settings_back
    )

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 24.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        contentDescription = backDescription
                    },
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor =
                        MaterialTheme.colorScheme.surface,
                    contentColor =
                        MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(
                    text = "←",
                    fontSize = 24.sp
                )
            }

            Text(
                text = stringResource(R.string.settings_title),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }

        SettingsSwitchRow(
            title = stringResource(R.string.settings_sound),
            description = stringResource(
                R.string.settings_sound_description
            ),
            checked = soundEnabled,
            onCheckedChange = onSoundChange
        )

        SettingsSwitchRow(
            title = stringResource(R.string.settings_vibration),
            description = stringResource(
                R.string.settings_vibration_description
            ),
            checked = vibrationEnabled,
            onCheckedChange = onVibrationChange
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 1.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showLanguageDialog = true
                    }
                    .padding(16.dp),
                horizontalArrangement =
                    Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(
                            R.string.settings_language
                        ),
                        color =
                            MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = stringResource(
                            when (selectedLanguage) {
                                "en" -> R.string.language_english
                                "ru" -> R.string.language_russian
                                else -> R.string.language_system
                            }
                        ),
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }

                Text(
                    text = "›",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 28.sp
                )
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = {
                showLanguageDialog = false
            },
            title = {
                Text(
                    text = stringResource(
                        R.string.language_dialog_title
                    )
                )
            },
            text = {
                Column {
                    LanguageOption(
                        label = stringResource(
                            R.string.language_system
                        ),
                        selected = selectedLanguage == "",
                        onClick = {
                            onLanguageChange("")
                            showLanguageDialog = false
                        }
                    )

                    LanguageOption(
                        label = stringResource(
                            R.string.language_english
                        ),
                        selected = selectedLanguage == "en",
                        onClick = {
                            onLanguageChange("en")
                            showLanguageDialog = false
                        }
                    )

                    LanguageOption(
                        label = stringResource(
                            R.string.language_russian
                        ),
                        selected = selectedLanguage == "ru",
                        onClick = {
                            onLanguageChange("ru")
                            showLanguageDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLanguageDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.close)
                    )
                }
            }
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 1.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    role = Role.Switch,
                    onValueChange = onCheckedChange
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = description,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = null
            )
        }
    }
}

@Composable
private fun LanguageOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (selected) "●" else "○",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 18.sp
        )

        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp
        )
    }
}