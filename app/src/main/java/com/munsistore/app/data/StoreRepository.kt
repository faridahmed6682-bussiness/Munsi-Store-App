package com.munsistore.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

/** Details collected at checkout (Daraz-style). */
data class CheckoutInput(
    val name: String,
    val phone: String,
    val address: Address,
    val paymentMethod: String,
    val trxId: String?,
    val voucher: Voucher?,
    val coinsUsed: Int
)

/** Single source of truth for the UI. Local Room first; Firebase sync is best-effort. */
class StoreRepository(private val db: AppDatabase) {

    fun getAllProducts(): Flow<List<Product>> = db.productDao().getAll()
    fun getProductsByCategory(category: String): Flow<List<Product>> = db.productDao().getByCategory(category)
    fun getFlashSaleProducts(): Flow<List<Product>> = db.productDao().getFlashSale()
    fun getBrands(): Flow<List<String>> = db.productDao().getBrands()
    fun searchProducts(query: String): Flow<List<Product>> = db.productDao().search(query)
    suspend fun getProduct(id: String): Product? = db.productDao().getById(id)
    suspend fun getFlashSale(): FlashSale? = db.flashSaleDao().get()

    fun getCartItems(): Flow<List<CartItem>> = db.cartDao().getAll()
    fun getWishlistItems(): Flow<List<WishlistItem>> = db.wishlistDao().getAll()
    fun getOrdersByUser(email: String): Flow<List<Order>> = db.orderDao().getByUser(email)
    fun getAllOrders(): Flow<List<Order>> = db.orderDao().getAll()
    fun getSellerOrders(email: String): Flow<List<Order>> = db.orderDao().getOrdersForSeller(email)
    fun getSellerProducts(email: String): Flow<List<Product>> = db.productDao().getBySeller(email)
    fun totalSales(): Flow<Double> = db.orderDao().totalSales()
    fun pendingCount(): Flow<Int> = db.orderDao().pendingCount()
    suspend fun getOrderItems(orderId: String): List<OrderItem> = db.orderItemDao().getByOrderId(orderId)
    suspend fun getOrderById(orderId: String): Order? = db.orderDao().getById(orderId)

    suspend fun addToCart(productId: String, quantity: Int = 1) {
        val current = db.cartDao().getAll().first().find { it.productId == productId }
        val newQty = (current?.quantity ?: 0) + quantity
        db.cartDao().insert(CartItem(productId, newQty))
    }

    suspend fun setCartQuantity(productId: String, quantity: Int) {
        if (quantity <= 0) {
            db.cartDao().delete(productId)
        } else {
            db.cartDao().insert(CartItem(productId, quantity))
        }
    }

    suspend fun removeFromCart(productId: String) = db.cartDao().delete(productId)
    suspend fun clearCart() = db.cartDao().clear()

    suspend fun toggleWishlist(productId: String) {
        val current = db.wishlistDao().getAll().first().any { it.productId == productId }
        if (current) db.wishlistDao().delete(productId) else db.wishlistDao().insert(WishlistItem(productId))
    }

    // ---- Checkout / orders ----
    /**
     * Creates the order + items, applies voucher + coins + zone delivery fee,
     * bumps sold counts, clears the cart, syncs to Firestore if configured.
     */
    suspend fun placeOrder(
        userEmail: String,
        input: CheckoutInput,
        cart: List<CartWithProduct>,
        flashActive: Boolean
    ): String {
        val subtotal = cart.sumOf { effectivePrice(it.product, flashActive) * it.quantity }
        val zoneFee = DeliveryZone.fee(input.address.zone)
        val deliveryFee = if (subtotal >= ShopConfig.FREE_DELIVERY_THRESHOLD) 0.0 else zoneFee
        val voucherDiscount = input.voucher?.let { discountFor(it, subtotal, deliveryFee) } ?: 0.0
        val coinsValue = input.coinsUsed * ShopConfig.COIN_VALUE_TAKA
        val total = (subtotal - voucherDiscount - coinsValue + deliveryFee).coerceAtLeast(0.0)
        val orderId = "MS-" + UUID.randomUUID().toString().take(8).uppercase()
        val order = Order(
            id = orderId,
            customerName = input.name,
            customerPhone = input.phone,
            customerAddress = input.address.fullAddress,
            deliveryZone = input.address.zone,
            paymentMethod = input.paymentMethod,
            trxId = input.trxId?.trim()?.ifBlank { null },
            voucherCode = input.voucher?.code,
            discountAmount = voucherDiscount,
            coinsUsed = input.coinsUsed,
            deliveryFee = deliveryFee,
            subtotal = subtotal,
            totalAmount = total,
            status = OrderStatus.PLACED,
            createdAt = System.currentTimeMillis(),
            userEmail = userEmail
        )
        val items = cart.map {
            OrderItem(
                orderId = orderId,
                productId = it.product.id,
                productName = it.product.name,
                price = effectivePrice(it.product, flashActive),
                quantity = it.quantity
            )
        }
        db.orderDao().insert(order)
        db.orderItemDao().insertAll(items)
        cart.forEach { db.productDao().addSold(it.product.id, it.quantity) }
        if (input.coinsUsed > 0) db.userDao().addCoins(userEmail, -input.coinsUsed)
        db.cartDao().clear()
        FirebaseManager.syncOrder(order, items)
        return orderId
    }

    /**
     * Moves an order to a new status. Assigns a courier when shipped.
     * Awards coins when an order is delivered for the first time, plus the
     * referral bonus to the referrer on the referred user's first delivery.
     */
    suspend fun updateOrderStatus(orderId: String, status: String) {
        val order = db.orderDao().getById(orderId) ?: return
        val wasDelivered = order.status == OrderStatus.DELIVERED
        if (status == OrderStatus.SHIPPED) {
            db.orderDao().updateStatusWithCourier(orderId, status, ShopConfig.courierFor(orderId))
        } else {
            db.orderDao().updateStatus(orderId, status)
        }
        FirebaseManager.syncOrderStatus(orderId, status)
        if (status == OrderStatus.DELIVERED && !wasDelivered) {
            val earned = (order.totalAmount / ShopConfig.COINS_EARN_DIVISOR).toInt()
            if (earned > 0) db.userDao().addCoins(order.userEmail, earned)
            val user = db.userDao().getByEmail(order.userEmail)
            if (user != null && user.referredBy.isNotBlank()) {
                val deliveredSoFar = db.orderDao().deliveredCountForUser(order.userEmail)
                if (deliveredSoFar <= 1) {
                    val referrer = db.userDao().getByReferralCode(user.referredBy)
                    if (referrer != null) {
                        db.userDao().addCoins(referrer.email, ShopConfig.REFERRAL_DELIVERY_BONUS)
                    }
                }
            }
        }
    }

    // ---- Reviews & Q&A ----
    fun getReviews(productId: String): Flow<List<Review>> = db.reviewDao().getByProduct(productId)

    /** Live average rating: review average when reviews exist, else the seeded rating. */
    suspend fun averageRating(productId: String, fallback: Float): Double {
        return db.reviewDao().averageFor(productId) ?: fallback.toDouble()
    }

    suspend fun addReview(review: Review) = db.reviewDao().insert(review)

    fun getQuestions(productId: String): Flow<List<ProductQuestion>> =
        db.questionDao().getByProduct(productId)

    suspend fun askQuestion(question: ProductQuestion) = db.questionDao().insert(question)
    suspend fun answerQuestion(id: String, answer: String) = db.questionDao().answer(id, answer)

    // ---- Vouchers ----
    fun getVouchers(): Flow<List<Voucher>> = db.voucherDao().getAll()
    suspend fun getVoucher(code: String): Voucher? = db.voucherDao().getByCode(code.trim().uppercase())
    suspend fun saveVoucher(voucher: Voucher) = db.voucherDao().insert(voucher)
    suspend fun setVoucherActive(code: String, active: Boolean) = db.voucherDao().setActive(code, active)
    suspend fun deleteVoucher(voucher: Voucher) = db.voucherDao().delete(voucher)

    // ---- Address book ----
    fun getAddresses(email: String): Flow<List<Address>> = db.addressDao().getByUser(email)
    suspend fun getDefaultAddress(email: String): Address? = db.addressDao().getDefault(email)

    suspend fun saveAddress(address: Address) {
        if (address.isDefault) db.addressDao().clearDefaults(address.userEmail)
        db.addressDao().insert(address)
    }

    suspend fun deleteAddress(address: Address) = db.addressDao().delete(address)

    // ---- Recently viewed / trending ----
    suspend fun recordView(productId: String) {
        db.recentViewDao().insert(RecentView(productId, System.currentTimeMillis()))
    }

    fun getRecentViews(limit: Int = 10): Flow<List<RecentView>> = db.recentViewDao().getRecent(limit)

    suspend fun recordSearch(rawTerm: String) {
        val term = rawTerm.trim().lowercase()
        if (term.length < 2) return
        val existing = db.searchTermDao().get(term)
        val now = System.currentTimeMillis()
        if (existing == null) {
            db.searchTermDao().insert(SearchTerm(term, 1, now))
        } else {
            db.searchTermDao().insert(existing.copy(count = existing.count + 1, lastSearchedAt = now))
        }
    }

    fun getTrending(limit: Int = 8): Flow<List<SearchTerm>> = db.searchTermDao().getTrending(limit)

    // ---- Returns / refunds ----
    suspend fun requestReturn(orderId: String, email: String, reason: String): Boolean {
        if (db.returnDao().getByOrder(orderId) != null) return false
        db.returnDao().insert(
            ReturnRequest(
                id = "RT-" + UUID.randomUUID().toString().take(8).uppercase(),
                orderId = orderId,
                userEmail = email,
                reason = reason.trim(),
                status = ReturnStatus.REQUESTED,
                createdAt = System.currentTimeMillis()
            )
        )
        return true
    }

    fun getReturnsByUser(email: String): Flow<List<ReturnRequest>> = db.returnDao().getByUser(email)
    fun getAllReturns(): Flow<List<ReturnRequest>> = db.returnDao().getAll()
    suspend fun getReturnForOrder(orderId: String): ReturnRequest? = db.returnDao().getByOrder(orderId)
    suspend fun updateReturnStatus(id: String, status: String) = db.returnDao().updateStatus(id, status)

    // ---- Coins / referral / seller ----
    suspend fun getUser(email: String): User? = db.userDao().getByEmail(email)
    suspend fun getCoins(email: String): Int = db.userDao().getByEmail(email)?.coins ?: 0

    suspend fun registerSeller(email: String, shopName: String) {
        db.userDao().registerSeller(email, shopName.trim())
    }

    suspend fun applyReferralSignup(newEmail: String, code: String): Boolean {
        val clean = code.trim().uppercase()
        if (clean.isBlank()) return false
        val referrer = db.userDao().getByReferralCode(clean) ?: return false
        if (referrer.email == newEmail) return false
        val user = db.userDao().getByEmail(newEmail) ?: return false
        db.userDao().update(user.copy(referredBy = clean))
        db.userDao().addCoins(newEmail, ShopConfig.REFERRAL_SIGNUP_BONUS)
        return true
    }

    // ---- Admin: products ----
    suspend fun saveProduct(product: Product) {
        db.productDao().insert(product)
        FirebaseManager.syncProduct(product)
    }

    suspend fun deleteProduct(product: Product) {
        db.productDao().delete(product)
        FirebaseManager.deleteProductRemote(product.id)
    }

    // ---- Local accounts (demo/offline). Firebase Auth is used when configured. ----
    suspend fun getLocalUser(email: String): User? = db.userDao().getByEmail(email)

    suspend fun registerLocalUser(name: String, email: String, password: String): Boolean {
        if (db.userDao().getByEmail(email) != null) return false
        db.userDao().insert(
            User(
                email, name, sha256(password), false,
                referralCode = ShopConfig.referralCodeFor(email)
            )
        )
        return true
    }

    suspend fun checkLocalLogin(email: String, password: String): User? {
        val user = db.userDao().getByEmail(email) ?: return null
        return if (user.passwordHash == sha256(password)) user else null
    }
}
