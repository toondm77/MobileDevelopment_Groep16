package edu.ap.myapplication.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    val showDialog = remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "Profile", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text("User info placeholder")
            }

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Logout")
            }
        }

        FloatingActionButton(
            onClick = { showDialog.value = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add city")
        }
    }

    if (showDialog.value) {
        AddCityDialog(
            onDismiss = { showDialog.value = false },
            onSave = { cityMap ->
                saveCityToFirestore(cityMap) { success, message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    if (success) showDialog.value = false
                }
            }
        )
    }
}

@Composable
fun AddCityDialog(
    onDismiss: () -> Unit,
    onSave: (Map<String, Any>) -> Unit
) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val defaultDate = remember {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            .format(Date())
    }
    var dateAdded by remember { mutableStateOf(defaultDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank() || country.isBlank()) {
                        Toast.makeText(ctx, "Please provide at least name and country", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    val city = mapOf(
                        "name" to name,
                        "category" to category,
                        "country" to country,
                        "dateAdded" to dateAdded,
                        "description" to description,
                        "location" to location
                    )
                    onSave(city)
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Add City") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("Country") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location (lat,lng or description)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dateAdded,
                    onValueChange = { dateAdded = it },
                    label = { Text("Date Added (ISO)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
        }
    )
}

private fun saveCityToFirestore(
    city: Map<String, Any>,
    callback: (Boolean, String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    db.collection("cities")
        .add(city)
        .addOnSuccessListener {
            callback(true, "City added")
        }
        .addOnFailureListener { e ->
            callback(false, "Failed to add city: ${e.message}")
        }
}
