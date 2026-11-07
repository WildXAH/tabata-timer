package com.example.tabatatimer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    var showLanguagePage by remember {
        mutableStateOf(false)
    }

    val backDescription = stringResource(
        R.string.settings_back
    )

    if (showLanguagePage) {
        LanguagePage(
            selectedLanguage = selectedLanguage,
            onLanguageChange = { language ->
                showLanguagePage = false
                onLanguageChange(language)
            },
            onBack = {
                showLanguagePage = false
            }
        )

        return
    }

    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
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
                    Icon(
                        painter = painterResource(
                            R.drawable.ic_arrow_back
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = stringResource(
                        R.string.settings_title
                    ),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            SettingsSwitchRow(
                iconRes = R.drawable.ic_sound,
                title = stringResource(
                    R.string.settings_sound
                ),
                description = stringResource(
                    R.string.settings_sound_description
                ),
                checked = soundEnabled,
                onCheckedChange = onSoundChange
            )

            SettingsSwitchRow(
                iconRes = R.drawable.ic_vibration,
                title = stringResource(
                    R.string.settings_vibration
                ),
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
                            showLanguagePage = true
                        }
                        .padding(16.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(
                            R.drawable.ic_language
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

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

                    Icon(
                        painter = painterResource(
                            R.drawable.ic_chevron_right
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    iconRes: Int,
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
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
private fun LanguagePage(
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    onBack: () -> Unit
) {
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
                Icon(
                    painter = painterResource(
                        R.drawable.ic_arrow_back
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = stringResource(
                    R.string.language_dialog_title
                ),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LanguageOption(
            label = stringResource(
                R.string.language_system
            ),
            selected = selectedLanguage == "",
            onClick = {
                onLanguageChange("")
            }
        )

        LanguageOption(
            label = stringResource(
                R.string.language_english
            ),
            selected = selectedLanguage == "en",
            onClick = {
                onLanguageChange("en")
            }
        )

        LanguageOption(
            label = stringResource(
                R.string.language_russian
            ),
            selected = selectedLanguage == "ru",
            onClick = {
                onLanguageChange("ru")
            }
        )
    }
}

@Composable
private fun LanguageOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
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
                .selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    onClick = onClick
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            RadioButton(
                selected = selected,
                onClick = null,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}