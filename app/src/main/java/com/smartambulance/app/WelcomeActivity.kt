package com.smartambulance.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class WelcomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is already logged in
        val prefs = getSharedPreferences(
            "SmartAmbulancePrefs",
            MODE_PRIVATE
        )

        val isLoggedIn = prefs.getBoolean(
            "isLoggedIn",
            false
        )

        val userRole = prefs.getString(
            "userRole",
            ""
        )

        if (isLoggedIn) {

            if (userRole == "AMBULANCE") {

                startActivity(
                    Intent(
                        this,
                        MainActivity::class.java
                    )
                )

            } else if (userRole == "DRIVER") {

                startActivity(
                    Intent(
                        this,
                        DriverActivity::class.java
                    )
                )
            }

            finish()
            return
        }

        setContent {

            WelcomeScreen(

                onAmbulanceRegister = {

                    startActivity(
                        Intent(
                            this,
                            AmbulanceRegistrationActivity::class.java
                        )
                    )
                },

                onVehicleRegister = {

                    startActivity(
                        Intent(
                            this,
                            VehicleRegistrationActivity::class.java
                        )
                    )
                },

                onLogin = {
                    startActivity(
                        Intent(
                            this,
                            LoginActivity::class.java
                        )
                    )
                }
            )
        }
    }
}

@Composable
fun WelcomeScreen(
    onAmbulanceRegister: () -> Unit,
    onVehicleRegister: () -> Unit,
    onLogin: () -> Unit
) {

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),

            horizontalAlignment = Alignment.CenterHorizontally,

            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "🚑",
                fontSize = 64.sp
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "SMART AMBULANCE",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "AI Emergency Corridor System",
                fontSize = 16.sp
            )

            Spacer(
                modifier = Modifier.height(40.dp)
            )

            Button(
                onClick = onAmbulanceRegister,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🚑 Register Ambulance",
                    fontSize = 16.sp
                )
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Button(
                onClick = onVehicleRegister,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🚗 Register Vehicle",
                    fontSize = 16.sp
                )
            }

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            OutlinedButton(
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🔐 Existing User Login",
                    fontSize = 16.sp
                )
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Text(
                text = "Emergency corridor • Live GPS • Hospital routing",
                fontSize = 13.sp
            )
        }
    }
}