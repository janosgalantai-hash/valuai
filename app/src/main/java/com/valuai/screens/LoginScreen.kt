package com.valuai.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.valuai.Screen
import com.valuai.i18n.LocalStrings
import com.valuai.ui.theme.*
import com.valuai.viewmodel.LoginViewModel

@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: LoginViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is com.valuai.viewmodel.LoginState.Success) {
            navController.navigate(Screen.Estimation.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("ValuAI", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
        Text(strings.appSubtitle, fontSize = 16.sp, color = TextSecondary,
            modifier = Modifier.padding(bottom = 48.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(strings.emailLabel, color = TextSecondary) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color(0xFF2A2A3A),
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                cursorColor = GoldPrimary, focusedContainerColor = Color(0xFF12121E),
                unfocusedContainerColor = Color(0xFF12121E)
            ),
            shape = RoundedCornerShape(8.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(strings.passwordLabel, color = TextSecondary) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color(0xFF2A2A3A),
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                cursorColor = GoldPrimary, focusedContainerColor = Color(0xFF12121E),
                unfocusedContainerColor = Color(0xFF12121E)
            ),
            shape = RoundedCornerShape(8.dp)
        )

        if (state is com.valuai.viewmodel.LoginState.Error) {
            Text(
                (state as com.valuai.viewmodel.LoginState.Error).message,
                color = StatusBad, fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        Button(
            onClick = { viewModel.login(context, email, password) },
            enabled = state !is com.valuai.viewmodel.LoginState.Loading,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
        ) {
            if (state is com.valuai.viewmodel.LoginState.Loading) {
                CircularProgressIndicator(color = BackgroundDark, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text(strings.signIn, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1200))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(strings.noAccount, fontSize = 14.sp, color = TextSecondary)
            Text(
                strings.signUpLink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = GoldPrimary,
                modifier = Modifier.clickable {
                    navController.navigate(Screen.Register.route)
                }
            )
        }
    }
}