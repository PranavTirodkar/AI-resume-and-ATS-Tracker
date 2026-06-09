package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.ResumeRepository
import com.example.ui.ResumeCoachScreen
import com.example.ui.ResumeViewModel
import com.example.ui.ResumeViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Build database and repository interfaces for Resume Coach App
        val database = AppDatabase.getDatabase(this)
        val repository = ResumeRepository(database.resumeDao())

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Jetpack Compose local ViewModel retrieval mechanism
                    val viewModel: ResumeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = ResumeViewModelFactory(repository)
                    )

                    ResumeCoachScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
