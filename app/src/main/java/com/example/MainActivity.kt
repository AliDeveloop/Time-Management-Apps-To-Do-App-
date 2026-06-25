package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.TaskRepository
import com.example.ui.AppViewModel
import com.example.ui.screens.RoozAraAppContent
import com.example.ui.theme.RoozAraTheme
import com.example.ui.theme.Translation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize Room Database and Repository
        val database = AppDatabase.getDatabase(this)
        val repository = TaskRepository(database.taskDao())

        // 2. Instantiate central ViewModel using Factory
        val viewModelFactory = AppViewModel.Factory(application, repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[AppViewModel::class.java]

        setContent {
            RoozAraTheme {
                val currentLanguage = viewModel.currentLanguage
                val layoutDirection = Translation.getLayoutDirection(currentLanguage)

                // 3. Set RTL / LTR dynamically based on translation setting
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    RoozAraAppContent(viewModel = viewModel)
                }
            }
        }
    }
}
