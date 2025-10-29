package edu.ap.myapplication.ui.components

import android.content.ContentValues.TAG
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.FirebaseStorage
import edu.ap.myapplication.model.CityTrip
import java.util.UUID
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenDetail: (CityTrip) -> Unit = {}) {
    val allTrips = remember { mutableStateListOf<CityTrip>() }
    val trips = remember { mutableStateListOf<CityTrip>() }

    var selectedCity by remember { mutableStateOf("All") }
    var selectedCountry by remember { mutableStateOf("All") }

    fun applyFilter() {
        trips.clear()
        trips.addAll(
            allTrips.filter { ct ->
                (selectedCity == "All" || ct.city == selectedCity) &&
                (selectedCountry == "All" || ct.country == selectedCountry)
            }
        )
    }

    val isRefreshing = remember { mutableStateOf(false) }
    val context = LocalContext.current

    val fetchOnce: () -> Unit = {
        isRefreshing.value = true
        FirebaseFirestore.getInstance()
            .collection("citytrips")
            .get()
            .addOnSuccessListener { snapshot ->
                allTrips.clear()
                for (doc in snapshot.documents) {
                    try {
                        val ct = CityTrip.fromSnapshot(doc)
                        allTrips.add(ct)
                    } catch (_: Throwable) { }
                }
                applyFilter()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Manual fetch failed from citytrips: ${e.message}", e)
                Toast.makeText(context, "Failed to refresh citytrips", Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener { _ -> isRefreshing.value = false }
    }

    fun fetchFallbackFromCities() {
        Log.d(TAG, "Attempting fallback read from 'cities'")
        FirebaseFirestore.getInstance()
            .collection("cities")
            .get()
            .addOnSuccessListener { snapshot ->
                allTrips.clear()
                for (doc in snapshot.documents) {
                    try {
                        val ct = CityTrip.fromSnapshot(doc)
                        allTrips.add(ct)
                    } catch (_: Throwable) { }
                }
                applyFilter()
                Toast.makeText(context, "Loaded fallback collection 'cities'", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Fallback fetch failed: ${e.message}", e)
                Toast.makeText(context, "Failed to load city data", Toast.LENGTH_SHORT).show()
            }
    }

    DisposableEffect(Unit) {
        val authUser = FirebaseAuth.getInstance().currentUser
        Log.d(TAG, "Current user uid=${authUser?.uid}, email=${authUser?.email}")
        val listener = FirebaseFirestore.getInstance()
            .collection("citytrips")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed for collection 'citytrips': ${error.message}", error)
                    if (error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.w(TAG, "Permission denied on 'citytrips'; attempting fallback to 'cities'.")
                        fetchFallbackFromCities()
                    } else {
                        Toast.makeText(context, "Failed to listen for citytrips: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    allTrips.clear()
                    for (doc in snapshot.documents) {
                        try {
                            val ct = CityTrip.fromSnapshot(doc)
                            allTrips.add(ct)
                        } catch (_: Throwable) { }
                    }
                    applyFilter()
                }
            }
        onDispose { listener.remove() }
    }

    val cityOptions = listOf("All") + allTrips.map { it.city }.filter { it.isNotBlank() }.distinct().sorted()
    val countryOptions = listOf("All") + allTrips.map { it.country }.filter { it.isNotBlank() }.distinct().sorted()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                var cityExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = cityExpanded, onExpandedChange = { cityExpanded = !cityExpanded }) {
                    OutlinedTextField(
                        readOnly = true,
                        value = if (selectedCity == "All") "All cities" else selectedCity,
                        onValueChange = {},
                        label = { Text("Filter by city") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = cityExpanded, onDismissRequest = { cityExpanded = false }) {
                        cityOptions.forEach { option ->
                            DropdownMenuItem(text = { Text(if (option == "All") "All cities" else option) }, onClick = {
                                selectedCity = option
                                cityExpanded = false
                                applyFilter()
                            })
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 6.dp)) {
                var countryExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = countryExpanded, onExpandedChange = { countryExpanded = !countryExpanded }) {
                    OutlinedTextField(
                        readOnly = true,
                        value = if (selectedCountry == "All") "All countries" else selectedCountry,
                        onValueChange = {},
                        label = { Text("Filter by country") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = countryExpanded, onDismissRequest = { countryExpanded = false }) {
                        countryOptions.forEach { option ->
                            DropdownMenuItem(text = { Text(if (option == "All") "All countries" else option) }, onClick = {
                                selectedCountry = option
                                countryExpanded = false
                                applyFilter()
                            })
                        }
                    }
                }
            }

            IconButton(onClick = { fetchOnce() }, modifier = Modifier.padding(start = 6.dp)) {
                if (isRefreshing.value) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Refresh")
                }
            }
        }

        if (trips.isEmpty()) {
            val hasData = allTrips.isNotEmpty()
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(if (hasData) "No results for selected filters" else "No city trips found")
            }
            return
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
            items(trips) { city ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onOpenDetail(city) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = city.image,
                            contentDescription = city.title,
                            modifier = Modifier.size(92.dp).padding(end = 12.dp),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(city.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("${city.city}, ${city.country}", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CityDetailScreen(city: CityTrip?, onBack: () -> Unit = {}) {
    if (city == null) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("City not found")
        }
        return
    }

    data class Review(
        val id: String,
        val userId: String?,
        val userEmail: String?,
        val rating: Int,
        val comment: String
    )

    val context = LocalContext.current
    val reviews = remember { mutableStateListOf<Review>() }

    DisposableEffect(city.id) {
        val db = FirebaseFirestore.getInstance()
        val listener = db.collection("citytrips").document(city.id).collection("reviews")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.w(TAG, "Failed to load reviews: ${err.message}")
                    return@addSnapshotListener
                }
                reviews.clear()
                snap?.documents?.forEach { d ->
                    val rating = (d.getLong("rating") ?: 0L).toInt()
                    val comment = d.getString("comment") ?: ""
                    val uid = d.getString("userId")
                    val email = d.getString("userEmail")
                    reviews.add(Review(d.id, uid, email, rating, comment))
                }
            }
        onDispose { listener.remove() }
    }

    var selectedRating by remember { mutableStateOf(4) }
    var commentText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(city.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        Spacer(modifier = Modifier.height(12.dp))

        AsyncImage(model = city.image, contentDescription = city.title, modifier = Modifier
            .fillMaxWidth()
            .height(220.dp), contentScale = ContentScale.Crop)

        Spacer(modifier = Modifier.height(12.dp))
        Text("${city.city}, ${city.country}", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Text(city.description)

        Spacer(modifier = Modifier.height(16.dp))
        Text("Reviews", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        if (reviews.isEmpty()) {
            Text("No reviews yet")
        } else {
            reviews.forEach { r ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { idx ->
                            val filled = idx < r.rating
                            if (filled) Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            else Icon(Icons.Outlined.StarBorder, contentDescription = null)
                        }
                    }
                    if (r.comment.isNotBlank()) {
                        Text(r.comment)
                    }
                    if (!r.userEmail.isNullOrBlank()) {
                        Text(r.userEmail ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Add your review", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(5) { i ->
                val idx = i + 1
                Icon(
                    if (idx <= selectedRating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = null,
                    tint = if (idx <= selectedRating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp).padding(2.dp).clickable { selectedRating = idx }
                )
            }
        }
        OutlinedTextField(
            value = commentText,
            onValueChange = { commentText = it },
            label = { Text("Comment") },
            modifier = Modifier.fillMaxWidth().height(100.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Toast.makeText(context, "Login required to post a review", Toast.LENGTH_SHORT).show()
                return@Button
            }
            val db = FirebaseFirestore.getInstance()
            val data = hashMapOf(
                "rating" to selectedRating,
                "comment" to commentText,
                "userId" to user.uid,
                "userEmail" to (user.email ?: ""),
                "timestamp" to FieldValue.serverTimestamp()
            )
            db.collection("citytrips").document(city.id).collection("reviews")
                .add(data)
                .addOnSuccessListener {
                    Toast.makeText(context, "Review posted", Toast.LENGTH_SHORT).show()
                    commentText = ""
                    selectedRating = 4
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }) {
            Text("Post review")
        }
    }
}

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
        val lastTask = fused.lastLocation.addOnSuccessListener { loc ->
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