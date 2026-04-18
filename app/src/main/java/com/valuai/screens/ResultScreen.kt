package com.valuai.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.valuai.TokenManager
import com.valuai.i18n.LocalStrings
import com.valuai.network.ResultRepository
import com.valuai.ui.theme.*

@Composable
fun ResultScreen(estimationId: String, navController: NavController) {
    val result = ResultRepository.lastResult
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val currency by tokenManager.currency.collectAsState(initial = "USD")
    val strings = LocalStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(strings.appraisalTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            IconButton(onClick = { }) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
            }
        }

        if (result == null) {
            Box(
                Modifier.fillMaxSize().background(BackgroundDark),
                contentAlignment = Alignment.Center
            ) {
                Text(strings.noData, color = TextSecondary, fontSize = 16.sp)
            }
            return@Column
        }

        val estimation = result.result ?: run {
            Box(
                Modifier.fillMaxSize().background(BackgroundDark),
                contentAlignment = Alignment.Center
            ) {
                Text(strings.noData, color = TextSecondary, fontSize = 16.sp)
            }
            return@Column
        }

        val symbol = currencySymbol(currency)

        // Ár hero
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = strings.estimatedMarketValue,
                fontSize = 11.sp,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$symbol${"%,.0f".format(estimation.price_min)} – ${"%,.0f".format(estimation.price_max)}",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "${strings.recommendedPrice}$symbol${"%.0f".format(estimation.price_recommended)}",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Állapot tagek
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ConditionChip(estimation.condition)
            CategoryChip(estimation.category)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI összefoglaló
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = strings.aiSummary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GoldPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = estimation.summary,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Piaci referenciák
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "${strings.marketReferences} (${estimation.market_references_count})",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PriceColumn(strings.min, "$symbol${"%,.0f".format(estimation.price_min)}", TextSecondary)
                    PriceColumn(strings.avg, "$symbol${"%,.0f".format(estimation.price_recommended)}", GoldPrimary)
                    PriceColumn(strings.max, "$symbol${"%,.0f".format(estimation.price_max)}", TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Megbízhatóság
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(strings.appraisalConfidence, fontSize = 14.sp, color = TextSecondary)
            Text(
                text = when (estimation.confidence.lowercase()) {
                    "high"   -> strings.confidenceHigh
                    "medium" -> strings.confidenceMedium
                    else     -> strings.confidenceLow
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = when (estimation.confidence.lowercase()) {
                    "high"   -> StatusGood
                    "medium" -> StatusMedium
                    else     -> StatusBad
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ConditionChip(condition: String) {
    val color = when (condition.lowercase()) {
        "excellent", "kiváló" -> StatusGood
        "good", "jó"          -> StatusGood
        "fair", "közepes"     -> StatusMedium
        else                  -> StatusBad
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = condition,
            color = color,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun CategoryChip(category: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = GoldPrimary.copy(alpha = 0.15f)
    ) {
        Text(
            text = category,
            color = GoldPrimary,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun PriceColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = TextCaption)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

fun currencySymbol(currency: String): String = when (currency) {
    "USD" -> "$"
    "EUR" -> "€"
    "GBP" -> "£"
    "HUF" -> "Ft "
    "CHF" -> "Fr "
    "JPY" -> "¥"
    "NZD" -> "NZ$"
    "AUD" -> "A$"
    else  -> "$"
}