package com.chrislentner.coach.ui

import androidx.compose.runtime.Composable
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
    configExercises: List<String> = emptyList(),
    startDestination: String = "home"
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable("home") {
            HomeScreen(navController = navController, repository = repository)
        }
        composable(
            "survey?date={date}",
            arguments = listOf(
                navArgument("date") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            SurveyScreen(navController = navController, repository = repository, date = date)
        }
        composable("weekly_planner") {
            val viewModel: WeeklyPlannerViewModel = viewModel(
                factory = WeeklyPlannerViewModelFactory(repository)
            )
            WeeklyPlannerScreen(navController = navController, viewModel = viewModel)
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
            ExerciseSelectionScreen(
                navController = navController,
                repository = workoutRepository,
                configExercises = configExercises
            )
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
        composable("suggest_schedule") {
            if (planner != null) {
                val viewModel: SuggestScheduleViewModel = viewModel(
                    factory = SuggestScheduleViewModelFactory(workoutRepository, repository, planner)
                )
                SuggestScheduleScreen(navController = navController, viewModel = viewModel)
            } else {
                androidx.compose.material3.Text("Planner configuration not found.")
            }
        }
    }
}
