package jp.riverapp.hexlide.presentation.screen.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.riverapp.hexlide.BuildConfig
import jp.riverapp.hexlide.presentation.localization.LocalizationManager
import jp.riverapp.hexlide.presentation.localization.LocalizationManager.Language
import jp.riverapp.hexlide.presentation.theme.HexlideColors
import jp.riverapp.hexlide.util.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    localizationManager: LocalizationManager,
    onBack: () -> Unit,
    onNavigateToWebView: (String) -> Unit,
) {
    val currentLanguage by localizationManager.language.collectAsState()
    val strings = localizationManager.strings
    val context = LocalContext.current

    var showLanguagePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDeleteComplete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.settings,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HexlideColors.Background,
                ),
            )
        },
        containerColor = HexlideColors.Background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // -- Language Section --
            SectionHeader(text = strings.language)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
            ) {
                SettingsItem(
                    icon = Icons.Filled.Language,
                    title = strings.language,
                    subtitle = displayLanguageName(currentLanguage, strings.followDeviceSetting),
                    onClick = { showLanguagePicker = true },
                )
            }

            // -- Links Section --
            SectionHeader(text = "")
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
            ) {
                SettingsItem(
                    icon = Icons.Filled.QuestionMark,
                    title = strings.howToPlay,
                    onClick = { onNavigateToWebView(Constants.WebPages.HOW_TO_PLAY) },
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Filled.Shield,
                    title = strings.privacyPolicy,
                    onClick = { onNavigateToWebView(Constants.WebPages.PRIVACY) },
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Filled.Email,
                    title = strings.contact,
                    onClick = { onNavigateToWebView(Constants.WebPages.CONTACT) },
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Filled.Home,
                    title = strings.homepage,
                    onClick = {
                        openExternalBrowser(context, Constants.WebPages.HOMEPAGE)
                    },
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Filled.OpenInBrowser,
                    title = strings.appWebsite,
                    onClick = {
                        openExternalBrowser(context, Constants.WebPages.WEBSITE)
                    },
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Filled.PlayCircle,
                    title = strings.webVersion,
                    onClick = {
                        openExternalBrowser(context, Constants.WebPages.WEB_VERSION)
                    },
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Filled.Star,
                    title = strings.writeReview,
                    onClick = {
                        openExternalBrowser(context, Constants.WebPages.GOOGLE_PLAY_REVIEW)
                    },
                )
            }

            // -- Version Section --
            SectionHeader(text = "")
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = strings.version,
                        fontSize = 16.sp,
                        color = HexlideColors.TextPrimary,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = BuildConfig.VERSION_NAME,
                        fontSize = 16.sp,
                        color = HexlideColors.TextSecondary,
                    )
                }
            }

            // -- Delete Local Data Section --
            SectionHeader(text = "")
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
            ) {
                SettingsItem(
                    icon = Icons.Filled.Delete,
                    title = strings.deleteLocalData,
                    titleColor = Color(0xFFDC2626),
                    onClick = { showDeleteConfirm = true },
                )
            }

            // Bottom padding
            Spacer(modifier = Modifier.padding(bottom = 32.dp))
        }
    }

    // -- Language Picker Dialog --
    if (showLanguagePicker) {
        AlertDialog(
            onDismissRequest = { showLanguagePicker = false },
            title = { Text(strings.language) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    // "Follow Device" option
                    LanguageRadioItem(
                        label = strings.followDeviceSetting,
                        selected = currentLanguage == Language.SYSTEM,
                        onClick = {
                            localizationManager.setLanguage(Language.SYSTEM)
                            showLanguagePicker = false
                        },
                    )
                    // All real languages
                    Language.entries
                        .filter { it != Language.SYSTEM }
                        .forEach { lang ->
                            LanguageRadioItem(
                                label = lang.nativeName,
                                selected = currentLanguage == lang,
                                onClick = {
                                    localizationManager.setLanguage(lang)
                                    showLanguagePicker = false
                                },
                            )
                        }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguagePicker = false }) {
                    Text(strings.cancel)
                }
            },
        )
    }

    // -- Delete Confirmation Dialog --
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(strings.deleteLocalData) },
            text = { Text(strings.deleteLocalDataConfirm) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteLocalData(context, localizationManager)
                        showDeleteConfirm = false
                        showDeleteComplete = true
                    },
                ) {
                    Text(
                        text = strings.delete,
                        color = Color(0xFFDC2626),
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(strings.cancel)
                }
            },
        )
    }

    // -- Delete Complete Dialog --
    if (showDeleteComplete) {
        AlertDialog(
            onDismissRequest = { showDeleteComplete = false },
            title = { Text(strings.deleteLocalData) },
            text = { Text(strings.deleteLocalDataComplete) },
            confirmButton = {
                TextButton(onClick = { showDeleteComplete = false }) {
                    Text(strings.done)
                }
            },
        )
    }
}

// ----- Internal composables -----

@Composable
private fun SectionHeader(text: String) {
    if (text.isNotEmpty()) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = HexlideColors.TextSecondary,
            modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp),
        )
    } else {
        Spacer(modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    titleColor: Color = HexlideColors.TextPrimary,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = titleColor,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = titleColor,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = HexlideColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 50.dp),
        color = HexlideColors.TileStroke,
    )
}

@Composable
private fun LanguageRadioItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontSize = 16.sp)
    }
}

// ----- Helpers -----

private fun displayLanguageName(language: Language, followDeviceLabel: String): String {
    return if (language == Language.SYSTEM) followDeviceLabel else language.nativeName
}

private fun openExternalBrowser(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

private fun deleteLocalData(context: Context, localizationManager: LocalizationManager) {
    val prefs = context.getSharedPreferences("hexlide_prefs", Context.MODE_PRIVATE)
    prefs.edit()
        .remove("hexlide_player_id")
        .remove("nonaga_player_id")
        .remove("hexlide_lang")
        .apply()
    jp.riverapp.hexlide.data.model.AIBattleStatsService.clear(context)
    localizationManager.setLanguage(Language.SYSTEM)
}
