package com.chrislentner.coach.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import com.chrislentner.coach.database.AppDatabase
import com.chrislentner.coach.database.ScheduleRepository
import com.chrislentner.coach.database.WorkoutRepository

@Composable
fun CoachApp(
    repository: ScheduleRepository,
    startDestination: String = "home"
) {
    val navController = rememberNavController()

    // Quick and dirty DB access for Workout
    val context = LocalContext.current
    val db = androidx.room.Room.databaseBuilder(
        context,
        AppDatabase::class.java, "coach-database"
    )
        .fallbackToDestructiveMigration() // Dev only
        .build()
    val workoutRepository = WorkoutRepository(db.workoutDao())

    NavHost(navController = navController, startDestination = startDestination) {
        composable("home") {
            HomeScreen(navController = navController, repository = repository)
        }
        composable("survey") {
            SurveyScreen(navController = navController, repository = repository)
        }
        composable("workout") {
            val viewModel: WorkoutViewModel = viewModel(
                factory = WorkoutViewModelFactory(workoutRepository)
            )
            WorkoutScreen(navController = navController, viewModel = viewModel)
        }
    }
}
