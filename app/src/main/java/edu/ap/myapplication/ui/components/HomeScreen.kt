package edu.ap.myapplication.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import edu.ap.myapplication.model.CityTrip
import java.util.Locale
import com.google.firebase.Timestamp
import androidx.compose.material3.MenuAnchorType
import androidx.core.content.ContextCompat
import kotlinx.coroutines.tasks.await

fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
    val result = FloatArray(1)
    Location.distanceBetween(lat1, lng1, lat2, lng2, result)
    return result[0]
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenDetail: (CityTrip) -> Unit = {}) {
    val allTrips = remember { mutableStateListOf<CityTrip>() }
    val trips = remember { mutableStateListOf<CityTrip>() }

    val cityNames = remember { mutableStateListOf<String>() }

    var selectedCity by remember { mutableStateOf("All") }
    var selectedCountry by remember { mutableStateOf("All") }

    val context = LocalContext.current
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }
    var currentLat by remember { mutableStateOf<Double?>(null) }
    var currentLng by remember { mutableStateOf<Double?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        try {
            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (hasFine || hasCoarse) {
                val last = fused.lastLocation.await()
                if (last != null) {
                    currentLat = last.latitude
                    currentLng = last.longitude
                } else {
                    val token = CancellationTokenSource()
                    val loc = fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token).await()
                    if (loc != null) {
                        currentLat = loc.latitude
                        currentLng = loc.longitude
                    }
                }
            } else {
                Log.w(TAG, "Geen locatiepermissie")
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Locatiepermissie geweigerd: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Kon huidige locatie niet ophalen: ${e.message}")
        }
    }

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

    fun fetchCityNames() {
        FirebaseFirestore.getInstance()
            .collection("cities")
            .get()
            .addOnSuccessListener { snapshot ->
                cityNames.clear()
                val names = snapshot.documents.mapNotNull { it.getString("name")?.trim() }
                    .filter { it.isNotBlank() }
                cityNames.addAll(names.distinct().sorted())
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to fetch city names: ${e.message}", e)
            }
    }

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
                fetchCityNames()
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
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
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

    androidx.compose.runtime.LaunchedEffect(true) {
        fetchCityNames()
    }

    val cityOptions = listOf("All") + (allTrips.map { it.city } + cityNames).filter { it.isNotBlank() }.distinct().sorted()
    val countryOptions = listOf("All") + allTrips.map { it.country }.filter { it.isNotBlank() }.distinct().sorted()

    val hasFilterApplied = selectedCity != "All" || selectedCountry != "All"

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
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    DropdownMenu(expanded = cityExpanded, onDismissRequest = { cityExpanded = false }) {
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
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    DropdownMenu(expanded = countryExpanded, onDismissRequest = { countryExpanded = false }) {
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

        val mapTrips = trips.filter { it.latitude != null && it.longitude != null }

        if (hasFilterApplied && mapTrips.isNotEmpty()) {
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(8.dp)),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                OSMMap(
                    trips = mapTrips,
                    modifier = Modifier.fillMaxSize(),
                    currentLat = currentLat,
                    currentLng = currentLng,
                    calculateDistance = ::calculateDistance
                )
            }
        }

        if (trips.isEmpty()) {
            val hasData = allTrips.isNotEmpty()
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(if (hasData) "No results for selected filters" else "No city trips found")
            }
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
            items(trips) { city ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable {
                            val lat = city.latitude
                            val lng = city.longitude
                            if (lat != null && lng != null && currentLat != null && currentLng != null) {
                                val dist = calculateDistance(currentLat!!, currentLng!!, lat, lng)
                                val distStr = if (dist >= 1000) String.format(Locale.getDefault(), "%.1f km", dist/1000) else String.format(Locale.getDefault(), "%.0f m", dist)
                                Toast.makeText(context, "Afstand tot deze locatie: $distStr", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Locatiegegevens niet beschikbaar", Toast.LENGTH_SHORT).show()
                            }
                            onOpenDetail(city)
                        },
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
        val username: String?,
        val rating: Int,
        val comment: String
    )

    val context = LocalContext.current
    val reviews = remember { mutableStateListOf<Review>() }

    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }
    var currentLat by remember { mutableStateOf<Double?>(null) }
    var currentLng by remember { mutableStateOf<Double?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        try {
            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (hasFine || hasCoarse) {
                val last = fused.lastLocation.await()
                if (last != null) {
                    currentLat = last.latitude
                    currentLng = last.longitude
                } else {
                    val token = CancellationTokenSource()
                    val loc = fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token).await()
                    if (loc != null) {
                        currentLat = loc.latitude
                        currentLng = loc.longitude
                    }
                }
            } else {
                Log.w(TAG, "Geen locatiepermissie (detail)")
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Locatiepermissie geweigerd (detail): ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Kon huidige locatie niet ophalen (detail): ${e.message}")
        }
    }

    DisposableEffect(city.id) {
        val db = FirebaseFirestore.getInstance()
        val listener = db.collection("citytrips").document(city.id)
            .addSnapshotListener { doc, err ->
                if (err != null) {
                    Log.w(TAG, "Failed to load reviews from document: ${err.message}")
                    return@addSnapshotListener
                }
                reviews.clear()
                if (doc != null && doc.exists()) {
                    val arr = doc.get("reviews") as? List<*>
                    if (!arr.isNullOrEmpty()) {
                        arr.forEachIndexed { idx, item ->
                            val map = item as? Map<*, *>
                            val rating = (map?.get("rating") as? Number)?.toInt() ?: 0
                            val comment = (map?.get("description") as? String)
                                ?: (map?.get("comment") as? String) ?: ""
                            val username = map?.get("username") as? String ?: map?.get("userEmail") as? String
                            val userId = map?.get("userId") as? String
                            reviews.add(Review(idx.toString(), userId, username, rating, comment))
                        }
                    } else {
                        db.collection("citytrips").document(city.id).collection("reviews")
                            .get()
                            .addOnSuccessListener { snap2 ->
                                snap2?.documents?.forEach { d2 ->
                                    val rating = (d2.getLong("rating") ?: 0L).toInt()
                                    val comment = d2.getString("comment") ?: ""
                                    val uid = d2.getString("userId")
                                    val email = d2.getString("userEmail")
                                    reviews.add(Review(d2.id, uid, email, rating, comment))
                                }
                            }
                            .addOnFailureListener { e -> Log.w(TAG, "Failed to load subcollection reviews: ${e.message}") }
                    }
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

        val cityLat = city.latitude
        val cityLng = city.longitude
        if (cityLat != null && cityLng != null && currentLat != null && currentLng != null) {
            val dist = calculateDistance(currentLat!!, currentLng!!, cityLat, cityLng)
            val distStr = if (dist >= 1000f) String.format(Locale.getDefault(), "%.1f km", dist / 1000f) else String.format(Locale.getDefault(), "%.0f m", dist)
            Text(distStr, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(12.dp))
        }

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
                    if (!r.username.isNullOrBlank()) {
                        Text(r.username, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            val username = user.displayName ?: user.email ?: "Anonymous"
            val reviewObj = hashMapOf<String, Any>(
                "username" to username,
                "userId" to user.uid,
                "description" to commentText,
                "rating" to selectedRating,
                "timestamp" to Timestamp.now()
            )

            db.collection("citytrips").document(city.id)
                .update("reviews", FieldValue.arrayUnion(reviewObj))
                .addOnSuccessListener {
                    Toast.makeText(context, "Review posted", Toast.LENGTH_SHORT).show()
                    commentText = ""
                    selectedRating = 4
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Array update failed, attempting safe write: ${e.message}")
                    db.collection("citytrips").document(city.id).get()
                        .addOnSuccessListener { docSnap ->
                            try {
                                val currentRaw = docSnap.get("reviews")
                                val newList = ArrayList<Any>()
                                if (currentRaw is List<*>) newList.addAll(currentRaw.filterNotNull())
                                newList.add(reviewObj)
                                db.collection("citytrips").document(city.id)
                                    .update("reviews", newList)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Review posted", Toast.LENGTH_SHORT).show()
                                        commentText = ""
                                        selectedRating = 4
                                    }
                                    .addOnFailureListener { e2 -> Toast.makeText(context, "Failed: ${e2.message}", Toast.LENGTH_SHORT).show() }
                            } catch (ex: Exception) {
                                Toast.makeText(context, "Failed to post review: ${ex.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e2 -> Toast.makeText(context, "Failed: ${e2.message}", Toast.LENGTH_SHORT).show() }
                }
        }) { Text("Post review") }

    }
}
