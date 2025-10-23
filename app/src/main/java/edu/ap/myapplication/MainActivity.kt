package edu.ap.myapplication

import NotificationsScreen
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import edu.ap.myapplication.ui.components.BottomNavBar
import edu.ap.myapplication.ui.components.BottomNavScreen
import edu.ap.myapplication.ui.components.HomeScreen
import edu.ap.myapplication.ui.components.MapScreen
import edu.ap.myapplication.ui.components.ProfileScreen
import edu.ap.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.material3.TopAppBar
import edu.ap.myapplication.ui.components.LoginScreen

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var isLoggedIn by rememberSaveable { mutableStateOf(false) }
                var currentRoute by rememberSaveable { mutableStateOf(BottomNavScreen.Home.route) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (currentRoute == BottomNavScreen.Home.route) {
                            TopAppBar(
                                title = { Text("City trip recommendations") },
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
                        if (!isLoggedIn) {
                            LoginScreen(
                                onLogin = { isLoggedIn = true },
                                onSkip = { isLoggedIn = true }
                            )
                        } else {
                            when (currentRoute) {
                                BottomNavScreen.Home.route -> HomeScreen()
                                BottomNavScreen.Map.route -> MapScreen()
                                BottomNavScreen.Profile.route -> ProfileScreen(
                                    onLogout = {
                                        isLoggedIn = false
                                        currentRoute = BottomNavScreen.Home.route
                                    }
                                )
                                "notifications" -> NotificationsScreen(
                                    onBack = { currentRoute = BottomNavScreen.Home.route }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}