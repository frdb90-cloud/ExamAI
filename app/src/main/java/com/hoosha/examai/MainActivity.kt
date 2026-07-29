package com.hoosha.examai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.hoosha.examai.presentation.navigation.ExamAiNavigation
import com.hoosha.examai.presentation.theme.ExamAiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ExamAiTheme {
                ExamAiNavigation(modifier = Modifier.fillMaxSize())
            }
        }
    }
}