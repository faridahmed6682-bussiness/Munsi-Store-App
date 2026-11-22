package com.munsistore.app.data

import android.content.Context
import android.net.Uri
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

/**
 * Firebase (Spark free plan) — wired in, but OPTIONAL at runtime.
 *
 * The google-services plugin is NOT applied, so the app builds and runs
 * fully offline without google-services.json. When the shop owner adds the
 * real google-services.json (Firebase console -> Project settings -> Your
 * apps -> Android -> download, place in app/), FirebaseApp initialises and
 * [isConfigured] becomes true: Auth sign-in, Firestore order/product sync
 * and Storage image upload light up. Until then every call below is a
 * guarded no-op and the local Room database is the source of truth.
 */
object FirebaseManager {
    var isConfigured: Boolean = false
        private set

    fun init(context: Context) {
        try {
            val apps = FirebaseApp.getApps(context)
            val app = apps.firstOrNull() ?: FirebaseApp.initializeApp(context)
            isConfigured = app != null
        } catch (e: Exception) {
            isConfigured = false
        }
    }

    fun auth(): FirebaseAuth? = if (isConfigured) {
        try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
    } else null

    fun firestore(): FirebaseFirestore? = if (isConfigured) {
        try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }
    } else null

    fun storage(): FirebaseStorage? = if (isConfigured) {
        try { FirebaseStorage.getInstance() } catch (e: Exception) { null }
    } else null

    /** Best-effort: mirror an order to Firestore so the admin can see it online. */
    fun syncOrder(order: Order, items: List<OrderItem>) {
        val db = firestore() ?: return
        try {
            val data = hashMapOf<String, Any>(
                "customerName" to order.customerName,
                "customerPhone" to order.customerPhone,
                "customerAddress" to order.customerAddress,
                "paymentMethod" to order.paymentMethod,
                "totalAmount" to order.totalAmount,
                "status" to order.status,
                "createdAt" to order.createdAt,
                "userEmail" to order.userEmail,
                "items" to items.map {
                    hashMapOf(
                        "productId" to it.productId,
                        "productName" to it.productName,
                        "price" to it.price,
                        "quantity" to it.quantity
                    )
                }
            )
            db.collection("orders").document(order.id).set(data)
        } catch (e: Exception) {
            // Offline / not configured: local Room copy is the truth.
        }
    }

    fun syncOrderStatus(orderId: String, status: String) {
        val db = firestore() ?: return
        try {
            db.collection("orders").document(orderId).update("status", status)
        } catch (e: Exception) {
        }
    }

    fun syncProduct(product: Product) {
        val db = firestore() ?: return
        try {
            val data = hashMapOf<String, Any>(
                "name" to product.name,
                "nameBn" to product.nameBn,
                "description" to product.description,
                "price" to product.price,
                "category" to product.category,
                "stock" to product.stock,
                "rating" to product.rating,
                "isFeatured" to product.isFeatured
            )
            product.oldPrice?.let { data["oldPrice"] = it }
            product.imageUrl?.let { data["imageUrl"] = it }
            db.collection("products").document(product.id).set(data)
        } catch (e: Exception) {
        }
    }

    fun deleteProductRemote(productId: String) {
        val db = firestore() ?: return
        try {
            db.collection("products").document(productId).delete()
        } catch (e: Exception) {
        }
    }

    /** Upload a product image to Firebase Storage; calls back with the download URL or null. */
    fun uploadProductImage(uri: Uri, onDone: (String?) -> Unit) {
        val storage = storage()
        if (storage == null) {
            onDone(null)
            return
        }
        try {
            val ref = storage.reference.child("product_images/${System.currentTimeMillis()}.jpg")
            ref.putFile(uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception ?: Exception("upload failed")
                    ref.downloadUrl
                }
                .addOnSuccessListener { downloadUri -> onDone(downloadUri.toString()) }
                .addOnFailureListener { onDone(null) }
        } catch (e: Exception) {
            onDone(null)
        }
    }
}
