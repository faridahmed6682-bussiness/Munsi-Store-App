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
    val brand: String = "Munsi",
    val imageUrl: String?,
    val stock: Int,
    /** Seeded base rating; the live average comes from [Review] rows when any exist. */
    val rating: Float,
    val soldCount: Int = 0,
    val createdAt: Long = 0L,
    val sellerId: String = "admin",
    val isFeatured: Boolean,
    val isFlashSale: Boolean = false,
    val flashPrice: Double? = null
)

/** Price the customer actually pays right now (flash price while the sale is live). */
fun effectivePrice(product: Product, flashActive: Boolean): Double {
    return if (flashActive && product.isFlashSale && product.flashPrice != null) {
        product.flashPrice
    } else {
        product.price
    }
}

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
    val deliveryZone: String = DeliveryZone.DHAKA_CITY,
    val paymentMethod: String, // COD, BKASH, NAGAD, ROCKET, SSLCOMMERZ
    val trxId: String? = null,
    val voucherCode: String? = null,
    val discountAmount: Double = 0.0,
    val coinsUsed: Int = 0,
    val deliveryFee: Double = 0.0,
    val subtotal: Double = 0.0,
    val totalAmount: Double,
    val status: String, // PLACED, CONFIRMED, PACKED, SHIPPED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED
    val courierName: String? = null,
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
    val isAdmin: Boolean,
    val isSeller: Boolean = false,
    val shopName: String = "",
    val coins: Int = 0,
    val referralCode: String = "",
    val referredBy: String = ""
)

@Entity(tableName = "reviews")
data class Review(
    @PrimaryKey val id: String, // "$productId|$userEmail" — one review per user per product
    val productId: String,
    val userEmail: String,
    val userName: String,
    val rating: Int, // 1..5
    val text: String,
    val createdAt: Long
)

@Entity(tableName = "questions")
data class ProductQuestion(
    @PrimaryKey val id: String,
    val productId: String,
    val userEmail: String,
    val question: String,
    val answer: String? = null,
    val createdAt: Long
)

@Entity(tableName = "vouchers")
data class Voucher(
    @PrimaryKey val code: String,
    val type: String, // PERCENT, FIXED, FREE_DELIVERY
    val value: Double, // percent (e.g. 10) or fixed taka amount; ignored for FREE_DELIVERY
    val minOrder: Double = 0.0,
    val active: Boolean = true,
    val expiresAt: Long = 0L // 0 = never expires
)

object VoucherType {
    const val PERCENT = "PERCENT"
    const val FIXED = "FIXED"
    const val FREE_DELIVERY = "FREE_DELIVERY"
}

/** Discount (in taka) this voucher gives on a given subtotal + delivery fee. */
fun discountFor(voucher: Voucher, subtotal: Double, deliveryFee: Double): Double {
    return when (voucher.type) {
        VoucherType.PERCENT -> subtotal * voucher.value / 100.0
        VoucherType.FIXED -> minOf(voucher.value, subtotal)
        VoucherType.FREE_DELIVERY -> deliveryFee
        else -> 0.0
    }
}

fun isVoucherUsable(voucher: Voucher, subtotal: Double, now: Long = System.currentTimeMillis()): Boolean {
    if (!voucher.active) return false
    if (voucher.expiresAt != 0L && voucher.expiresAt < now) return false
    if (subtotal < voucher.minOrder) return false
    return true
}

@Entity(tableName = "addresses")
data class Address(
    @PrimaryKey val id: String,
    val userEmail: String,
    val label: String, // Home / Office / Other
    val fullAddress: String,
    val zone: String, // DeliveryZone.*
    val phone: String,
    val isDefault: Boolean = false
)

@Entity(tableName = "recent_views")
data class RecentView(
    @PrimaryKey val productId: String,
    val viewedAt: Long
)

@Entity(tableName = "search_terms")
data class SearchTerm(
    @PrimaryKey val term: String,
    val count: Int,
    val lastSearchedAt: Long
)

@Entity(tableName = "return_requests")
data class ReturnRequest(
    @PrimaryKey val id: String,
    val orderId: String,
    val userEmail: String,
    val reason: String,
    val status: String, // REQUESTED, APPROVED, REJECTED, REFUNDED
    val createdAt: Long
)

@Entity(tableName = "flash_sale")
data class FlashSale(
    @PrimaryKey val id: Int = 1,
    val title: String,
    val endsAt: Long
)

/** A cart row joined with its product, for the UI. */
data class CartWithProduct(
    val product: Product,
    val quantity: Int
)

object OrderStatus {
    const val PLACED = "PLACED"
    const val CONFIRMED = "CONFIRMED"
    const val PACKED = "PACKED"
    const val SHIPPED = "SHIPPED"
    const val OUT_FOR_DELIVERY = "OUT_FOR_DELIVERY"
    const val DELIVERED = "DELIVERED"
    const val CANCELLED = "CANCELLED"

    /** Full delivery timeline shown to the customer. */
    val timeline = listOf(PLACED, CONFIRMED, PACKED, SHIPPED, OUT_FOR_DELIVERY, DELIVERED)
    val all = timeline + CANCELLED

    fun next(status: String): String? {
        val i = timeline.indexOf(status)
        return if (i >= 0 && i < timeline.size - 1) timeline[i + 1] else null
    }
}

object ReturnStatus {
    const val REQUESTED = "REQUESTED"
    const val APPROVED = "APPROVED"
    const val REJECTED = "REJECTED"
    const val REFUNDED = "REFUNDED"
}

object PaymentMethod {
    const val COD = "COD"
    const val BKASH = "BKASH"
    const val NAGAD = "NAGAD"
    const val ROCKET = "ROCKET"
    const val SSLCOMMERZ = "SSLCOMMERZ" // gateway-ready placeholder (not live yet)
}

/** Delivery zones with their charges (free when the order is over the threshold). */
object DeliveryZone {
    const val DHAKA_CITY = "dhaka_city"
    const val INSIDE_DHAKA = "inside_dhaka"
    const val OUTSIDE_DHAKA = "outside_dhaka"

    val all = listOf(DHAKA_CITY, INSIDE_DHAKA, OUTSIDE_DHAKA)

    fun fee(zone: String): Double = when (zone) {
        DHAKA_CITY -> 60.0
        INSIDE_DHAKA -> 80.0
        OUTSIDE_DHAKA -> 120.0
        else -> 60.0
    }
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

/** Sort orders for the catalog. */
object SortBy {
    const val NEWEST = "newest"
    const val POPULAR = "popular"
    const val PRICE_LOW = "price_low"
    const val PRICE_HIGH = "price_high"
    const val RATING = "rating"
}
