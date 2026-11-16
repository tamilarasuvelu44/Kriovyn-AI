package com.orbita.personalagent.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.orbita.personalagent.ui.screens.AiConnectionStatusScreen
import com.orbita.personalagent.ui.screens.ApprovalViewModel
import com.orbita.personalagent.ui.screens.ChatScreen
import com.orbita.personalagent.ui.screens.ChatViewModel
import com.orbita.personalagent.ui.screens.HistoryScreen
import com.orbita.personalagent.ui.screens.HistoryViewModel
import com.orbita.personalagent.ui.screens.MainViewModel
import com.orbita.personalagent.ui.screens.MemoryScreen
import com.orbita.personalagent.ui.screens.MemoryViewModel
import com.orbita.personalagent.ui.screens.NotesScreen
import com.orbita.personalagent.ui.screens.NotesViewModel
import com.orbita.personalagent.ui.screens.SettingsScreen
import com.orbita.personalagent.ui.screens.SettingsViewModel
import com.orbita.personalagent.ui.screens.SplashScreen
import com.orbita.personalagent.ui.screens.TasksScreen
import com.orbita.personalagent.ui.screens.TasksViewModel
import com.orbita.personalagent.ui.screens.ToolApprovalScreen
import com.orbita.personalagent.ui.screens.WelcomeScreen
import kotlinx.serialization.Serializable

sealed interface KriovynRoute {
    @Serializable data object Splash : KriovynRoute
    @Serializable data object Welcome : KriovynRoute
    @Serializable data object Chat : KriovynRoute
    @Serializable data object History : KriovynRoute
    @Serializable data object Tasks : KriovynRoute
    @Serializable data object Notes : KriovynRoute
    @Serializable data object Memory : KriovynRoute
    @Serializable data object ToolApproval : KriovynRoute
    @Serializable data object Settings : KriovynRoute
    @Serializable data object ConnectionStatus : KriovynRoute
}

private data class BottomNavTab(
    val route: KriovynRoute,
    val label: String,
    val icon: ImageVector
)

private val PrimaryTabs = listOf(
    BottomNavTab(KriovynRoute.Chat, "Chat", Icons.Rounded.AutoAwesome),
    BottomNavTab(KriovynRoute.Tasks, "Tasks", Icons.Rounded.TaskAlt),
    BottomNavTab(KriovynRoute.Notes, "Notes", Icons.Rounded.NoteAlt),
    BottomNavTab(KriovynRoute.Memory, "Memory", Icons.Rounded.Memory),
    BottomNavTab(KriovynRoute.ToolApproval, "Approvals", Icons.Rounded.Security)
)

@Composable
fun KriovynNavHost(
    viewModelFactory: ViewModelProvider.Factory,
    mainViewModel: MainViewModel
) {
    val navController = rememberNavController()
    val prefs by mainViewModel.preferences.collectAsStateWithLifecycle()
    val pendingApprovals by mainViewModel.pendingApprovals.collectAsStateWithLifecycle()

    val chatViewModel: ChatViewModel = viewModel(factory = viewModelFactory)
    val historyViewModel: HistoryViewModel = viewModel(factory = viewModelFactory)
    val tasksViewModel: TasksViewModel = viewModel(factory = viewModelFactory)
    val notesViewModel: NotesViewModel = viewModel(factory = viewModelFactory)
    val memoryViewModel: MemoryViewModel = viewModel(factory = viewModelFactory)
    val approvalViewModel: ApprovalViewModel = viewModel(factory = viewModelFactory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination

    val showBottomBar = PrimaryTabs.any { tab ->
        currentDest?.hasRoute(tab.route::class) == true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    PrimaryTabs.forEach { tab ->
                        val selected = currentDest?.hasRoute(tab.route::class) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                if (tab.route == KriovynRoute.ToolApproval && pendingApprovals.isNotEmpty()) {
                                    BadgedBox(
                                        badge = { Badge { Text(pendingApprovals.size.toString()) } }
                                    ) {
                                        Icon(tab.icon, contentDescription = tab.label)
                                    }
                                } else {
                                    Icon(tab.icon, contentDescription = tab.label)
                                }
                            },
                            label = { Text(tab.label, maxLines = 1) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = KriovynRoute.Splash,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<KriovynRoute.Splash> {
                SplashScreen(
                    onboardingCompleted = prefs.onboardingCompleted,
                    onNavigateToWelcome = {
                        navController.navigate(KriovynRoute.Welcome) {
                            popUpTo<KriovynRoute.Splash> { inclusive = true }
                        }
                    },
                    onNavigateToChat = {
                        navController.navigate(KriovynRoute.Chat) {
                            popUpTo<KriovynRoute.Splash> { inclusive = true }
                        }
                    }
                )
            }

            composable<KriovynRoute.Welcome> {
                WelcomeScreen(
                    onGetStarted = {
                        mainViewModel.completeOnboarding()
                        navController.navigate(KriovynRoute.Chat) {
                            popUpTo<KriovynRoute.Welcome> { inclusive = true }
                        }
                    }
                )
            }

            composable<KriovynRoute.Chat> {
                ChatScreen(
                    viewModel = chatViewModel,
                    onOpenHistory = { navController.navigate(KriovynRoute.History) },
                    onOpenSettings = { navController.navigate(KriovynRoute.Settings) },
                    onOpenConnectionStatus = { navController.navigate(KriovynRoute.ConnectionStatus) },
                    onOpenToolApproval = { navController.navigate(KriovynRoute.ToolApproval) }
                )
            }

            composable<KriovynRoute.History> {
                HistoryScreen(
                    viewModel = historyViewModel,
                    onSelectSession = { sessionId ->
                        chatViewModel.openSession(sessionId)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<KriovynRoute.Tasks> {
                TasksScreen(viewModel = tasksViewModel)
            }

            composable<KriovynRoute.Notes> {
                NotesScreen(viewModel = notesViewModel)
            }

            composable<KriovynRoute.Memory> {
                MemoryScreen(viewModel = memoryViewModel)
            }

            composable<KriovynRoute.ToolApproval> {
                ToolApprovalScreen(viewModel = approvalViewModel)
            }

            composable<KriovynRoute.Settings> {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onOpenConnectionStatus = { navController.navigate(KriovynRoute.ConnectionStatus) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<KriovynRoute.ConnectionStatus> {
                AiConnectionStatusScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
