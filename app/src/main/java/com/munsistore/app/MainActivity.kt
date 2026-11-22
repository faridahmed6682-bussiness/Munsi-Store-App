package com.munsistore.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.munsistore.app.data.SessionManager
import com.munsistore.app.ui.AdminProductScreen
import com.munsistore.app.ui.AdminScreen
import com.munsistore.app.ui.AuthScreen
import com.munsistore.app.ui.CartScreen
import com.munsistore.app.ui.CheckoutScreen
import com.munsistore.app.ui.HomeScreen
import com.munsistore.app.ui.OrderDetailScreen
import com.munsistore.app.ui.OrdersScreen
import com.munsistore.app.ui.ProductDetailScreen
import com.munsistore.app.ui.ProfileScreen
import com.munsistore.app.ui.StoreViewModel
import com.munsistore.app.ui.WishlistScreen
import com.munsistore.app.ui.theme.MunsiStoreTheme

object Routes {
    const val HOME = "home"
    const val DETAIL = "detail/{productId}"
    const val CART = "cart"
    const val CHECKOUT = "checkout"
    const val ORDERS = "orders"
    const val ORDER_DETAIL = "orderDetail/{orderId}"
    const val WISHLIST = "wishlist"
    const val AUTH = "auth"
    const val PROFILE = "profile"
    const val ADMIN = "admin"
    const val ADMIN_PRODUCT = "adminProduct/{productId}"

    fun detail(productId: String) = "detail/$productId"
    fun orderDetail(orderId: String) = "orderDetail/$orderId"
    fun adminProduct(productId: String) = "adminProduct/$productId"
}

class MainActivity : ComponentActivity() {

    private val viewModel: StoreViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply the saved language (Bengali default).
        val lang = SessionManager(this).getLanguage()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))

        setContent {
            MunsiStoreTheme {
                MunsiStoreNav(viewModel)
            }
        }
    }
}

@Composable
fun MunsiStoreNav(viewModel: StoreViewModel) {
    val navController = rememberNavController()
    val cartCount by viewModel.cartCount.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomRoutes = listOf(Routes.HOME, Routes.WISHLIST, Routes.CART, Routes.ORDERS, Routes.PROFILE)
    val showBottomBar = currentRoute in bottomRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.HOME,
                        onClick = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text(stringResource(R.string.home)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.WISHLIST,
                        onClick = { navController.navigate(Routes.WISHLIST) { launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                        label = { Text(stringResource(R.string.wishlist)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.CART,
                        onClick = { navController.navigate(Routes.CART) { launchSingleTop = true } },
                        icon = {
                            BadgedBox(badge = {
                                if (cartCount > 0) Badge { Text(cartCount.toString()) }
                            }) {
                                Icon(Icons.Filled.ShoppingCart, contentDescription = null)
                            }
                        },
                        label = { Text(stringResource(R.string.cart)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.ORDERS,
                        onClick = { navController.navigate(Routes.ORDERS) { launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Receipt, contentDescription = null) },
                        label = { Text(stringResource(R.string.orders)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.PROFILE,
                        onClick = { navController.navigate(Routes.PROFILE) { launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        label = { Text(stringResource(R.string.profile)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(viewModel = viewModel, onProductClick = { id ->
                    navController.navigate(Routes.detail(id))
                })
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("productId") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("productId") ?: ""
                ProductDetailScreen(
                    viewModel = viewModel,
                    productId = id,
                    onBack = { navController.popBackStack() },
                    onGoToCart = { navController.navigate(Routes.CART) }
                )
            }
            composable(Routes.CART) {
                CartScreen(
                    viewModel = viewModel,
                    onCheckout = { navController.navigate(Routes.CHECKOUT) },
                    onProductClick = { id -> navController.navigate(Routes.detail(id)) }
                )
            }
            composable(Routes.CHECKOUT) {
                CheckoutScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOrderPlaced = { orderId ->
                        navController.navigate(Routes.orderDetail(orderId)) {
                            popUpTo(Routes.CART) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.ORDERS) {
                OrdersScreen(
                    viewModel = viewModel,
                    onOrderClick = { id -> navController.navigate(Routes.orderDetail(id)) },
                    onLoginClick = { navController.navigate(Routes.AUTH) }
                )
            }
            composable(
                route = Routes.ORDER_DETAIL,
                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("orderId") ?: ""
                OrderDetailScreen(
                    viewModel = viewModel,
                    orderId = id,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.WISHLIST) {
                WishlistScreen(
                    viewModel = viewModel,
                    onProductClick = { id -> navController.navigate(Routes.detail(id)) }
                )
            }
            composable(Routes.AUTH) {
                AuthScreen(
                    viewModel = viewModel,
                    onDone = { navController.popBackStack() }
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    viewModel = viewModel,
                    onLoginClick = { navController.navigate(Routes.AUTH) },
                    onAdminClick = { navController.navigate(Routes.ADMIN) },
                    onOrdersClick = { navController.navigate(Routes.ORDERS) }
                )
            }
            composable(Routes.ADMIN) {
                AdminScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onAddProduct = { navController.navigate(Routes.adminProduct("new")) },
                    onEditProduct = { id -> navController.navigate(Routes.adminProduct(id)) }
                )
            }
            composable(
                route = Routes.ADMIN_PRODUCT,
                arguments = listOf(navArgument("productId") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("productId") ?: "new"
                AdminProductScreen(
                    viewModel = viewModel,
                    productId = id,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
