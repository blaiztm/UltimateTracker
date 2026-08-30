package com.example.ultimatetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ultimatetracker.navigation.AppNavigation
import com.example.ultimatetracker.ui.theme.UltimateTrackerTheme
import com.example.ultimatetracker.viewmodel.MediaViewModel
import com.example.ultimatetracker.viewmodel.MediaViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UltimateTrackerTheme {
                val app = application as UltimateTrackerApplication
                val viewModel: MediaViewModel = viewModel(factory = MediaViewModelFactory(app.repository))
                AppNavigation(viewModel)
            }
        }
    }
}
