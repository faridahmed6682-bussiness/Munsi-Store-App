package com.munsistore.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: String,
    val name: String,
    val nameBn: String,
    val description: String,
    val price: Double,
    val oldPrice: Double?,
    val category: String,
    val imageUrl: String?,
    val stock: Int,
    val rating: Float,
    val isFeatured: Boolean
)

@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey val productId: String,
    val quantity: Int
)

@Entity(tableName = "wishlist_items")
data class WishlistItem(
    @PrimaryKey val productId: String
)

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey val id: String,
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String,
    val paymentMethod: String, // COD, BKASH, NAGAD
    val totalAmount: Double,
    val status: String, // PLACED, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    val createdAt: Long,
    val userEmail: String
)

@Entity(tableName = "order_items")
data class OrderItem(
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
    val orderId: String,
    val productId: String,
    val productName: String,
    val price: Double,
    val quantity: Int
)

@Entity(tableName = "users")
data class User(
    @PrimaryKey val email: String,
    val name: String,
    val passwordHash: String,
    val isAdmin: Boolean
)

/** A cart row joined with its product, for the UI. */
data class CartWithProduct(
    val product: Product,
    val quantity: Int
)

object OrderStatus {
    const val PLACED = "PLACED"
    const val CONFIRMED = "CONFIRMED"
    const val SHIPPED = "SHIPPED"
    const val DELIVERED = "DELIVERED"
    const val CANCELLED = "CANCELLED"

    val all = listOf(PLACED, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)
}

object PaymentMethod {
    const val COD = "COD"
    const val BKASH = "BKASH"
    const val NAGAD = "NAGAD"
}

/** Product categories (keys are stable; display names come from the UI map). */
object Categories {
    const val RAZOR = "razor"
    const val ORAL_CARE = "oral"
    const val BATH = "bath"
    const val GROCERY = "grocery"
    const val COSMETICS = "cosmetics"
    const val HANDMADE = "handmade"

    val all = listOf(RAZOR, ORAL_CARE, BATH, GROCERY, COSMETICS, HANDMADE)
}
