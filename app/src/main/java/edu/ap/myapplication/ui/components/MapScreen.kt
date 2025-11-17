package edu.ap.myapplication.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.UUID


@Composable
fun MapScreen() {
    val context = LocalContext.current
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }

    var showDialog by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageUrl by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        imageUri = uri
    }

    var hasLocationPermission by remember { mutableStateOf(false) }
    val requestLocationPerms = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        hasLocationPermission = (res[android.Manifest.permission.ACCESS_FINE_LOCATION] == true) || (res[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        if (!hasLocationPermission) {
            Toast.makeText(context, "Location permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    fun ensureLocationPerms() {
        val fine = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarse = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        hasLocationPermission = fine || coarse
        if (!hasLocationPermission) {
            requestLocationPerms.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    fun resetForm() {
        title = ""; city = ""; country = ""; category = ""; description = ""; imageUri = null; imageUrl = ""
    }

    val defaultLatLng = LatLng(50.8503, 4.3517)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLatLng, 12f)
    }
    var myLatLng by remember { mutableStateOf<LatLng?>(null) }

    DisposableEffect(Unit) {
        ensureLocationPerms()
        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                myLatLng = LatLng(loc.latitude, loc.longitude)
            } else {
                val cts = CancellationTokenSource()
                fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { cur ->
                        if (cur != null) myLatLng = LatLng(cur.latitude, cur.longitude)
                    }
            }
        }
        onDispose { /* no-op */ }
    }

    DisposableEffect(myLatLng) {
        val dst = myLatLng
        if (dst != null) {
            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(dst, 15f))
        }
        onDispose { }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
            uiSettings = MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = false)
        )

        FloatingActionButton(onClick = { showDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Filled.AddLocationAlt, contentDescription = "Save current location")
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isBlank() || city.isBlank() || country.isBlank() || category.isBlank()) {
                        Toast.makeText(context, "Please fill in title, city, country and category", Toast.LENGTH_SHORT).show(); return@TextButton
                    }
                    if (imageUri == null && imageUrl.isBlank()) { Toast.makeText(context, "Pick a photo or paste an image URL", Toast.LENGTH_SHORT).show(); return@TextButton }

                    ensureLocationPerms()
                    if (!hasLocationPermission) return@TextButton

                    fun saveTrip(finalLat: Double, finalLng: Double, finalImage: String) {
                        val data = hashMapOf(
                            "title" to title,
                            "city" to city,
                            "country" to country,
                            "description" to description,
                            "image" to finalImage,
                            "category" to category,
                            "latitude" to finalLat,
                            "longitude" to finalLng,
                            "dateAdded" to FieldValue.serverTimestamp()
                        )
                        FirebaseFirestore.getInstance().collection("citytrips")
                            .add(data)
                            .addOnSuccessListener {
                                Toast.makeText(context, "CityTrip saved", Toast.LENGTH_SHORT).show()
                                showDialog = false
                                resetForm()
                            }
                            .addOnFailureListener { e -> Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show() }
                    }

                    fun handleWithLocation(lat: Double, lng: Double) {
                        val picked = imageUri
                        if (picked != null) {
                            val ref = FirebaseStorage.getInstance().reference.child("citytrips_images/${UUID.randomUUID()}.jpg")
                            ref.putFile(picked)
                                .continueWithTask { ref.downloadUrl }
                                .addOnSuccessListener { downloadUri ->
                                    saveTrip(lat, lng, downloadUri.toString())
                                }
                                .addOnFailureListener { e -> Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show() }
                        } else if (imageUrl.isNotBlank()) {
                            saveTrip(lat, lng, imageUrl.trim())
                        } else {
                            Toast.makeText(context, "No image selected", Toast.LENGTH_SHORT).show()
                        }
                    }

                    fused.lastLocation.addOnSuccessListener { last ->
                        val token = CancellationTokenSource()
                        val requestCurrent = {
                            fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
                                .addOnSuccessListener { loc ->
                                    val finalLat = loc?.latitude
                                    val finalLng = loc?.longitude
                                    if (finalLat == null || finalLng == null) {
                                        Toast.makeText(context, "Couldn't get location", Toast.LENGTH_SHORT).show(); return@addOnSuccessListener
                                    }
                                    handleWithLocation(finalLat, finalLng)
                                }
                                .addOnFailureListener { e -> Toast.makeText(context, "Location failed: ${e.message}", Toast.LENGTH_SHORT).show() }
                        }
                        if (last != null) {
                            handleWithLocation(last.latitude, last.longitude)
                        } else {
                            requestCurrent()
                        }
                    }.addOnFailureListener { e ->
                        Toast.makeText(context, "Location failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } },
            title = { Text("Save current location as CityTrip") },
            text = {
                Column {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = country, onValueChange = { country = it }, label = { Text("Country") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth().height(100.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Image", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) { Icon(Icons.Filled.Image, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Pick photo") }
                        Spacer(modifier = Modifier.width(8.dp))
                        val preview = imageUri
                        if (preview != null) {
                            AsyncImage(model = preview, contentDescription = null, modifier = Modifier.size(56.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("Or paste image URL") },
                        placeholder = { Text("https://…/photo.jpg") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (imageUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.size(56.dp))
                    }
                }
            }
        )
    }
}
