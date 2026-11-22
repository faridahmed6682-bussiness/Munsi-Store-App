package com.munsistore.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.munsistore.app.MunsiStoreApp
import com.munsistore.app.data.CartWithProduct
import com.munsistore.app.data.Categories
import com.munsistore.app.data.FirebaseManager
import com.munsistore.app.data.Order
import com.munsistore.app.data.OrderItem
import com.munsistore.app.data.OrderStatus
import com.munsistore.app.data.Product
import com.munsistore.app.data.SessionManager
import com.munsistore.app.data.StoreRepository
import com.munsistore.app.data.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StoreViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as MunsiStoreApp).database
    private val repo = StoreRepository(db)
    private val session = SessionManager(application)

    val isFirebaseConfigured: Boolean = FirebaseManager.isConfigured

    var language by mutableStateOf(session.getLanguage())
        private set

    // ---- Search / category ----
    var searchQuery by mutableStateOf("")
        private set
    var selectedCategory by mutableStateOf<String?>(null)
        private set

    // ---- Session ----
    private val userEmailFlow = MutableStateFlow(session.getUserEmail())
    var currentUserEmail by mutableStateOf(session.getUserEmail())
        private set
    var isAdmin by mutableStateOf(session.isAdmin())
        private set
    var isLoggedIn by mutableStateOf(session.isLoggedIn())
        private set
    var authMessage by mutableStateOf<String?>(null)
        private set

    // ---- Catalog ----
    val products: StateFlow<List<Product>> = repo.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Products after applying the current search + category, computed in the UI from [products]. */
    fun applyFilter(list: List<Product>): List<Product> {
        var out = list
        selectedCategory?.let { cat -> out = out.filter { it.category == cat } }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim()
            out = out.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.nameBn.contains(q) ||
                    it.description.contains(q, ignoreCase = true)
            }
        }
        return out
    }

    fun setSearchQuery(q: String) { searchQuery = q }
    fun setCategory(cat: String?) { selectedCategory = cat }

    // ---- Cart / wishlist ----
    val cartItems = repo.getCartItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartWithProducts: StateFlow<List<CartWithProduct>> =
        combine(repo.getCartItems(), repo.getAllProducts()) { items, prods ->
            items.mapNotNull { ci ->
                prods.find { it.id == ci.productId }?.let { CartWithProduct(it, ci.quantity) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartCount: StateFlow<Int> =
        repo.getCartItems()
            .map { items -> items.sumOf { it.quantity } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val wishlistIds: StateFlow<Set<String>> =
        repo.getWishlistItems()
            .map { list -> list.map { it.productId }.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val wishlistProducts: StateFlow<List<Product>> =
        combine(repo.getWishlistItems(), repo.getAllProducts()) { items, prods ->
            items.mapNotNull { w -> prods.find { it.id == w.productId } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addToCart(productId: String, quantity: Int = 1) {
        viewModelScope.launch { repo.addToCart(productId, quantity) }
    }

    fun setCartQuantity(productId: String, quantity: Int) {
        viewModelScope.launch { repo.setCartQuantity(productId, quantity) }
    }

    fun removeFromCart(productId: String) {
        viewModelScope.launch { repo.removeFromCart(productId) }
    }

    fun toggleWishlist(productId: String) {
        viewModelScope.launch { repo.toggleWishlist(productId) }
    }

    // ---- Orders ----
    val myOrders: StateFlow<List<Order>> =
        userEmailFlow.flatMapLatest { email ->
            if (email == null) flowOf(emptyList()) else repo.getOrdersByUser(email)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOrders: StateFlow<List<Order>> = repo.getAllOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSales: StateFlow<Double> = repo.totalSales()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val pendingCount: StateFlow<Int> = repo.pendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    var lastPlacedOrderId by mutableStateOf<String?>(null)
        private set

    fun clearLastOrder() { lastPlacedOrderId = null }

    suspend fun getProduct(id: String): Product? = repo.getProduct(id)
    suspend fun getOrder(id: String): Order? = repo.getOrderById(id)
    suspend fun getOrderItems(id: String): List<OrderItem> = repo.getOrderItems(id)

    fun placeOrder(
        name: String,
        phone: String,
        address: String,
        paymentMethod: String,
        onDone: (String?) -> Unit
    ) {
        val email = currentUserEmail ?: "guest@munsi.store"
        val cart = cartWithProducts.value
        if (cart.isEmpty()) {
            onDone(null)
            return
        }
        viewModelScope.launch {
            val id = repo.placeOrder(email, name, phone, address, paymentMethod, cart)
            lastPlacedOrderId = id
            onDone(id)
        }
    }

    fun updateOrderStatus(orderId: String, status: String) {
        viewModelScope.launch { repo.updateOrderStatus(orderId, status) }
    }

    // ---- Admin: products ----
    fun saveProduct(product: Product) {
        viewModelScope.launch { repo.saveProduct(product) }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch { repo.deleteProduct(product) }
    }

    // ---- Auth ----
    fun login(email: String, password: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val cleanEmail = email.trim().lowercase()
            // Firebase Auth first when configured, local Room as fallback/offline.
            val firebaseAuth = FirebaseManager.auth()
            if (firebaseAuth != null) {
                try {
                    val result = kotlinx.coroutines.tasks.await(
                        firebaseAuth.signInWithEmailAndPassword(cleanEmail, password)
                    )
                    val fbUser = result.user
                    if (fbUser != null) {
                        val local = repo.getLocalUser(cleanEmail)
                        val admin = local?.isAdmin == true || cleanEmail == "admin@munsi.store"
                        applyLogin(cleanEmail, admin)
                        onDone(true, null)
                        return@launch
                    }
                } catch (e: Exception) {
                    // Fall through to local login (offline or not registered in Firebase).
                }
            }
            val user = repo.checkLocalLogin(cleanEmail, password)
            if (user != null) {
                applyLogin(user.email, user.isAdmin)
                onDone(true, null)
            } else {
                onDone(false, "invalid")
            }
        }
    }

    fun register(name: String, email: String, password: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val cleanEmail = email.trim().lowercase()
            val firebaseAuth = FirebaseManager.auth()
            if (firebaseAuth != null) {
                try {
                    kotlinx.coroutines.tasks.await(
                        firebaseAuth.createUserWithEmailAndPassword(cleanEmail, password)
                    )
                } catch (e: Exception) {
                    // Fall through to local registration.
                }
            }
            val ok = repo.registerLocalUser(name, cleanEmail, password)
            if (ok) {
                applyLogin(cleanEmail, false)
                onDone(true, null)
            } else {
                onDone(false, "exists")
            }
        }
    }

    /** Called after a successful Google sign-in with the account email. */
    fun loginWithGoogle(email: String, name: String) {
        viewModelScope.launch {
            val cleanEmail = email.trim().lowercase()
            val existing = repo.getLocalUser(cleanEmail)
            if (existing == null) {
                repo.registerLocalUser(name, cleanEmail, java.util.UUID.randomUUID().toString())
            }
            val user = repo.getLocalUser(cleanEmail)
            applyLogin(cleanEmail, user?.isAdmin == true)
        }
    }

    private fun applyLogin(email: String, admin: Boolean) {
        session.saveLogin(email, admin)
        currentUserEmail = email
        isAdmin = admin
        isLoggedIn = true
        userEmailFlow.value = email
    }

    fun logout() {
        session.logout()
        currentUserEmail = null
        isAdmin = false
        isLoggedIn = false
        userEmailFlow.value = null
    }

    fun setLanguage(lang: String) {
        session.saveLanguage(lang)
        language = lang
    }

    fun categoryName(cat: String): String {
        return when (cat) {
            Categories.RAZOR -> "Razor / রেজর"
            Categories.ORAL_CARE -> "Oral Care / ওরাল কেয়ার"
            Categories.BATH -> "Bath / গোসল"
            Categories.GROCERY -> "Grocery / মুদি"
            Categories.COSMETICS -> "Cosmetics / কসমেটিকস"
            Categories.HANDMADE -> "Handmade / হস্তশিল্প"
            else -> cat
        }
    }
}
