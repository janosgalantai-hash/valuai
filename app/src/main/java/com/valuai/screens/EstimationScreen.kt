package com.valuai.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.valuai.Screen
import com.valuai.i18n.LocalStrings
import com.valuai.ui.theme.*
import com.valuai.viewmodel.EstimationState
import com.valuai.viewmodel.EstimationViewModel
import java.io.File

@Composable
fun EstimationScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: EstimationViewModel = viewModel(context as ComponentActivity)
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current

    val imagePaths by viewModel.imagePaths.collectAsState()
    val description by viewModel.description.collectAsState()

    var showImageSourceDialog by remember { mutableStateOf(false) }
    val slots = 4

    // Kamera: fájlútvonalat követünk, nem URI-t
    val cameraFilePath = remember { mutableStateOf<String?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraFilePath.value?.let { path ->
                viewModel.addImage(path)
            }
        }
    }

    fun launchCamera() {
        try {
            val photoFile = File(
                File(context.filesDir, "valuai_images").also { it.mkdirs() },
                "img_${System.currentTimeMillis()}.jpg"
            ).also { it.createNewFile() }
            cameraFilePath.value = photoFile.absolutePath
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.provider", photoFile
            )
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            android.util.Log.e("Camera", "Failed: ${e.message}")
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.addImageFromGallery(context, it) }
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            containerColor = Color(0xFF1A1A25),
            title = { Text(strings.addPhotoTitle, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            showImageSourceDialog = false
                            viewModel.resetState()
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) launchCamera()
                            else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                    ) {
                        Text("📷  ${strings.camera}", color = Color(0xFF1A1200))
                    }
                    Button(
                        onClick = {
                            showImageSourceDialog = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A3A))
                    ) {
                        Text("🖼️  ${strings.gallery}", color = Color.White)
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
        Text(strings.photosLabel, fontSize = 11.sp, color = TextSecondary,
            letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in 0..1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (col in 0..1) {
                        val index = row * 2 + col
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (index < imagePaths.size) Color(0xFF1E1A14)
                                    else Color(0xFF12121E)
                                )
                                .border(
                                    width = if (index == 0) 1.dp else 0.5.dp,
                                    color = if (index == 0) GoldDark else Color(0xFF2A2A3A),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    if (index < imagePaths.size) {
                                        viewModel.removeImage(index)
                                    } else if (imagePaths.size < slots) {
                                        showImageSourceDialog = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (index < imagePaths.size) {
                                AsyncImage(
                                    model = File(imagePaths[index]),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp))
                                }
                            } else if (index == 0 && imagePaths.isEmpty()) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.CameraAlt, null,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(28.dp))
                                    Spacer(Modifier.height(4.dp))
                                    Text(strings.addPhotoSmall, fontSize = 11.sp, color = GoldPrimary)
                                }
                            } else {
                                Icon(Icons.Default.Add, null,
                                    tint = Color(0xFF3A3A5A),
                                    modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(strings.multipleAnglesHint, fontSize = 11.sp, color = TextCaption,
            modifier = Modifier.padding(bottom = 20.dp))

        Text(strings.descriptionLabel, fontSize = 11.sp, color = TextSecondary,
            letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { viewModel.setDescription(it) },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            placeholder = { Text(strings.describePlaceholder, color = TextCaption, fontSize = 14.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = GoldPrimary,
                unfocusedBorderColor    = Color(0xFF2A2A3A),
                focusedTextColor        = Color.White,
                unfocusedTextColor      = Color.White,
                cursorColor             = GoldPrimary,
                focusedContainerColor   = Color(0xFF12121E),
                unfocusedContainerColor = Color(0xFF12121E),
            ),
            shape = RoundedCornerShape(8.dp),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (state is EstimationState.Error) {
            Text(text = (state as EstimationState.Error).message,
                color = StatusBad, fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 12.dp))
        }

        if (state is EstimationState.Success) {
            Button(
                onClick = {
                    val result = (state as EstimationState.Success).result
                    viewModel.reset()
                    navController.navigate(Screen.Result.createRoute(result.id))
                },
                modifier = Modifier.fillMaxWidth().height(54.dp).padding(bottom = 8.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StatusGood)
            ) {
                Text(strings.viewResult, fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (imagePaths.isNotEmpty() && description.isNotBlank()) {
                    viewModel.startEstimation(context, description)
                }
            },
            enabled = imagePaths.isNotEmpty() &&
                    description.isNotBlank() &&
                    state !is EstimationState.Loading,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor         = GoldPrimary,
                disabledContainerColor = Color(0xFF5A4A2A)
            )
        ) {
            if (state is EstimationState.Loading) {
                CircularProgressIndicator(color = BackgroundDark,
                    modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text(strings.startAppraisal, fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1200))
            }
        }
    }
}
