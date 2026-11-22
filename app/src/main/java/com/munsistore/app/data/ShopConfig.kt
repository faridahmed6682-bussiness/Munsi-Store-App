package com.munsistore.app.data

/** Shop-wide constants. Change the payment numbers here to change them everywhere. */
object ShopConfig {
    // Manual mobile-banking "send money" numbers (shop's numbers).
    const val BKASH_NUMBER = "01636410157"
    const val NAGAD_NUMBER = "01636410157"
    const val ROCKET_NUMBER = "01636410157"

    /** Free delivery when the (post-discount) subtotal reaches this amount. */
    const val FREE_DELIVERY_THRESHOLD = 500.0

    /** Coins earned: 1 coin per this many taka spent on a delivered order. */
    const val COINS_EARN_DIVISOR = 10.0

    /** 1 coin is worth this many taka at checkout. */
    const val COIN_VALUE_TAKA = 1.0

    /** At most this fraction of the subtotal can be paid with coins. */
    const val MAX_COIN_REDEEM_FRACTION = 0.10

    /** Coins a new user gets for signing up with a referral code. */
    const val REFERRAL_SIGNUP_BONUS = 50

    /** Coins the referrer gets when the referred user's first order is delivered. */
    const val REFERRAL_DELIVERY_BONUS = 50

    /** Bangladeshi courier partners shown on shipped orders. */
    val COURIERS = listOf("Steadfast", "Pathao", "RedX", "Paperfly")

    /** Deterministic courier for an order (stable across reloads). */
    fun courierFor(orderId: String): String {
        val idx = (orderId.hashCode() and Int.MAX_VALUE) % COURIERS.size
        return COURIERS[idx]
    }

    /** Referral code derived from an email address (stable, shareable). */
    fun referralCodeFor(email: String): String {
        return "MUNSI-" + sha256(email.trim().lowercase()).take(6).uppercase()
    }
}
