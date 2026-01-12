package com.chrislentner.coach.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chrislentner.coach.database.ScheduleRepository

@Composable
fun CoachApp(
    repository: ScheduleRepository,
    startDestination: String = "home"
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable("home") {
            HomeScreen(navController = navController, repository = repository)
        }
        composable("survey") {
            SurveyScreen(navController = navController, repository = repository)
        }
    }
}
