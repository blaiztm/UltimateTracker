package com.example.ultimatetracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ultimatetracker.navigation.AppNavigation
import com.example.ultimatetracker.ui.theme.UltimateTrackerTheme
import com.example.ultimatetracker.viewmodel.MediaViewModel
import com.example.ultimatetracker.viewmodel.MediaViewModelFactory
import com.example.ultimatetracker.viewmodel.AccountViewModel
import com.example.ultimatetracker.viewmodel.AccountViewModelFactory

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        }
        enableEdgeToEdge()
        setContent {
            UltimateTrackerTheme {
                val app = application as UltimateTrackerApplication
                val viewModel: MediaViewModel = viewModel(factory = MediaViewModelFactory(app.repository, app.tmdbClient))
                val accountViewModel: AccountViewModel = viewModel(factory = AccountViewModelFactory(app.accountRepository, app.backupRepository))
                AppNavigation(viewModel, accountViewModel)
            }
        }
    }
}
