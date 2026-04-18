package com.valuai.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.valuai.Screen
import com.valuai.i18n.LocalStrings
import com.valuai.network.EstimationResponse
import com.valuai.network.ResultRepository
import com.valuai.ui.theme.*
import com.valuai.viewmodel.HistoryState
import com.valuai.viewmodel.HistoryViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: HistoryViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadHistory(context) }
    val strings = LocalStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)) {
            Text(
                strings.history,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            val count = if (state is HistoryState.Success)
                (state as HistoryState.Success).items.size else 0
            Text(
                String.format(strings.appraisalsCount, count),
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        when (val s = state) {
            is HistoryState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GoldPrimary)
                }
            }
            is HistoryState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(s.message, color = StatusBad)
                }
            }
            is HistoryState.Success -> {
                val filtered = s.items.filter { it.status == "done" && it.result != null }

                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(strings.noAppraisals, color = TextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(filtered) { item ->
                            HistoryItem(item, strings) {
                                ResultRepository.lastResult = item
                                navController.navigate(Screen.Result.createRoute(item.id))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(item: EstimationResponse, strings: com.valuai.i18n.AppStrings, onClick: () -> Unit) {
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
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E1E30)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = categoryIcon(item.result?.category ?: ""),
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.result?.item_name ?: strings.unknownItem,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(formatDate(item.created_at), fontSize = 12.sp, color = TextCaption)
                ConditionBadge(item.result?.condition ?: "", strings)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                "$${"%,.0f".format(item.result?.price_recommended ?: 0.0)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextCaption,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun ConditionBadge(condition: String, strings: com.valuai.i18n.AppStrings) {
    val (label, color) = when (condition.lowercase()) {
        "excellent", "kiváló" -> strings.conditionExcellent to StatusGood
        "good", "jó"          -> strings.conditionGood to StatusGood
        "fair", "közepes"     -> strings.conditionFair to StatusMedium
        else                  -> strings.conditionPoor to StatusBad
    }
    Surface(shape = RoundedCornerShape(4.dp), color = color.copy(alpha = 0.15f)) {
        Text(
            label,
            fontSize = 11.sp,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

fun categoryIcon(category: String): ImageVector = when {
    category.contains("watch", ignoreCase = true) ||
            category.contains("jewelry", ignoreCase = true) ||
            category.contains("ékszer", ignoreCase = true) -> Icons.Default.Watch
    category.contains("electronics", ignoreCase = true) ||
            category.contains("tech", ignoreCase = true) -> Icons.Default.Devices
    category.contains("collectible", ignoreCase = true) ||
            category.contains("gyűjtemény", ignoreCase = true) -> Icons.Default.Collections
    category.contains("kamera", ignoreCase = true) -> Icons.Default.CameraAlt
    else -> Icons.Default.Diamond
}

fun formatDate(dateString: String): String {
    return try {
        val instant = Instant.parse(dateString)
        val formatter = DateTimeFormatter.ofPattern("MMM dd.")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        dateString
    }
}