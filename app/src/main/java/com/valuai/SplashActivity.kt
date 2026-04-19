package com.valuai

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valuai.ui.theme.*
import kotlinx.coroutines.*

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SplashScreen()
        }
        CoroutineScope(Dispatchers.Main).launch {
            delay(2500)
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }
    }
}

@Composable
fun SplashScreen() {
    val gold = GoldPrimary
    val goldFaded = Color(0x33C8A96A)

    // Pulsing rings
    val pulse = rememberInfiniteTransition(label = "pulse")
    val ring1 = pulse.animateFloat(
        initialValue = 0.6f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse),
        label = "r1"
    )
    val ring2 = pulse.animateFloat(
        initialValue = 0.8f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1600, 300, EaseInOut), RepeatMode.Reverse),
        label = "r2"
    )

    // Scan line
    val scanAnim = pulse.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "scan"
    )

    // Icon scale on entry
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        // Radial glow background
        Canvas(modifier = Modifier.size(320.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x22C8A96A), Color.Transparent),
                    center = Offset(size.width / 2, size.height / 2),
                    radius = size.width / 2
                )
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Icon + rings + scan
            Box(contentAlignment = Alignment.Center) {
                // Animated rings
                Canvas(modifier = Modifier.size(200.dp)) {
                    val cx = size.width / 2
                    val cy = size.height / 2
                    drawCircle(
                        color = gold.copy(alpha = 0.15f * ring1.value),
                        radius = 90.dp.toPx() * ring1.value,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = gold.copy(alpha = 0.25f * ring2.value),
                        radius = 68.dp.toPx() * ring2.value,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Camera icon box with scan line
                Box(
                    modifier = Modifier.size(96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Scan line inside the icon area
                    Canvas(modifier = Modifier.size(96.dp)) {
                        val y = size.height * scanAnim.value
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, gold.copy(alpha = 0.8f), Color.Transparent)
                            ),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = gold,
                        modifier = Modifier.size(64.dp)
                    )
                }

                // Corner brackets (viewfinder)
                Canvas(modifier = Modifier.size(200.dp)) {
                    val bSize = 20.dp.toPx()
                    val bStroke = 3.dp.toPx()
                    val pad = 52.dp.toPx()
                    val goldColor = gold

                    // Top-left
                    drawLine(goldColor, Offset(pad, pad), Offset(pad + bSize, pad), bStroke)
                    drawLine(goldColor, Offset(pad, pad), Offset(pad, pad + bSize), bStroke)
                    // Top-right
                    drawLine(goldColor, Offset(size.width - pad, pad), Offset(size.width - pad - bSize, pad), bStroke)
                    drawLine(goldColor, Offset(size.width - pad, pad), Offset(size.width - pad, pad + bSize), bStroke)
                    // Bottom-left
                    drawLine(goldColor, Offset(pad, size.height - pad), Offset(pad + bSize, size.height - pad), bStroke)
                    drawLine(goldColor, Offset(pad, size.height - pad), Offset(pad, size.height - pad - bSize), bStroke)
                    // Bottom-right
                    drawLine(goldColor, Offset(size.width - pad, size.height - pad), Offset(size.width - pad - bSize, size.height - pad), bStroke)
                    drawLine(goldColor, Offset(size.width - pad, size.height - pad), Offset(size.width - pad, size.height - pad - bSize), bStroke)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "ValuAI",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = gold,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "AI Item Valuation",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = TextSecondary,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
