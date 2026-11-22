package com.munsistore.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Shower
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.munsistore.app.data.Categories
import com.munsistore.app.data.OrderStatus
import com.munsistore.app.data.Product

fun formatPrice(price: Double): String {
    return if (price == price.toLong().toDouble()) "৳${price.toLong()}" else "৳%.2f".format(price)
}

fun categoryColor(category: String): Color {
    return when (category) {
        Categories.RAZOR -> Color(0xFF1565C0)
        Categories.ORAL_CARE -> Color(0xFF00897B)
        Categories.BATH -> Color(0xFF6A1B9A)
        Categories.GROCERY -> Color(0xFFEF6C00)
        Categories.COSMETICS -> Color(0xFFC2185B)
        Categories.HANDMADE -> Color(0xFF5D4037)
        else -> Color(0xFF2E7D32)
    }
}

fun categoryIcon(category: String): ImageVector {
    return when (category) {
        Categories.RAZOR -> Icons.Filled.ContentCut
        Categories.ORAL_CARE -> Icons.Filled.Brush
        Categories.BATH -> Icons.Filled.Shower
        Categories.GROCERY -> Icons.Filled.ShoppingBasket
        Categories.COSMETICS -> Icons.Filled.Spa
        Categories.HANDMADE -> Icons.Filled.Handyman
        else -> Icons.Filled.Store
    }
}

/** Product image: remote photo when an URL exists, otherwise a coloured category tile. */
@Composable
fun ProductTile(product: Product, modifier: Modifier = Modifier) {
    if (!product.imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = product.imageUrl,
            contentDescription = product.name,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(12.dp))
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(categoryColor(product.category).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = categoryIcon(product.category),
                contentDescription = null,
                tint = categoryColor(product.category),
                modifier = Modifier.size(56.dp)
            )
        }
    }
}

fun statusColor(status: String): Color {
    return when (status) {
        OrderStatus.PLACED -> Color(0xFFEF6C00)
        OrderStatus.CONFIRMED -> Color(0xFF1565C0)
        OrderStatus.PACKED -> Color(0xFF00838F)
        OrderStatus.SHIPPED -> Color(0xFF6A1B9A)
        OrderStatus.OUT_FOR_DELIVERY -> Color(0xFF4527A0)
        OrderStatus.DELIVERED -> Color(0xFF2E7D32)
        OrderStatus.CANCELLED -> Color(0xFFC62828)
        else -> Color.Gray
    }
}

@Composable
fun StatusChip(status: String, label: String) {
    val color = statusColor(status)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/** Row of 5 stars showing an average rating (fills whole stars, rounds half up). */
@Composable
fun StarRating(rating: Double, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        for (i in 1..5) {
            androidx.compose.material3.Icon(
                imageVector = if (rating >= i - 0.25) Icons.Filled.Star
                else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** Tap-to-pick star input (1..5). */
@Composable
fun StarInput(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        for (i in 1..5) {
            androidx.compose.material3.IconButton(onClick = { onSelect(i) }) {
                androidx.compose.material3.Icon(
                    imageVector = if (i <= selected) Icons.Filled.Star
                    else Icons.Filled.StarBorder,
                    contentDescription = "$i",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

/** Formats a countdown (millis left) as HH:MM:SS. */
fun formatCountdown(millisLeft: Long): String {
    val totalSeconds = (millisLeft.coerceAtLeast(0L) / 1000)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
