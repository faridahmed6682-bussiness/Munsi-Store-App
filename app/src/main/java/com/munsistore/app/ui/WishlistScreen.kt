package com.munsistore.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.munsistore.app.R

@Composable
fun WishlistScreen(viewModel: StoreViewModel, onProductClick: (String) -> Unit) {
    val products by viewModel.wishlistProducts.collectAsState()
    val wishlistIds by viewModel.wishlistIds.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.wishlist),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
        if (products.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.empty_wishlist))
            }
            return@Column
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(products) { product ->
                ProductCard(
                    product = product,
                    isWishlisted = wishlistIds.contains(product.id),
                    showBnName = viewModel.language == "bn",
                    flashActive = viewModel.isFlashActive(),
                    viewModel = viewModel,
                    onClick = { onProductClick(product.id) },
                    onAddToCart = { viewModel.addToCart(product.id) },
                    onToggleWishlist = { viewModel.toggleWishlist(product.id) }
                )
            }
        }
    }
}
