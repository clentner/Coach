package com.chrislentner.coach.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chrislentner.coach.database.ScheduleRepository
import com.chrislentner.coach.database.WorkoutRepository
import com.chrislentner.coach.planner.AdvancedWorkoutPlanner

@Composable
fun CoachApp(
    repository: ScheduleRepository,
    workoutRepository: WorkoutRepository,
    planner: AdvancedWorkoutPlanner?,
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
        composable("suggested_schedule") {
            val viewModel: SuggestedScheduleViewModel = viewModel(
                factory = SuggestedScheduleViewModelFactory(workoutRepository, repository, planner)
            )
            SuggestedScheduleScreen(navController = navController, viewModel = viewModel)
        }
        composable("suggested_schedule_detail/{dayIndex}") { backStackEntry ->
            val dayIndex = backStackEntry.arguments?.getString("dayIndex")?.toIntOrNull() ?: 0
            val parentEntry = remember { navController.getBackStackEntry("suggested_schedule") }
            val viewModel: SuggestedScheduleViewModel = viewModel(
                parentEntry,
                factory = SuggestedScheduleViewModelFactory(workoutRepository, repository, planner)
            )
            SuggestedScheduleDetailScreen(
                navController = navController,
                viewModel = viewModel,
                dayIndex = dayIndex
            )
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
        composable(
            "edit_exercise/{sessionId}?logId={logId}",
            arguments = listOf(
                navArgument("sessionId") { type = NavType.LongType },
                navArgument("logId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
            val logIdArg = backStackEntry.arguments?.getLong("logId") ?: -1L
            val logId = if (logIdArg == -1L) null else logIdArg

            val viewModel: EditExerciseViewModel = viewModel(
                factory = EditExerciseViewModelFactory(workoutRepository, sessionId, logId)
            )
            EditExerciseScreen(navController = navController, viewModel = viewModel)
        }
    }
}
