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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AmbulanceRegistrationActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContent {

            AmbulanceRegistrationScreen(

                onBack = {
                    finish()
                },

                onRegister = {
                        ambulanceId,
                        vehicleNumber,
                        driverName,
                        hospitalName,
                        hospitalContact,
                        authorizationId,
                        email,
                        password ->

                    registerAmbulance(
                        ambulanceId,
                        vehicleNumber,
                        driverName,
                        hospitalName,
                        hospitalContact,
                        authorizationId,
                        email,
                        password
                    )
                }
            )
        }
    }

    private fun registerAmbulance(
        ambulanceId: String,
        vehicleNumber: String,
        driverName: String,
        hospitalName: String,
        hospitalContact: String,
        authorizationId: String,
        email: String,
        password: String
    ) {

        Toast.makeText(
            this,
            "Creating ambulance account...",
            Toast.LENGTH_SHORT
        ).show()

        auth.createUserWithEmailAndPassword(
            email,
            password
        )
            .addOnSuccessListener { result ->

                val uid = result.user?.uid

                if (uid == null) {
                    Toast.makeText(
                        this,
                        "Registration failed: UID not found",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                val ambulanceData = hashMapOf(
                    "uid" to uid,
                    "ambulanceId" to ambulanceId,
                    "vehicleNumber" to vehicleNumber,
                    "driverName" to driverName,
                    "hospitalName" to hospitalName,
                    "hospitalContact" to hospitalContact,
                    "authorizationId" to authorizationId,
                    "email" to email,
                    "role" to "AMBULANCE",
                    "isActive" to true,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                db.collection("ambulances")
                    .document(ambulanceId)
                    .set(ambulanceData)
                    .addOnSuccessListener {

                        Toast.makeText(
                            this,
                            "✅ Ambulance registered successfully",
                            Toast.LENGTH_LONG
                        ).show()

                        finish()
                    }
                    .addOnFailureListener { error ->

                        Toast.makeText(
                            this,
                            "Firestore error: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { error ->

                Toast.makeText(
                    this,
                    "Registration failed: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}


@Composable
fun AmbulanceRegistrationScreen(
    onBack: () -> Unit,
    onRegister: (
        String,
        String,
        String,
        String,
        String,
        String,
        String,
        String
    ) -> Unit
) {

    var ambulanceId by remember {
        mutableStateOf("")
    }

    var vehicleNumber by remember {
        mutableStateOf("")
    }

    var driverName by remember {
        mutableStateOf("")
    }

    var hospitalName by remember {
        mutableStateOf("")
    }

    var hospitalContact by remember {
        mutableStateOf("")
    }

    var authorizationId by remember {
        mutableStateOf("")
    }

    var email by remember {
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
                text = "🚑 Ambulance Registration",
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Register a new emergency ambulance",
                fontSize = 15.sp
            )

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            OutlinedTextField(
                value = ambulanceId,
                onValueChange = {
                    ambulanceId = it
                    errorMessage = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Ambulance ID")
                },
                placeholder = {
                    Text("Example: AMB001")
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
                value = hospitalName,
                onValueChange = {
                    hospitalName = it
                    errorMessage = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Hospital Name")
                },
                placeholder = {
                    Text("Example: City Hospital")
                },
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OutlinedTextField(
                value = hospitalContact,
                onValueChange = {
                    hospitalContact = it
                    errorMessage = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Hospital Contact")
                },
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OutlinedTextField(
                value = authorizationId,
                onValueChange = {
                    authorizationId = it
                    errorMessage = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Emergency Authorization ID")
                },
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Email")
                },
                placeholder = {
                    Text("Example: ambulance001@gmail.com")
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
                        ambulanceId.trim().isEmpty() ||
                        vehicleNumber.trim().isEmpty() ||
                        driverName.trim().isEmpty() ||
                        hospitalName.trim().isEmpty() ||
                        hospitalContact.trim().isEmpty() ||
                        authorizationId.trim().isEmpty() ||
                        email.trim().isEmpty() ||
                        password.isEmpty()
                    ) {

                        errorMessage =
                            "Please fill all the fields."

                    } else {

                        onRegister(
                            ambulanceId.trim(),
                            vehicleNumber.trim(),
                            driverName.trim(),
                            hospitalName.trim(),
                            hospitalContact.trim(),
                            authorizationId.trim(),
                            email.trim(),
                            password
                        )
                    }
                },

                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "🚑 Register Ambulance",
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