package com.ptsl.crowdsourcing_host_app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ptsl.crowdsourcing_host_app.ui.screens.FTPMeasurementScreen
import com.ptsl.crowdsourcing_host_app.ui.screens.RecentTestsScreen
import com.ptsl.crowdsourcing_host_app.ui.screens.StandardMeasurementScreen
import com.ptsl.crowdsourcing_host_app.ui.screens.getMockTestResults
import com.ptsl.crowdsourcing_host_app.ui.screens.HomeScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object ViewPager : Screen("view_pager")
    object StandardMeasurement : Screen("standard_measurement")
    object FTPMeasurement : Screen("ftp_measurement")
    object RecentTests : Screen("recent_tests")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    onStartCrowdsourcing: (String, String) -> Unit,
    onTripleCapture: () -> Unit
) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                onSingleCapture = { eventName ->
                    onStartCrowdsourcing(eventName, "NetworkDataCapture")
                },
                onTripleCapture = onTripleCapture
            )
        }
        
        composable(Screen.ViewPager.route) {
            ViewPagerScreen(
                navController = navController,
                onStartCrowdsourcing = onStartCrowdsourcing
            )
        }
        
        composable(Screen.StandardMeasurement.route) {
            StandardMeasurementScreen(
                onBackClick = { navController.popBackStack() },
                onStartCapture = onStartCrowdsourcing
            )
        }
        
        composable(Screen.FTPMeasurement.route) {
            FTPMeasurementScreen(
                onBackClick = { navController.popBackStack() },
                onStartCapture = onStartCrowdsourcing
            )
        }
        
        composable(Screen.RecentTests.route) {
            RecentTestsScreen(
                onBackClick = { navController.popBackStack() },
                testResults = getMockTestResults()
            )
        }
    }
}
