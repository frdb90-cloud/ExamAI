package com.hoosha.examai.presentation.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hoosha.examai.presentation.analysis.AnalysisScreen
import com.hoosha.examai.presentation.createexam.CreateExamScreen
import com.hoosha.examai.presentation.history.HistoryScreen
import com.hoosha.examai.presentation.results.ResultsScreen
import com.hoosha.examai.presentation.review.ReviewExamScreen
import com.hoosha.examai.presentation.sources.SourcesScreen

object Routes {
    const val SOURCES = "sources"
    const val HISTORY = "history"
    const val CREATE_EXAM = "create-exam"
    const val REVIEW_EXAM = "review/{examId}"
    const val ANALYSIS = "analysis/{examId}"
    const val RESULTS = "results/{examId}"

    fun review(examId: String): String =
        "review/${Uri.encode(examId)}"

    fun analysis(examId: String): String =
        "analysis/${Uri.encode(examId)}"

    fun results(examId: String): String =
        "results/${Uri.encode(examId)}"
}

@Composable
fun ExamAiNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SOURCES
    ) {
        composable(Routes.SOURCES) {
            SourcesScreen(
                onCreateExam = {
                    navController.navigate(Routes.CREATE_EXAM)
                },
                onOpenHistory = {
                    navController.navigate(Routes.HISTORY) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                onBack = navController::popBackStack,
                onOpenExam = { exam ->
                    val destination = when (exam.status) {
                        "COMPLETED" ->
                            Routes.results(exam.id)

                        "READY_TO_ANALYZE",
                        "ANALYZING" ->
                            Routes.analysis(exam.id)

                        else ->
                            Routes.review(exam.id)
                    }

                    navController.navigate(destination)
                }
            )
        }

        composable(Routes.CREATE_EXAM) {
            CreateExamScreen(
                onBack = navController::popBackStack,
                onReview = { examId ->
                    navController.navigate(
                        Routes.review(examId)
                    ) {
                        popUpTo(Routes.CREATE_EXAM) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(
            route = Routes.REVIEW_EXAM,
            arguments = listOf(
                navArgument("examId") {
                    type = NavType.StringType
                }
            )
        ) {
            ReviewExamScreen(
                onBack = navController::popBackStack,
                onAnalyze = { examId ->
                    navController.navigate(
                        Routes.analysis(examId)
                    )
                }
            )
        }

        composable(
            route = Routes.ANALYSIS,
            arguments = listOf(
                navArgument("examId") {
                    type = NavType.StringType
                }
            )
        ) {
            AnalysisScreen(
                onBack = navController::popBackStack,
                onCompleted = { examId ->
                    navController.navigate(
                        Routes.results(examId)
                    ) {
                        popUpTo(Routes.ANALYSIS) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(
            route = Routes.RESULTS,
            arguments = listOf(
                navArgument("examId") {
                    type = NavType.StringType
                }
            )
        ) {
            ResultsScreen(
                onBack = navController::popBackStack
            )
        }
    }
}