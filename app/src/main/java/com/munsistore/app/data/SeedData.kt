package com.munsistore.app.data

import java.security.MessageDigest

/** Demo-only hashing for the local (offline) account store.
 *  Real passwords live in Firebase Auth once google-services.json is added. */
fun sha256(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

object SeedData {
    private val now = System.currentTimeMillis()
    private const val DAY = 86_400_000L

    val adminUser = User(
        email = "admin@munsi.store",
        name = "Munsi Store Admin",
        passwordHash = sha256("admin123"),
        isAdmin = true,
        referralCode = ShopConfig.referralCodeFor("admin@munsi.store")
    )

    val products = listOf(
        Product(
            id = "p01", name = "Gillette Guard Razor — 1 Dozen", nameBn = "জিলেট গার্ড রেজর — ১ ডজন",
            description = "Original Gillette Guard disposable razors, pack of 12. Smooth and safe shave.",
            price = 240.0, oldPrice = 280.0, category = Categories.RAZOR, brand = "Gillette",
            imageUrl = null, stock = 200, rating = 4.8f, soldCount = 320,
            createdAt = now - 25 * DAY, isFeatured = true,
            isFlashSale = true, flashPrice = 199.0
        ),
        Product(
            id = "p02", name = "Lord Twin Blade Razor (2 pcs)", nameBn = "লর্ড টুইন ব্লেড রেজর (২ পিস)",
            description = "Egyptian twin-blade disposable razor by APCL (PRAN Group).",
            price = 40.0, oldPrice = null, category = Categories.RAZOR, brand = "Lord",
            imageUrl = null, stock = 500, rating = 4.5f, soldCount = 540,
            createdAt = now - 40 * DAY, isFeatured = true
        ),
        Product(
            id = "p03", name = "Mr. Clean Toothbrush", nameBn = "মিস্টার ক্লিন টুথব্রাশ",
            description = "Soft bristle toothbrush, gentle on gums. Made in Bangladesh.",
            price = 35.0, oldPrice = 45.0, category = Categories.ORAL_CARE, brand = "Mr. Clean",
            imageUrl = null, stock = 300, rating = 4.6f, soldCount = 410,
            createdAt = now - 30 * DAY, isFeatured = true,
            isFlashSale = true, flashPrice = 29.0
        ),
        Product(
            id = "p04", name = "Matador Toothbrush", nameBn = "ম্যাটাডর টুথব্রাশ",
            description = "Everyday toothbrush with medium bristles.",
            price = 30.0, oldPrice = null, category = Categories.ORAL_CARE, brand = "Matador",
            imageUrl = null, stock = 400, rating = 4.4f, soldCount = 260,
            createdAt = now - 45 * DAY, isFeatured = false
        ),
        Product(
            id = "p05", name = "Lux Soap 100g", nameBn = "লাক্স সাবান ১০০ গ্রাম",
            description = "Beauty soap with soft fragrance.",
            price = 55.0, oldPrice = 65.0, category = Categories.BATH, brand = "Lux",
            imageUrl = null, stock = 250, rating = 4.7f, soldCount = 380,
            createdAt = now - 20 * DAY, isFeatured = false
        ),
        Product(
            id = "p06", name = "Sunsilk Shampoo 170ml", nameBn = "সানসিল্ক শ্যাম্পু ১৭০ মিলি",
            description = "Shampoo for smooth and shiny hair.",
            price = 180.0, oldPrice = 210.0, category = Categories.BATH, brand = "Sunsilk",
            imageUrl = null, stock = 120, rating = 4.6f, soldCount = 190,
            createdAt = now - 12 * DAY, isFeatured = true,
            isFlashSale = true, flashPrice = 149.0
        ),
        Product(
            id = "p07", name = "Vaseline Body Lotion 200ml", nameBn = "ভ্যাসলিন বডি লোশন ২০০ মিলি",
            description = "Moisturising body lotion for soft skin.",
            price = 220.0, oldPrice = null, category = Categories.COSMETICS, brand = "Vaseline",
            imageUrl = null, stock = 90, rating = 4.7f, soldCount = 95,
            createdAt = now - 8 * DAY, isFeatured = false
        ),
        Product(
            id = "p08", name = "Fair & Lovely Cream 50g", nameBn = "ফেয়ার অ্যান্ড লাভলি ক্রিম ৫০ গ্রাম",
            description = "Face cream for daily skin care.",
            price = 120.0, oldPrice = 140.0, category = Categories.COSMETICS, brand = "Fair & Lovely",
            imageUrl = null, stock = 150, rating = 4.3f, soldCount = 140,
            createdAt = now - 15 * DAY, isFeatured = false
        ),
        Product(
            id = "p09", name = "ACI Pure Salt 1kg", nameBn = "এসিআই পিওর লবণ ১ কেজি",
            description = "Iodised pure salt, 1 kg pack.",
            price = 38.0, oldPrice = null, category = Categories.GROCERY, brand = "ACI",
            imageUrl = null, stock = 600, rating = 4.8f, soldCount = 720,
            createdAt = now - 50 * DAY, isFeatured = false
        ),
        Product(
            id = "p10", name = "Teer Sugar 1kg", nameBn = "তীর চিনি ১ কেজি",
            description = "Refined white sugar, 1 kg pack.",
            price = 120.0, oldPrice = null, category = Categories.GROCERY, brand = "Teer",
            imageUrl = null, stock = 350, rating = 4.8f, soldCount = 480,
            createdAt = now - 35 * DAY, isFeatured = false
        ),
        Product(
            id = "p11", name = "Wheel Laundry Soap", nameBn = "হুইল কাপড় কাচা সাবান",
            description = "Laundry soap bar for everyday washing.",
            price = 45.0, oldPrice = null, category = Categories.GROCERY, brand = "Wheel",
            imageUrl = null, stock = 500, rating = 4.5f, soldCount = 390,
            createdAt = now - 28 * DAY, isFeatured = false
        ),
        Product(
            id = "p12", name = "Handmade Crochet Tote Bag", nameBn = "হাতে বোনা ক্রোশে টোট ব্যাগ",
            description = "Handmade granny-square tote bag, pink sunflower design. Made in Bangladesh.",
            price = 850.0, oldPrice = 1000.0, category = Categories.HANDMADE, brand = "Munsi Handmade",
            imageUrl = null, stock = 10, rating = 5.0f, soldCount = 22,
            createdAt = now - 5 * DAY, isFeatured = true,
            isFlashSale = true, flashPrice = 749.0
        )
    )

    /** Flash sale ends 36 hours after the first seed. */
    val flashSale = FlashSale(
        id = 1,
        title = "Flash Sale",
        endsAt = now + 36 * 3_600_000L
    )

    val vouchers = listOf(
        Voucher(code = "WELCOME10", type = VoucherType.PERCENT, value = 10.0, minOrder = 0.0),
        Voucher(code = "FLAT50", type = VoucherType.FIXED, value = 50.0, minOrder = 300.0),
        Voucher(code = "FREEDLV", type = VoucherType.FREE_DELIVERY, value = 0.0, minOrder = 200.0),
        Voucher(code = "EID20", type = VoucherType.PERCENT, value = 20.0, minOrder = 1000.0)
    )

    val reviews = listOf(
        Review(
            id = "p01|mehedi@example.com", productId = "p01",
            userEmail = "mehedi@example.com", userName = "Mehedi",
            rating = 5, text = "Original product, very smooth shave. Delivery was fast.",
            createdAt = now - 3 * DAY
        ),
        Review(
            id = "p01|rabeya@example.com", productId = "p01",
            userEmail = "rabeya@example.com", userName = "Rabeya",
            rating = 4, text = "Good quality for the price. Packing could be better.",
            createdAt = now - 6 * DAY
        ),
        Review(
            id = "p03|mehedi@example.com", productId = "p03",
            userEmail = "mehedi@example.com", userName = "Mehedi",
            rating = 5, text = "Soft bristles, genuine Mr. Clean brush.",
            createdAt = now - 2 * DAY
        ),
        Review(
            id = "p12|nusrat@example.com", productId = "p12",
            userEmail = "nusrat@example.com", userName = "Nusrat",
            rating = 5, text = "Beautiful handmade bag! The sunflower design is lovely.",
            createdAt = now - 1 * DAY
        )
    )

    val questions = listOf(
        ProductQuestion(
            id = "q01", productId = "p01", userEmail = "mehedi@example.com",
            question = "Is this the original Gillette Guard from India?",
            answer = "Yes, 100% original Gillette Guard, imported pack of 12.",
            createdAt = now - 4 * DAY
        ),
        ProductQuestion(
            id = "q02", productId = "p01", userEmail = "rabeya@example.com",
            question = "What is the delivery charge outside Dhaka?",
            answer = "Outside Dhaka delivery is ৳120. Free delivery over ৳500.",
            createdAt = now - 2 * DAY
        )
    )

    val trendingTerms = listOf(
        SearchTerm(term = "razor", count = 42, lastSearchedAt = now - 1 * DAY),
        SearchTerm(term = "toothbrush", count = 31, lastSearchedAt = now - 1 * DAY),
        SearchTerm(term = "soap", count = 25, lastSearchedAt = now - 2 * DAY),
        SearchTerm(term = "shampoo", count = 18, lastSearchedAt = now - 2 * DAY),
        SearchTerm(term = "crochet", count = 12, lastSearchedAt = now - 3 * DAY)
    )
}
