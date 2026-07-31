package com.hoosha.examai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hoosha.examai.ui.ExamApp
import com.hoosha.examai.ui.ExamAiTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ExamAiTheme {
                ExamApp()
            }
        }
    }
}