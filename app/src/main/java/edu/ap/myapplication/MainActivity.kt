package edu.ap.myapplication

import edu.ap.myapplication.ui.components.NotificationsScreen
import edu.ap.myapplication.ui.components.MessagesScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import edu.ap.myapplication.ui.components.BottomNavBar
import edu.ap.myapplication.ui.components.BottomNavScreen
import edu.ap.myapplication.ui.components.MapScreen
import edu.ap.myapplication.ui.components.ProfileScreen
import edu.ap.myapplication.ui.components.CityDetailScreen
import edu.ap.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import edu.ap.myapplication.ui.components.LoginScreen
import edu.ap.myapplication.model.CityTrip
import edu.ap.myapplication.ui.components.HomeScreen
import edu.ap.myapplication.ui.components.RegisterScreen

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var isLoggedIn by rememberSaveable { mutableStateOf(false) }
                var showRegister by rememberSaveable { mutableStateOf(false) }
                var currentRoute by rememberSaveable { mutableStateOf(BottomNavScreen.Home.route) }
                val selectedCity = remember { mutableStateOf<CityTrip?>(null) }

                if (!isLoggedIn) {
                    if (showRegister) {
                        RegisterScreen(
                            onRegistered = {
                                isLoggedIn = true
                                showRegister = false
                            },
                            onBackToLogin = {
                                showRegister = false
                            }
                        )
                    } else {
                        LoginScreen(
                            onLogin = {
                                isLoggedIn = true
                            },
                            onSkip = {
                                isLoggedIn = true
                            },
                            onCreateAccount = {
                                showRegister = true
                            }
                        )
                    }
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            if (currentRoute == BottomNavScreen.Home.route) {
                                TopAppBar(
                                    title = { Text("Location recommendations") },
                                    actions = {
                                        IconButton(onClick = { currentRoute = "notifications" }) {
                                            Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                                        }
                                    }
                                )
                            }
                        },
                        bottomBar = {
                            if (currentRoute != "notifications") {
                                BottomNavBar(currentRoute = currentRoute) { route ->
                                    currentRoute = route
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentRoute) {
                                BottomNavScreen.Home.route -> HomeScreen(onOpenDetail = { city ->
                                    selectedCity.value = city
                                    currentRoute = "cityDetail"
                                })
                                BottomNavScreen.Map.route -> MapScreen()
                                BottomNavScreen.Messages.route -> MessagesScreen()
                                BottomNavScreen.Profile.route -> ProfileScreen(
                                    onLogout = {
                                        isLoggedIn = false
                                        currentRoute = BottomNavScreen.Home.route
                                    }
                                )
                                "notifications" -> NotificationsScreen(
                                    onBack = { currentRoute = BottomNavScreen.Home.route }
                                )
                                "cityDetail" -> CityDetailScreen(selectedCity.value, onBack = {
                                    selectedCity.value = null
                                    currentRoute = BottomNavScreen.Home.route
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}