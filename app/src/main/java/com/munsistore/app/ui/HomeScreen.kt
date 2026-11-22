package com.munsistore.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.munsistore.app.R
import com.munsistore.app.data.Categories
import com.munsistore.app.data.Product
import com.munsistore.app.data.SortBy
import com.munsistore.app.data.effectivePrice
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(viewModel: StoreViewModel, onProductClick: (String) -> Unit) {
    val products by viewModel.products.collectAsState()
    val wishlistIds by viewModel.wishlistIds.collectAsState()
    val filtered = viewModel.applyFilter(products)
    val flashProducts by viewModel.flashProducts.collectAsState()
    val recent by viewModel.recentProducts.collectAsState()
    val recommended by viewModel.recommendedProducts.collectAsState()
    val trending by viewModel.trendingTerms.collectAsState()
    val brands by viewModel.brands.collectAsState()
    val flashOn = viewModel.isFlashActive()
    var showFilters by remember { mutableStateOf(false) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(2) }) {
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.shop_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = viewModel.selectedCategory == null,
                            onClick = { viewModel.setCategory(null) },
                            label = { Text(stringResource(R.string.all)) }
                        )
                    }
                    items(Categories.all) { cat ->
                        FilterChip(
                            selected = viewModel.selectedCategory == cat,
                            onClick = { viewModel.setCategory(cat) },
                            label = { Text(viewModel.categoryName(cat)) }
                        )
                    }
                }
            }
        }

        // Trending searches
        if (viewModel.searchQuery.isBlank() && trending.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = stringResource(R.string.trending_searches),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(trending) { term ->
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.updateSearchQuery(term.term) },
                                label = { Text(term.term) }
                            )
                        }
                    }
                }
            }
        }

        // Flash sale with live countdown
        if (flashOn && flashProducts.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                FlashSaleSection(viewModel = viewModel, onProductClick = onProductClick)
            }
        }

        // Recently viewed
        if (recent.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Column {
                    Text(
                        text = stringResource(R.string.recently_viewed),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(recent) { product ->
                            MiniProductCard(product = product, viewModel = viewModel, onClick = { onProductClick(product.id) })
                        }
                    }
                }
            }
        }

        // Recommended for you
        if (recommended.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Column {
                    Text(
                        text = stringResource(R.string.recommended_for_you),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(recommended) { product ->
                            MiniProductCard(product = product, viewModel = viewModel, onClick = { onProductClick(product.id) })
                        }
                    }
                }
            }
        }

        // Filter + sort bar
        item(span = { GridItemSpan(2) }) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButtonLike(
                        onClick = { showFilters = true },
                        label = stringResource(R.string.filter)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = viewModel.sortBy == SortBy.NEWEST,
                                onClick = { viewModel.updateSortBy(SortBy.NEWEST) },
                                label = { Text(stringResource(R.string.sort_newest)) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = viewModel.sortBy == SortBy.POPULAR,
                                onClick = { viewModel.updateSortBy(SortBy.POPULAR) },
                                label = { Text(stringResource(R.string.sort_popular)) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = viewModel.sortBy == SortBy.PRICE_LOW,
                                onClick = { viewModel.updateSortBy(SortBy.PRICE_LOW) },
                                label = { Text(stringResource(R.string.sort_price_low)) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = viewModel.sortBy == SortBy.PRICE_HIGH,
                                onClick = { viewModel.updateSortBy(SortBy.PRICE_HIGH) },
                                label = { Text(stringResource(R.string.sort_price_high)) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = viewModel.sortBy == SortBy.RATING,
                                onClick = { viewModel.updateSortBy(SortBy.RATING) },
                                label = { Text(stringResource(R.string.sort_rating)) }
                            )
                        }
                    }
                }
                if (viewModel.brandFilter != null || viewModel.priceMin != null ||
                    viewModel.priceMax != null || viewModel.minRating > 0f
                ) {
                    TextButton(onClick = { viewModel.clearFilters() }) {
                        Text(stringResource(R.string.clear_filters))
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.no_products))
                }
            }
        } else {
            items(filtered) { product ->
                ProductCard(
                    product = product,
                    isWishlisted = wishlistIds.contains(product.id),
                    showBnName = viewModel.language == "bn",
                    flashActive = flashOn,
                    viewModel = viewModel,
                    onClick = { onProductClick(product.id) },
                    onAddToCart = { viewModel.addToCart(product.id) },
                    onToggleWishlist = { viewModel.toggleWishlist(product.id) }
                )
            }
        }
    }

    if (showFilters) {
        FilterDialog(
            viewModel = viewModel,
            brands = brands,
            onDismiss = { showFilters = false }
        )
    }
}

@Composable
private fun OutlinedButtonLike(onClick: () -> Unit, label: String) {
    androidx.compose.material3.OutlinedButton(onClick = onClick) {
        Icon(Icons.Filled.FilterList, contentDescription = null)
        Spacer(modifier = Modifier.width(4.dp))
        Text(label)
    }
}

@Composable
private fun FlashSaleSection(viewModel: StoreViewModel, onProductClick: (String) -> Unit) {
    val sale = viewModel.flashSale ?: return
    val flashProducts by viewModel.flashProducts.collectAsState()
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(sale.endsAt) {
        while (now < sale.endsAt) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    val remaining = sale.endsAt - now
    if (remaining <= 0 || flashProducts.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FlashOn, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(
                    text = stringResource(R.string.flash_sale),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.ends_in) + " " + formatCountdown(remaining),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(flashProducts) { product ->
                    MiniProductCard(product = product, viewModel = viewModel, onClick = { onProductClick(product.id) })
                }
            }
        }
    }
}

@Composable
private fun MiniProductCard(product: Product, viewModel: StoreViewModel, onClick: () -> Unit) {
    val flashOn = viewModel.isFlashActive()
    val price = effectivePrice(product, flashOn)
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            ProductTile(
                product = product,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (viewModel.language == "bn") product.nameBn else product.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatPrice(price),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (flashOn && product.isFlashSale && product.flashPrice != null) {
                    Text(
                        text = formatPrice(product.price),
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = TextDecoration.LineThrough,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterDialog(viewModel: StoreViewModel, brands: List<String>, onDismiss: () -> Unit) {
    var minText by remember { mutableStateOf(viewModel.priceMin?.toInt()?.toString() ?: "") }
    var maxText by remember { mutableStateOf(viewModel.priceMax?.toInt()?.toString() ?: "") }
    var brand by remember { mutableStateOf(viewModel.brandFilter) }
    var rating by remember { mutableStateOf(viewModel.minRating) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filter)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.price_range), fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minText,
                        onValueChange = { minText = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.min_price)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxText,
                        onValueChange = { maxText = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.max_price)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(text = stringResource(R.string.brand), fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = brand == null,
                            onClick = { brand = null },
                            label = { Text(stringResource(R.string.all)) }
                        )
                    }
                    items(brands) { b ->
                        FilterChip(
                            selected = brand == b,
                            onClick = { brand = b },
                            label = { Text(b) }
                        )
                    }
                }
                Text(text = stringResource(R.string.min_rating), fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0f to stringResource(R.string.any_rating), 4f to "4★+", 4.5f to "4.5★+").forEach { (value, label) ->
                        FilterChip(
                            selected = rating == value,
                            onClick = { rating = value },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                viewModel.setPriceRange(minText.toDoubleOrNull(), maxText.toDoubleOrNull())
                viewModel.updateBrandFilter(brand)
                viewModel.updateMinRating(rating)
                onDismiss()
            }) { Text(stringResource(R.string.apply_filters)) }
        },
        dismissButton = {
            TextButton(onClick = {
                viewModel.clearFilters()
                onDismiss()
            }) { Text(stringResource(R.string.clear_filters)) }
        }
    )
}

@Composable
fun ProductCard(
    product: Product,
    isWishlisted: Boolean,
    showBnName: Boolean,
    flashActive: Boolean,
    viewModel: StoreViewModel,
    onClick: () -> Unit,
    onAddToCart: () -> Unit,
    onToggleWishlist: () -> Unit
) {
    // Live average from reviews (falls back to the seeded rating).
    var liveRating by remember { mutableStateOf(product.rating.toDouble()) }
    var reviewCount by remember { mutableStateOf(0) }
    LaunchedEffect(product.id) {
        liveRating = viewModel.averageRating(product.id, product.rating)
        viewModel.reviewsOf(product.id).collect { list -> reviewCount = list.size }
    }
    val price = effectivePrice(product, flashActive)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            ProductTile(
                product = product,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (showBnName) product.nameBn else product.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(end = 2.dp)
                )
                Text(
                    text = "%.1f".format(liveRating) + if (reviewCount > 0) " ($reviewCount)" else "",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (product.stock > 0) stringResource(R.string.in_stock) else stringResource(R.string.out_of_stock),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (product.stock > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatPrice(price),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (product.oldPrice != null || (flashActive && product.isFlashSale && product.flashPrice != null)) {
                    Text(
                        text = formatPrice(product.oldPrice ?: product.price),
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = TextDecoration.LineThrough,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                if (flashActive && product.isFlashSale && product.flashPrice != null) {
                    val off = ((1 - price / product.price) * 100).toInt()
                    Text(
                        text = "-$off%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
            Text(
                text = stringResource(R.string.sold_count) + ": " + product.soldCount,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleWishlist) {
                    Icon(
                        imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = stringResource(R.string.wishlist),
                        tint = if (isWishlisted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onAddToCart,
                    enabled = product.stock > 0
                ) {
                    Icon(
                        imageVector = Icons.Filled.ShoppingCart,
                        contentDescription = stringResource(R.string.add_to_cart),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
