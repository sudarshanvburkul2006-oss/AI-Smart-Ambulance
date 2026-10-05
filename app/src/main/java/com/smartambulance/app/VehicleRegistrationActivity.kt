package com.smartambulance.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class VehicleRegistrationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            VehicleRegistrationScreen(

                onBack = {
                    finish()
                },

                onRegister = {
                        vehicleId,
                        vehicleNumber,
                        driverName,
                        mobileNumber,
                        username,
                        password ->

                    // Temporary registration test.
                    // Cloud database will be connected later.

                    Toast.makeText(
                        this,
                        "Vehicle registration form submitted",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }
}

@Composable
fun VehicleRegistrationScreen(
    onBack: () -> Unit,
    onRegister: (
        String,
        String,
        String,
        String,
        String,
        String
    ) -> Unit
) {

    var vehicleId by remember {
        mutableStateOf("")
    }

    var vehicleNumber by remember {
        mutableStateOf("")
    }

    var driverName by remember {
        mutableStateOf("")
    }

    var mobileNumber by remember {
        mutableStateOf("")
    }

    var username by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(24.dp),

            verticalArrangement = Arrangement.Top
        ) {

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text = "🚗 Vehicle Registration",
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Register your connected vehicle",
                fontSize = 15.sp
            )

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            OutlinedTextField(
                value = vehicleId,
                onValueChange = {
                    vehicleId = it
                    errorMessage = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Vehicle ID")
                },
                placeholder = {
                    Text("Example: CAR001")
                },
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OutlinedTextField(
                value = vehicleNumber,
                onValueChange = {
                    vehicleNumber = it
                    errorMessage = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Vehicle Number")
                },
                placeholder = {
                    Text("Example: MH12AB1234")
                },
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OutlinedTextField(
                value = driverName,
                onValueChange = {
                    driverName = it
                    errorMessage = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Driver Name")
                },
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OutlinedTextField(
                value = mobileNumber,
                onValueChange = {
                    mobileNumber = it
                    errorMessage = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Mobile Number")
                },
                placeholder = {
                    Text("Example: 9876543210")
                },
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    errorMessage = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Username")
                },
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

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
                visualTransformation =
                    PasswordVisualTransformation(),
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            if (errorMessage.isNotEmpty()) {

                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )
            }

            Button(
                onClick = {

                    if (
                        vehicleId.trim().isEmpty() ||
                        vehicleNumber.trim().isEmpty() ||
                        driverName.trim().isEmpty() ||
                        mobileNumber.trim().isEmpty() ||
                        username.trim().isEmpty() ||
                        password.trim().isEmpty()
                    ) {

                        errorMessage =
                            "Please fill all the fields."

                    } else {

                        onRegister(
                            vehicleId.trim(),
                            vehicleNumber.trim(),
                            driverName.trim(),
                            mobileNumber.trim(),
                            username.trim(),
                            password
                        )
                    }
                },

                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "🚗 Register Vehicle",
                    fontSize = 16.sp
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "← Back"
                )
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )
        }
    }
}