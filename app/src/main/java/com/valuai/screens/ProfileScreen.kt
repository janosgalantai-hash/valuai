package com.valuai.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valuai.TokenManager
import com.valuai.i18n.LocalStrings
import com.valuai.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }

    val currency by tokenManager.currency.collectAsState(initial = "USD")
    val language by tokenManager.language.collectAsState(initial = "English")

    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val strings = LocalStrings.current

    val currencies = listOf("NZD", "AUD", "USD", "EUR", "GBP", "HUF", "CHF", "JPY")
    val languages  = listOf("English", "Magyar", "Deutsch", "Français", "中文")

    // Currency dialog
    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            containerColor = Color(0xFF1A1A25),
            title = { Text(strings.selectCurrency, color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    currencies.forEach { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    scope.launch { tokenManager.saveCurrency(c) }
                                    showCurrencyDialog = false
                                }
                                .background(
                                    if (c == currency) GoldPrimary.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(c, color = if (c == currency) GoldPrimary else Color.White,
                                fontSize = 15.sp, fontWeight = if (c == currency) FontWeight.SemiBold else FontWeight.Normal)
                            if (c == currency) {
                                Icon(Icons.Default.Check, null, tint = GoldPrimary,
                                    modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Language dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = Color(0xFF1A1A25),
            title = { Text(strings.appLanguage, color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    languages.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    scope.launch { tokenManager.saveLanguage(lang) }
                                    showLanguageDialog = false
                                }
                                .background(
                                    if (lang == language) GoldPrimary.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(languageFlag(lang), fontSize = 20.sp)
                                Text(lang, color = if (lang == language) GoldPrimary else Color.White,
                                    fontSize = 15.sp, fontWeight = if (lang == language) FontWeight.SemiBold else FontWeight.Normal)
                            }
                            if (lang == language) {
                                Icon(Icons.Default.Check, null, tint = GoldPrimary,
                                    modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Text(strings.profile, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(strings.settingsSubtitle, fontSize = 14.sp, color = TextSecondary,
            modifier = Modifier.padding(bottom = 32.dp))

        // Avatar
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A25))
                    .border(2.dp, GoldPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = GoldPrimary,
                    modifier = Modifier.size(40.dp))
            }
        }

        // Settings szekció
        Text(strings.preferencesSection, fontSize = 11.sp, color = TextSecondary,
            letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))

        SettingsItem(
            icon = Icons.Default.AttachMoney,
            title = strings.currency,
            value = currency,
            onClick = { showCurrencyDialog = true }
        )

        Spacer(modifier = Modifier.height(2.dp))

        SettingsItem(
            icon = Icons.Default.Language,
            title = strings.appLanguage,
            value = language,
            onClick = { showLanguageDialog = true }
        )
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(GoldPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
            Text(value, fontSize = 13.sp, color = TextSecondary)
        }

        Icon(Icons.Default.ChevronRight, null, tint = TextCaption,
            modifier = Modifier.size(20.dp))
    }
}

fun languageFlag(language: String): String = when (language) {
    "English"  -> "🇬🇧"
    "Magyar"   -> "🇭🇺"
    "Deutsch"  -> "🇩🇪"
    "Français" -> "🇫🇷"
    "中文"      -> "🇨🇳"
    else       -> "🌐"
}