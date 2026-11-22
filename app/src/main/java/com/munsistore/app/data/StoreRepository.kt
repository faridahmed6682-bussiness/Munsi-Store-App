package com.munsistore.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

/** Single source of truth for the UI. Local Room first; Firebase sync is best-effort. */
class StoreRepository(private val db: AppDatabase) {

    fun getAllProducts(): Flow<List<Product>> = db.productDao().getAll()
    fun getProductsByCategory(category: String): Flow<List<Product>> = db.productDao().getByCategory(category)
    fun searchProducts(query: String): Flow<List<Product>> = db.productDao().search(query)
    suspend fun getProduct(id: String): Product? = db.productDao().getById(id)

    fun getCartItems(): Flow<List<CartItem>> = db.cartDao().getAll()
    fun getWishlistItems(): Flow<List<WishlistItem>> = db.wishlistDao().getAll()
    fun getOrdersByUser(email: String): Flow<List<Order>> = db.orderDao().getByUser(email)
    fun getAllOrders(): Flow<List<Order>> = db.orderDao().getAll()
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

    /** Creates the order + items, clears the cart, syncs to Firestore if configured. */
    suspend fun placeOrder(
        userEmail: String,
        name: String,
        phone: String,
        address: String,
        paymentMethod: String,
        cart: List<CartWithProduct>
    ): String {
        val total = cart.sumOf { it.product.price * it.quantity }
        val deliveryFee = if (total >= 500) 0.0 else 60.0
        val orderId = "MS-" + UUID.randomUUID().toString().take(8).uppercase()
        val order = Order(
            id = orderId,
            customerName = name,
            customerPhone = phone,
            customerAddress = address,
            paymentMethod = paymentMethod,
            totalAmount = total + deliveryFee,
            status = OrderStatus.PLACED,
            createdAt = System.currentTimeMillis(),
            userEmail = userEmail
        )
        val items = cart.map {
            OrderItem(
                orderId = orderId,
                productId = it.product.id,
                productName = it.product.name,
                price = it.product.price,
                quantity = it.quantity
            )
        }
        db.orderDao().insert(order)
        db.orderItemDao().insertAll(items)
        db.cartDao().clear()
        FirebaseManager.syncOrder(order, items)
        return orderId
    }

    suspend fun updateOrderStatus(orderId: String, status: String) {
        db.orderDao().updateStatus(orderId, status)
        FirebaseManager.syncOrderStatus(orderId, status)
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
        db.userDao().insert(User(email, name, sha256(password), false))
        return true
    }

    suspend fun checkLocalLogin(email: String, password: String): User? {
        val user = db.userDao().getByEmail(email) ?: return null
        return if (user.passwordHash == sha256(password)) user else null
    }
}
