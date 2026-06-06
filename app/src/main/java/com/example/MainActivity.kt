package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ui.ContactsViewModel
import com.example.ui.screens.AddEditContactScreen
import com.example.ui.screens.ContactDetailScreen
import com.example.ui.screens.ContactsListScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val viewModel: ContactsViewModel by viewModels {
                    ContactsViewModel.Factory(application)
                }

                // Check and track if contacts read permission has been granted
                var hasReadPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.READ_CONTACTS
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasReadPermission = isGranted
                }

                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "list",
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. Contacts List screen
                    composable("list") {
                        ContactsListScreen(
                            navController = navController,
                            viewModel = viewModel,
                            onRequestPermission = {
                                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            },
                            hasReadPermission = hasReadPermission
                        )
                    }

                    // 2. Contact Detail screen
                    composable(
                        route = "detail/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("id") ?: ""
                        ContactDetailScreen(
                            navController = navController,
                            viewModel = viewModel,
                            contactId = id
                        )
                    }

                    // 3. Add or Edit Contact screen
                    composable(
                        route = "add_edit?id={id}",
                        arguments = listOf(
                            navArgument("id") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("id")
                        AddEditContactScreen(
                            navController = navController,
                            viewModel = viewModel,
                            contactId = id
                        )
                    }
                }
            }
        }
    }
}
