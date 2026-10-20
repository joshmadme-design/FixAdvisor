package com.shahab.fixadvisor.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shahab.fixadvisor.FixAdvisorApp
import com.shahab.fixadvisor.ui.screens.HomeScreen
import com.shahab.fixadvisor.ui.screens.ProfileScreen
import com.shahab.fixadvisor.ui.screens.RegisterShopScreen
import com.shahab.fixadvisor.ui.screens.ShopDetailScreen
import com.shahab.fixadvisor.ui.screens.ShopQrScreen
import com.shahab.fixadvisor.ui.screens.WriteReviewScreen

object Routes {
    const val HOME = "home"
    const val REGISTER_SHOP = "register_shop"
    const val PROFILE = "profile"
    const val SHOP_DETAIL = "shop"
    const val WRITE_REVIEW = "write_review"
    const val SHOP_QR = "shop_qr"

    fun shopDetail(shopId: String): String = "$SHOP_DETAIL/$shopId"
    fun shopQr(shopId: String): String = "$SHOP_QR/$shopId"

    /** [code] lets a scanned counter-QR deep link prefill the verified code. */
    fun writeReview(shopId: String, code: String? = null): String =
        if (code.isNullOrBlank()) {
            "$WRITE_REVIEW/$shopId"
        } else {
            "$WRITE_REVIEW/$shopId?code=${Uri.encode(code)}"
        }
}

@Composable
fun AppNavGraph() {
    val app = LocalContext.current.applicationContext as FixAdvisorApp
    val navController = rememberNavController()

    fun goTab(route: String) {
        navController.navigate(route) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                app = app,
                onOpenShop = { id -> navController.navigate(Routes.shopDetail(id)) },
                onRegisterShop = { goTab(Routes.REGISTER_SHOP) },
                onOpenProfile = { goTab(Routes.PROFILE) }
            )
        }
        composable(Routes.REGISTER_SHOP) {
            RegisterShopScreen(
                app = app,
                onRegistered = { id ->
                    navController.navigate(Routes.shopDetail(id)) {
                        popUpTo(Routes.HOME)
                    }
                },
                onHome = { goTab(Routes.HOME) },
                onOpenProfile = { goTab(Routes.PROFILE) }
            )
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                app = app,
                onHome = { goTab(Routes.HOME) },
                onRegisterShop = { goTab(Routes.REGISTER_SHOP) },
                onOpenShop = { id -> navController.navigate(Routes.shopDetail(id)) },
                onOpenQr = { id -> navController.navigate(Routes.shopQr(id)) }
            )
        }
        composable(
            route = "${Routes.SHOP_DETAIL}/{shopId}",
            arguments = listOf(navArgument("shopId") { type = NavType.StringType })
        ) { backStackEntry ->
            val shopId = backStackEntry.arguments?.getString("shopId") ?: return@composable
            ShopDetailScreen(
                app = app,
                shopId = shopId,
                onBack = { navController.popBackStack() },
                onWriteReview = { id -> navController.navigate(Routes.writeReview(id)) },
                onOpenQr = { id -> navController.navigate(Routes.shopQr(id)) }
            )
        }
        composable(
            route = "${Routes.WRITE_REVIEW}/{shopId}?code={code}",
            arguments = listOf(
                navArgument("shopId") { type = NavType.StringType },
                navArgument("code") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val shopId = backStackEntry.arguments?.getString("shopId") ?: return@composable
            val code = backStackEntry.arguments?.getString("code")?.takeIf { it.isNotBlank() }
            WriteReviewScreen(
                app = app,
                shopId = shopId,
                code = code,
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "${Routes.SHOP_QR}/{shopId}",
            arguments = listOf(navArgument("shopId") { type = NavType.StringType })
        ) { backStackEntry ->
            val shopId = backStackEntry.arguments?.getString("shopId") ?: return@composable
            ShopQrScreen(
                app = app,
                shopId = shopId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
