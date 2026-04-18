package com.valuai.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.valuai.Screen
import com.valuai.i18n.LocalStrings
import com.valuai.ui.theme.*
import com.valuai.viewmodel.RegisterState
import com.valuai.viewmodel.RegisterViewModel

@Composable
fun RegisterScreen(navController: NavController) {
    val viewModel: RegisterViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current

    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var successShown    by remember { mutableStateOf(false) }

    // Sikeres regisztráció után visszamegyünk a login-ra
    LaunchedEffect(state) {
        if (state is RegisterState.Success) {
            successShown = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(56.dp))

        // Vissza gomb
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Vissza", tint = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(strings.signUp, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(
            strings.createAccount,
            fontSize = 15.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 8.dp, bottom = 40.dp)
        )

        if (successShown) {
            // Sikeres regisztráció képernyő
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text("✓", fontSize = 64.sp, color = StatusGood)
                Text(
                    strings.registrationSuccess,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    strings.canNowSignIn,
                    fontSize = 15.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                ) {
                    Text(
                        strings.signIn,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1200)
                    )
                }
            }
        } else {
            // Regisztrációs form
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(strings.emailAddress, color = TextSecondary) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = textFieldColors(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(strings.passwordLabel, color = TextSecondary) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = textFieldColors(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            Text(
                strings.minimumChars,
                fontSize = 12.sp,
                color = TextCaption,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 16.dp)
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text(strings.confirmPassword, color = TextSecondary) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                colors = textFieldColors(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            if (state is RegisterState.Error) {
                Text(
                    (state as RegisterState.Error).message,
                    color = StatusBad,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Button(
                onClick = { viewModel.register(email, password, confirmPassword) },
                enabled = state !is RegisterState.Loading,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
            ) {
                if (state is RegisterState.Loading) {
                    CircularProgressIndicator(
                        color = BackgroundDark,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        strings.signUp,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1200)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = GoldPrimary,
    unfocusedBorderColor    = Color(0xFF2A2A3A),
    focusedTextColor        = Color.White,
    unfocusedTextColor      = Color.White,
    cursorColor             = GoldPrimary,
    focusedContainerColor   = Color(0xFF12121E),
    unfocusedContainerColor = Color(0xFF12121E)
)
