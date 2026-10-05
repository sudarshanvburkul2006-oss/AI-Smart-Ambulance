package com.smartambulance.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            SmartLoginScreen(

                onAmbulanceLogin = {
                    val intent = Intent(
                        this,
                        MainActivity::class.java
                    )

                    intent.putExtra(
                        "USER_ROLE",
                        "AMBULANCE"
                    )

                    startActivity(intent)
                    finish()
                },

                onDriverLogin = {
                    val intent = Intent(
                        this,
                        DriverActivity::class.java
                    )

                    intent.putExtra(
                        "USER_ROLE",
                        "DRIVER"
                    )

                    startActivity(intent)
                    finish()
                }
            )
        }
    }
}

@Composable
fun SmartLoginScreen(
    onAmbulanceLogin: () -> Unit,
    onDriverLogin: () -> Unit
) {
    val context = LocalContext.current

    var selectedRole by remember {
        mutableStateOf("AMBULANCE")
    }

    var userId by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    fun login() {

        errorMessage = ""

        val enteredUserId = userId.trim()
        val enteredPassword = password.trim()

        if (enteredUserId.isEmpty() || enteredPassword.isEmpty()) {
            errorMessage = "Please enter User ID and Password."
            return
        }

        isLoading = true

        when (selectedRole) {

            "AMBULANCE" -> {

                if (
                    enteredUserId.equals("AMB001", ignoreCase = true) &&
                    enteredPassword == "amb123"
                ) {
                    isLoading = false

                    val prefs = context.getSharedPreferences(
                        "SmartAmbulancePrefs",
                        0
                    )

                    prefs.edit()
                        .putBoolean("isLoggedIn", true)
                        .putString("userRole", "AMBULANCE")
                        .apply()

                    onAmbulanceLogin()
                } else {
                    isLoading = false

                    val prefs = context.getSharedPreferences(
                        "SmartAmbulancePrefs",
                        0
                    )

                    prefs.edit()
                        .putBoolean("isLoggedIn", true)
                        .putString("userRole", "DRIVER")
                        .apply()

                    onDriverLogin()
            }
        }

        "DRIVER" -> {

                if (
                    enteredUserId.equals("CAR001", ignoreCase = true) &&
                    enteredPassword == "car123"
                ) {
                    isLoading = false
                    onDriverLogin()
                } else {
                    isLoading = false
                    errorMessage = "Invalid driver credentials."
                }
            }

            else -> {
                isLoading = false
                errorMessage = "Please select an account type."
            }
        }
    }

    MaterialTheme {

        Surface(
            modifier = Modifier.fillMaxSize()
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                // LOGO / TITLE

                Text(
                    text = "🚑",
                    fontSize = 48.sp
                )

                Text(
                    text = "Smart Ambulance",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Emergency Corridor System",
                    fontSize = 15.sp
                )

                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                // LOGIN CARD

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 8.dp
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {

                        Text(
                            text = "Secure Login",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )

                        Text(
                            text = "Select your account type",
                            fontSize = 14.sp
                        )

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        // ROLE SELECTION

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(10.dp)
                        ) {

                            RoleCard(
                                emoji = "🚑",
                                title = "Ambulance",
                                selected =
                                    selectedRole == "AMBULANCE",
                                modifier =
                                    Modifier.weight(1f),
                                onClick = {

                                    selectedRole =
                                        "AMBULANCE"

                                    userId = ""
                                    password = ""
                                    errorMessage = ""
                                }
                            )

                            RoleCard(
                                emoji = "🚗",
                                title = "Driver",
                                selected =
                                    selectedRole == "DRIVER",
                                modifier =
                                    Modifier.weight(1f),
                                onClick = {

                                    selectedRole =
                                        "DRIVER"

                                    userId = ""
                                    password = ""
                                    errorMessage = ""
                                }
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(18.dp)
                        )

                        // USER ID

                        OutlinedTextField(
                            value = userId,
                            onValueChange = {
                                userId = it
                                errorMessage = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("User ID")
                            },
                            singleLine = true
                        )

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        // PASSWORD

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Password")
                            },
                            singleLine = true,
                            visualTransformation =
                                if (passwordVisible)
                                    VisualTransformation.None
                                else
                                    PasswordVisualTransformation(),

                            trailingIcon = {

                                IconButton(
                                    onClick = {
                                        passwordVisible =
                                            !passwordVisible
                                    }
                                ) {

                                    Icon(
                                        imageVector =
                                            if (passwordVisible)
                                                Icons.Default.VisibilityOff
                                            else
                                                Icons.Default.Visibility,
                                        contentDescription =
                                            "Show password"
                                    )
                                }
                            }
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        // ERROR

                        if (errorMessage.isNotEmpty()) {

                            Text(
                                text = "⚠ $errorMessage",
                                color =
                                    MaterialTheme.colorScheme.error,
                                fontSize = 14.sp
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        // LOGIN BUTTON

                        Button(
                            onClick = {
                                login()
                            },
                            modifier =
                                Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {

                            if (isLoading) {

                                CircularProgressIndicator(
                                    modifier =
                                        Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )

                            } else {

                                Text(
                                    text =
                                        if (
                                            selectedRole ==
                                            "AMBULANCE"
                                        )
                                            "🚑 Login as Ambulance"
                                        else
                                            "🚗 Login as Driver"
                                )
                            }
                        }

                        Spacer(
                            modifier =
                                Modifier.height(14.dp)
                        )

                        Text(
                            text =
                                if (
                                    selectedRole ==
                                    "AMBULANCE"
                                )
                                    "Emergency controls available after authentication."
                                else
                                    "Vehicle monitoring runs in the background after setup.",
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                Text(
                    text = "🔐 Authorized Access Only",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun RoleCard(
    emoji: String,
    title: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {

    val borderColor =
        if (selected)
            MaterialTheme.colorScheme.primary
        else
            Color.LightGray

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable {
                onClick()
            },
        colors = CardDefaults.cardColors(
            containerColor =
                if (selected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
        )
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = emoji,
                    fontSize = 28.sp
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = title,
                    fontWeight = FontWeight.Bold
                )

                if (selected) {

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Icon(
                        imageVector =
                            Icons.Default.CheckCircle,
                        contentDescription =
                            "Selected",
                        modifier =
                            Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}