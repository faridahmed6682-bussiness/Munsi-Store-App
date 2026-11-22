package com.munsistore.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.munsistore.app.MunsiStoreApp
import com.munsistore.app.data.Address
import com.munsistore.app.data.CartWithProduct
import com.munsistore.app.data.Categories
import com.munsistore.app.data.CheckoutInput
import com.munsistore.app.data.DeliveryZone
import com.munsistore.app.data.FirebaseManager
import com.munsistore.app.data.FlashSale
import com.munsistore.app.data.NotificationHelper
import com.munsistore.app.data.Order
import com.munsistore.app.data.OrderItem
import com.munsistore.app.data.OrderStatus
import com.munsistore.app.data.Product
import com.munsistore.app.data.ProductQuestion
import com.munsistore.app.data.ReturnRequest
import com.munsistore.app.data.Review
import com.munsistore.app.data.SearchTerm
import com.munsistore.app.data.SessionManager
import com.munsistore.app.data.ShopConfig
import com.munsistore.app.data.SortBy
import com.munsistore.app.data.StoreRepository
import com.munsistore.app.data.User
import com.munsistore.app.data.Voucher
import com.munsistore.app.data.discountFor
import com.munsistore.app.data.effectivePrice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class StoreViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val db = (application as MunsiStoreApp).database
    private val repo = StoreRepository(db)
    private val session = SessionManager(application)

    val isFirebaseConfigured: Boolean = FirebaseManager.isConfigured

    var language by mutableStateOf(session.getLanguage())
        private set

    // ---- Search / category / filters / sort ----
    var searchQuery by mutableStateOf("")
        private set
    var selectedCategory by mutableStateOf<String?>(null)
        private set
    var priceMin by mutableStateOf<Double?>(null)
        private set
    var priceMax by mutableStateOf<Double?>(null)
        private set
    var brandFilter by mutableStateOf<String?>(null)
        private set
    var minRating by mutableStateOf(0f)
        private set
    var sortBy by mutableStateOf(SortBy.NEWEST)
        private set

    fun updateSearchQuery(q: String) { searchQuery = q }
    fun setCategory(cat: String?) { selectedCategory = cat }
    fun setPriceRange(min: Double?, max: Double?) { priceMin = min; priceMax = max }
    fun updateBrandFilter(brand: String?) { brandFilter = brand }
    fun updateMinRating(rating: Float) { minRating = rating }
    fun updateSortBy(sort: String) { sortBy = sort }
    fun clearFilters() {
        priceMin = null; priceMax = null; brandFilter = null; minRating = 0f
        selectedCategory = null
    }

    val brands: StateFlow<List<String>> = repo.getBrands()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Flash sale ends-at, loaded once at startup. */
    var flashSale by mutableStateOf<FlashSale?>(null)
        private set

    fun isFlashActive(): Boolean {
        val sale = flashSale ?: return false
        return sale.endsAt > System.currentTimeMillis()
    }

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

    // ---- Profile extras (coins / referral / seller) ----
    var myCoins by mutableStateOf(0)
        private set
    var myReferralCode by mutableStateOf("")
        private set
    var isSeller by mutableStateOf(false)
        private set
    var myShopName by mutableStateOf("")
        private set
    var notificationsEnabled by mutableStateOf(session.notificationsEnabled())
        private set

    fun updateNotificationsEnabled(enabled: Boolean) {
        session.updateNotificationsEnabled(enabled)
        notificationsEnabled = enabled
    }

    private fun refreshProfile() {
        val email = currentUserEmail ?: return
        viewModelScope.launch {
            val user = repo.getUser(email)
            myCoins = user?.coins ?: 0
            myReferralCode = user?.referralCode ?: ShopConfig.referralCodeFor(email)
            isSeller = user?.isSeller == true
            myShopName = user?.shopName ?: ""
        }
    }

    fun refreshCoins() = refreshProfile()

    // ---- Catalog ----
    val products: StateFlow<List<Product>> = repo.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flashProducts: StateFlow<List<Product>> = repo.getFlashSaleProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Products after search + category + filters + sort (uses seeded rating for the filter). */
    fun applyFilter(list: List<Product>): List<Product> {
        val flashOn = isFlashActive()
        var out = list
        selectedCategory?.let { cat -> out = out.filter { it.category == cat } }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim()
            out = out.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.nameBn.contains(q) ||
                    it.description.contains(q, ignoreCase = true) ||
                    it.brand.contains(q, ignoreCase = true)
            }
        }
        priceMin?.let { min -> out = out.filter { effectivePrice(it, flashOn) >= min } }
        priceMax?.let { max -> out = out.filter { effectivePrice(it, flashOn) <= max } }
        brandFilter?.let { b -> out = out.filter { it.brand == b } }
        if (minRating > 0f) out = out.filter { it.rating >= minRating }
        out = when (sortBy) {
            SortBy.PRICE_LOW -> out.sortedBy { effectivePrice(it, flashOn) }
            SortBy.PRICE_HIGH -> out.sortedByDescending { effectivePrice(it, flashOn) }
            SortBy.RATING -> out.sortedByDescending { it.rating }
            SortBy.POPULAR -> out.sortedByDescending { it.soldCount }
            else -> out.sortedByDescending { it.createdAt } // NEWEST
        }
        return out
    }

    fun setSearchQueryAndRecord(q: String) { searchQuery = q }

    // ---- Recently viewed / recommended / trending ----
    val recentProducts: StateFlow<List<Product>> =
        combine(repo.getRecentViews(10), repo.getAllProducts()) { views, prods ->
            views.mapNotNull { v -> prods.find { it.id == v.productId } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recommendedProducts: StateFlow<List<Product>> =
        combine(repo.getRecentViews(10), repo.getWishlistItems(), repo.getAllProducts()) { views, wishes, prods ->
            val viewedIds = views.map { it.productId }.toSet()
            val likedCats = (views.mapNotNull { v -> prods.find { it.id == v.productId }?.category } +
                wishes.mapNotNull { w -> prods.find { it.id == w.productId }?.category }).toSet()
            val pool = if (likedCats.isEmpty()) {
                prods.filter { it.isFeatured }
            } else {
                prods.filter { it.category in likedCats && it.id !in viewedIds }
            }
            pool.sortedByDescending { it.soldCount }.take(10)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trendingTerms: StateFlow<List<SearchTerm>> = repo.getTrending(8)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun recordView(productId: String) {
        viewModelScope.launch { repo.recordView(productId) }
    }

    fun recordSearch(term: String) {
        viewModelScope.launch { repo.recordSearch(term) }
    }

    // ---- Reviews & Q&A ----
    fun reviewsOf(productId: String): Flow<List<Review>> = repo.getReviews(productId)
    fun questionsOf(productId: String): Flow<List<ProductQuestion>> = repo.getQuestions(productId)

    suspend fun averageRating(productId: String, fallback: Float): Double =
        repo.averageRating(productId, fallback)

    fun addReview(productId: String, rating: Int, text: String, userName: String) {
        val email = currentUserEmail ?: return
        viewModelScope.launch {
            repo.addReview(
                Review(
                    id = "$productId|$email",
                    productId = productId,
                    userEmail = email,
                    userName = userName.ifBlank { email.substringBefore("@") },
                    rating = rating.coerceIn(1, 5),
                    text = text.trim(),
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun askQuestion(productId: String, question: String) {
        val email = currentUserEmail ?: return
        viewModelScope.launch {
            repo.askQuestion(
                ProductQuestion(
                    id = "q-" + UUID.randomUUID().toString().take(8),
                    productId = productId,
                    userEmail = email,
                    question = question.trim(),
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun answerQuestion(id: String, answer: String) {
        viewModelScope.launch { repo.answerQuestion(id, answer.trim()) }
    }

    // ---- Vouchers ----
    val vouchers: StateFlow<List<Voucher>> = repo.getVouchers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveVoucher(voucher: Voucher) {
        viewModelScope.launch { repo.saveVoucher(voucher) }
    }

    fun setVoucherActive(code: String, active: Boolean) {
        viewModelScope.launch { repo.setVoucherActive(code, active) }
    }

    fun deleteVoucher(voucher: Voucher) {
        viewModelScope.launch { repo.deleteVoucher(voucher) }
    }

    suspend fun validateVoucher(code: String, subtotal: Double, deliveryFee: Double): Pair<Voucher?, Double> {
        val voucher = repo.getVoucher(code) ?: return null to 0.0
        if (!com.munsistore.app.data.isVoucherUsable(voucher, subtotal)) return null to 0.0
        return voucher to discountFor(voucher, subtotal, deliveryFee)
    }

    // ---- Address book ----
    val myAddresses: StateFlow<List<Address>> =
        userEmailFlow.flatMapLatest { email ->
            if (email == null) flowOf(emptyList()) else repo.getAddresses(email)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveAddress(address: Address) {
        viewModelScope.launch { repo.saveAddress(address) }
    }

    fun deleteAddress(address: Address) {
        viewModelScope.launch { repo.deleteAddress(address) }
    }

    suspend fun defaultAddress(): Address? {
        val email = currentUserEmail ?: return null
        return repo.getDefaultAddress(email)
    }

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

    val sellerOrders: StateFlow<List<Order>> =
        userEmailFlow.flatMapLatest { email ->
            if (email == null) flowOf(emptyList()) else repo.getSellerOrders(email)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sellerProducts: StateFlow<List<Product>> =
        userEmailFlow.flatMapLatest { email ->
            if (email == null) flowOf(emptyList()) else repo.getSellerProducts(email)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    suspend fun getReturnForOrder(orderId: String): ReturnRequest? = repo.getReturnForOrder(orderId)

    fun placeOrder(input: CheckoutInput, onDone: (String?) -> Unit) {
        val email = currentUserEmail ?: "guest@munsi.store"
        val cart = cartWithProducts.value
        if (cart.isEmpty()) {
            onDone(null)
            return
        }
        viewModelScope.launch {
            val id = repo.placeOrder(email, input, cart, isFlashActive())
            lastPlacedOrderId = id
            refreshProfile()
            onDone(id)
        }
    }

    fun updateOrderStatus(orderId: String, status: String) {
        viewModelScope.launch {
            repo.updateOrderStatus(orderId, status)
            refreshProfile()
            if (session.notificationsEnabled()) {
                NotificationHelper.notifyOrderStatus(
                    appContext,
                    orderId,
                    statusLabel(status)
                )
            }
        }
    }

    // ---- Returns / refunds ----
    val myReturns: StateFlow<List<ReturnRequest>> =
        userEmailFlow.flatMapLatest { email ->
            if (email == null) flowOf(emptyList()) else repo.getReturnsByUser(email)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReturns: StateFlow<List<ReturnRequest>> = repo.getAllReturns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun requestReturn(orderId: String, reason: String, onDone: (Boolean) -> Unit) {
        val email = currentUserEmail ?: return
        viewModelScope.launch {
            onDone(repo.requestReturn(orderId, email, reason))
        }
    }

    fun updateReturnStatus(id: String, status: String) {
        viewModelScope.launch { repo.updateReturnStatus(id, status) }
    }

    // ---- Seller ----
    fun registerSeller(shopName: String, onDone: (Boolean) -> Unit) {
        val email = currentUserEmail ?: return
        viewModelScope.launch {
            repo.registerSeller(email, shopName)
            refreshProfile()
            onDone(true)
        }
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
                    val result = firebaseAuth.signInWithEmailAndPassword(cleanEmail, password).await()
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

    fun register(
        name: String,
        email: String,
        password: String,
        referralCode: String = "",
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val cleanEmail = email.trim().lowercase()
            val firebaseAuth = FirebaseManager.auth()
            if (firebaseAuth != null) {
                try {
                    firebaseAuth.createUserWithEmailAndPassword(cleanEmail, password).await()
                } catch (e: Exception) {
                    // Fall through to local registration.
                }
            }
            val ok = repo.registerLocalUser(name, cleanEmail, password)
            if (ok) {
                if (referralCode.isNotBlank()) {
                    repo.applyReferralSignup(cleanEmail, referralCode)
                }
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
            } else if (existing.referralCode.isBlank()) {
                repo.getUser(cleanEmail)?.let { user ->
                    db.userDao().update(user.copy(referralCode = ShopConfig.referralCodeFor(cleanEmail)))
                }
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
        refreshProfile()
    }

    fun logout() {
        session.logout()
        currentUserEmail = null
        isAdmin = false
        isLoggedIn = false
        myCoins = 0
        myReferralCode = ""
        isSeller = false
        myShopName = ""
        userEmailFlow.value = null
    }

    fun updateLanguage(lang: String) {
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

    fun zoneName(zone: String): String {
        return when (zone) {
            DeliveryZone.DHAKA_CITY -> "Dhaka City"
            DeliveryZone.INSIDE_DHAKA -> "Inside Dhaka"
            DeliveryZone.OUTSIDE_DHAKA -> "Outside Dhaka"
            else -> zone
        }
    }

    init {
        viewModelScope.launch {
            flashSale = repo.getFlashSale()
        }
        if (isLoggedIn) refreshProfile()
    }
}
