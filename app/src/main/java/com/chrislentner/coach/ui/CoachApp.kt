package com.chrislentner.coach.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chrislentner.coach.database.ScheduleRepository
import com.chrislentner.coach.database.WorkoutRepository
import com.chrislentner.coach.planner.AdvancedWorkoutPlanner

@Composable
fun CoachApp(
    repository: ScheduleRepository,
    workoutRepository: WorkoutRepository,
    planner: AdvancedWorkoutPlanner,
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
        composable("workout") {
            // repository is ScheduleRepository, passing it to factory
            val viewModel: WorkoutViewModel = viewModel(
                factory = WorkoutViewModelFactory(workoutRepository, repository, planner)
            )
            WorkoutScreen(navController = navController, viewModel = viewModel)
        }
        composable("past_workouts") {
            val viewModel: PastWorkoutsViewModel = viewModel(
                factory = PastWorkoutsViewModelFactory(workoutRepository)
            )
            PastWorkoutsScreen(navController = navController, viewModel = viewModel)
        }
        composable("workout_detail/{sessionId}") { backStackEntry ->
            val sessionIdStr = backStackEntry.arguments?.getString("sessionId")
            val sessionId = sessionIdStr?.toLongOrNull() ?: 0L
            val viewModel: WorkoutDetailViewModel = viewModel(
                factory = WorkoutDetailViewModelFactory(workoutRepository, sessionId)
            )
            WorkoutDetailScreen(navController = navController, viewModel = viewModel)
        }
        composable("exercise_selection") {
            ExerciseSelectionScreen(navController = navController, repository = workoutRepository)
        }
    }
}
