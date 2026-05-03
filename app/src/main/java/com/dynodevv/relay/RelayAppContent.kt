package com.dynodevv.relay

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dynodevv.relay.ui.chat.ChatScreen
import com.dynodevv.relay.ui.chat.ChatViewModel
import com.dynodevv.relay.ui.providers.AddProviderScreen
import com.dynodevv.relay.ui.providers.ProvidersScreen
import com.dynodevv.relay.ui.providers.ProvidersViewModel
import com.dynodevv.relay.ui.settings.SettingsScreen
import com.dynodevv.relay.ui.models.ModelsScreen
import com.dynodevv.relay.ui.models.AddModelScreen
import com.dynodevv.relay.ui.models.ModelsViewModel

object Routes {
    const val CHAT = "chat/{conversationId}"
    const val CHAT_BASE = "chat/"
    const val PROVIDERS = "providers"
    const val ADD_PROVIDER = "add_provider"
    const val MODELS = "models/{providerId}"
    const val ADD_MODEL = "add_model/{providerId}"
    const val SETTINGS = "settings"

    fun chat(conversationId: Long = 0L) = "chat/$conversationId"
    fun models(providerId: Long) = "models/$providerId"
    fun addModel(providerId: Long) = "add_model/$providerId"
}

@Composable
fun RelayAppContent(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Routes.chat(0L),
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                tween(350)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                tween(350)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                tween(350)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                tween(350)
            )
        }
    ) {
        composable(Routes.CHAT) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId")?.toLongOrNull() ?: 0L
            val viewModel: ChatViewModel = hiltViewModel()
            ChatScreen(
                conversationId = conversationId,
                viewModel = viewModel,
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToChat = { id ->
                    navController.navigate(Routes.chat(id)) {
                        popUpTo(Routes.CHAT_BASE) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.PROVIDERS) {
            val viewModel: ProvidersViewModel = hiltViewModel()
            ProvidersScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onAddProvider = { navController.navigate(Routes.ADD_PROVIDER) },
                onManageModels = { providerId ->
                    navController.navigate(Routes.models(providerId))
                }
            )
        }

        composable(Routes.ADD_PROVIDER) {
            val viewModel: ProvidersViewModel = hiltViewModel()
            AddProviderScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MODELS) { backStackEntry ->
            val providerId = backStackEntry.arguments?.getString("providerId")?.toLongOrNull() ?: 0L
            val viewModel: ModelsViewModel = hiltViewModel()
            ModelsScreen(
                providerId = providerId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onAddModel = { navController.navigate(Routes.addModel(providerId)) }
            )
        }

        composable(Routes.ADD_MODEL) { backStackEntry ->
            val providerId = backStackEntry.arguments?.getString("providerId")?.toLongOrNull() ?: 0L
            val viewModel: ModelsViewModel = hiltViewModel()
            AddModelScreen(
                providerId = providerId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToProviders = { navController.navigate(Routes.PROVIDERS) }
            )
        }
    }
}
