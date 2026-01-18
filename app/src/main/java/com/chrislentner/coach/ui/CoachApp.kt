package com.chrislentner.coach.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chrislentner.coach.database.ScheduleRepository
import com.chrislentner.coach.database.WorkoutRepository
import com.chrislentner.coach.planner.AdvancedWorkoutPlanner

@Composable
fun CoachApp(
    workoutRepo: WorkoutRepository,
    scheduleRepo: ScheduleRepository,
    planner: AdvancedWorkoutPlanner?
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController, scheduleRepo)
        }
        composable("workout") {
            val viewModel: WorkoutViewModel = viewModel(
                factory = WorkoutViewModelFactory(workoutRepo, scheduleRepo, planner)
            )
            WorkoutScreen(navController, viewModel)
        }
        composable("past_workouts") {
            val viewModel: PastWorkoutsViewModel = viewModel(
                factory = PastWorkoutsViewModelFactory(workoutRepo)
            )
            PastWorkoutsScreen(navController, viewModel)
        }
        composable(
            "workout_detail/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId")
            requireNotNull(sessionId) { "SessionId parameter not found" }

            val viewModel: WorkoutDetailViewModel = viewModel(
                factory = WorkoutDetailViewModelFactory(workoutRepo, sessionId)
            )
            WorkoutDetailScreen(navController, viewModel)
        }
        composable(
            route = "historical_workout?sessionId={sessionId}",
            arguments = listOf(navArgument("sessionId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId")
            val viewModel: HistoricalWorkoutViewModel = viewModel(
                factory = HistoricalWorkoutViewModelFactory(workoutRepo, if (sessionId == -1L) null else sessionId)
            )
            HistoricalWorkoutScreen(navController, viewModel)
        }
        composable("exercise_selection") {
            val viewModel: ExerciseSelectionViewModel = viewModel(
                factory = ExerciseSelectionViewModelFactory(workoutRepo)
            )
            ExerciseSelectionScreen(navController, viewModel)
        }
        composable("survey") {
            SurveyScreen(navController, scheduleRepo)
        }
    }
}
