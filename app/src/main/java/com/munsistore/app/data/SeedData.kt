package com.munsistore.app.data

import java.security.MessageDigest

/** Demo-only hashing for the local (offline) account store.
 *  Real passwords live in Firebase Auth once google-services.json is added. */
fun sha256(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

object SeedData {
    val adminUser = User(
        email = "admin@munsi.store",
        name = "Munsi Store Admin",
        passwordHash = sha256("admin123"),
        isAdmin = true
    )

    val products = listOf(
        Product(
            id = "p01", name = "Gillette Guard Razor — 1 Dozen", nameBn = "জিলেট গার্ড রেজর — ১ ডজন",
            description = "Original Gillette Guard disposable razors, pack of 12. Smooth and safe shave.",
            price = 240.0, oldPrice = 280.0, category = Categories.RAZOR,
            imageUrl = null, stock = 200, rating = 4.8f, isFeatured = true
        ),
        Product(
            id = "p02", name = "Lord Twin Blade Razor (2 pcs)", nameBn = "লর্ড টুইন ব্লেড রেজর (২ পিস)",
            description = "Egyptian twin-blade disposable razor by APCL (PRAN Group).",
            price = 40.0, oldPrice = null, category = Categories.RAZOR,
            imageUrl = null, stock = 500, rating = 4.5f, isFeatured = true
        ),
        Product(
            id = "p03", name = "Mr. Clean Toothbrush", nameBn = "মিস্টার ক্লিন টুথব্রাশ",
            description = "Soft bristle toothbrush, gentle on gums. Made in Bangladesh.",
            price = 35.0, oldPrice = 45.0, category = Categories.ORAL_CARE,
            imageUrl = null, stock = 300, rating = 4.6f, isFeatured = true
        ),
        Product(
            id = "p04", name = "Matador Toothbrush", nameBn = "ম্যাটাডর টুথব্রাশ",
            description = "Everyday toothbrush with medium bristles.",
            price = 30.0, oldPrice = null, category = Categories.ORAL_CARE,
            imageUrl = null, stock = 400, rating = 4.4f, isFeatured = false
        ),
        Product(
            id = "p05", name = "Lux Soap 100g", nameBn = "লাক্স সাবান ১০০ গ্রাম",
            description = "Beauty soap with soft fragrance.",
            price = 55.0, oldPrice = 65.0, category = Categories.BATH,
            imageUrl = null, stock = 250, rating = 4.7f, isFeatured = false
        ),
        Product(
            id = "p06", name = "Sunsilk Shampoo 170ml", nameBn = "সানসিল্ক শ্যাম্পু ১৭০ মিলি",
            description = "Shampoo for smooth and shiny hair.",
            price = 180.0, oldPrice = 210.0, category = Categories.BATH,
            imageUrl = null, stock = 120, rating = 4.6f, isFeatured = true
        ),
        Product(
            id = "p07", name = "Vaseline Body Lotion 200ml", nameBn = "ভ্যাসলিন বডি লোশন ২০০ মিলি",
            description = "Moisturising body lotion for soft skin.",
            price = 220.0, oldPrice = null, category = Categories.COSMETICS,
            imageUrl = null, stock = 90, rating = 4.7f, isFeatured = false
        ),
        Product(
            id = "p08", name = "Fair & Lovely Cream 50g", nameBn = "ফেয়ার অ্যান্ড লাভলি ক্রিম ৫০ গ্রাম",
            description = "Face cream for daily skin care.",
            price = 120.0, oldPrice = 140.0, category = Categories.COSMETICS,
            imageUrl = null, stock = 150, rating = 4.3f, isFeatured = false
        ),
        Product(
            id = "p09", name = "ACI Pure Salt 1kg", nameBn = "এসিআই পিওর লবণ ১ কেজি",
            description = "Iodised pure salt, 1 kg pack.",
            price = 38.0, oldPrice = null, category = Categories.GROCERY,
            imageUrl = null, stock = 600, rating = 4.8f, isFeatured = false
        ),
        Product(
            id = "p10", name = "Teer Sugar 1kg", nameBn = "তীর চিনি ১ কেজি",
            description = "Refined white sugar, 1 kg pack.",
            price = 120.0, oldPrice = null, category = Categories.GROCERY,
            imageUrl = null, stock = 350, rating = 4.8f, isFeatured = false
        ),
        Product(
            id = "p11", name = "Wheel Laundry Soap", nameBn = "হুইল কাপড় কাচা সাবান",
            description = "Laundry soap bar for everyday washing.",
            price = 45.0, oldPrice = null, category = Categories.GROCERY,
            imageUrl = null, stock = 500, rating = 4.5f, isFeatured = false
        ),
        Product(
            id = "p12", name = "Handmade Crochet Tote Bag", nameBn = "হাতে বোনা ক্রোশে টোট ব্যাগ",
            description = "Handmade granny-square tote bag, pink sunflower design. Made in Bangladesh.",
            price = 850.0, oldPrice = 1000.0, category = Categories.HANDMADE,
            imageUrl = null, stock = 10, rating = 5.0f, isFeatured = true
        )
    )
}
