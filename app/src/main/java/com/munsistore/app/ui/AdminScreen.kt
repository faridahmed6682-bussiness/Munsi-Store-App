package com.munsistore.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import com.munsistore.app.R
import com.munsistore.app.data.Categories
import com.munsistore.app.data.Order
import com.munsistore.app.data.OrderStatus
import com.munsistore.app.data.Product
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: StoreViewModel,
    onBack: () -> Unit,
    onAddProduct: () -> Unit,
    onEditProduct: (String) -> Unit
) {
    var tab by remember { mutableStateOf(0) } // 0 = orders, 1 = products
    val allOrders by viewModel.allOrders.collectAsState()
    val products by viewModel.products.collectAsState()
    val totalSales by viewModel.totalSales.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    if (!viewModel.isAdmin) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.admin_panel)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.login_required))
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_panel)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            if (tab == 1) {
                FloatingActionButton(onClick = onAddProduct) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_product))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Dashboard cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DashboardCard(
                    label = stringResource(R.string.total_sales),
                    value = formatPrice(totalSales),
                    modifier = Modifier.weight(1f)
                )
                DashboardCard(
                    label = stringResource(R.string.total_orders),
                    value = allOrders.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                DashboardCard(
                    label = stringResource(R.string.pending_orders),
                    value = pendingCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                DashboardCard(
                    label = stringResource(R.string.products),
                    value = products.size.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.manage_orders)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.manage_products)) })
            }
            if (tab == 0) {
                if (allOrders.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(stringResource(R.string.no_orders))
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
                    ) {
                        items(allOrders) { order ->
                            AdminOrderRow(
                                order = order,
                                onStatusChange = { status ->
                                    viewModel.updateOrderStatus(order.id, status)
                                }
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
                ) {
                    items(products) { product ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = formatPrice(product.price) + " • " +
                                            stringResource(R.string.stock) + ": " + product.stock,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                IconButton(onClick = { onEditProduct(product.id) }) {
                                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_product))
                                }
                                IconButton(onClick = { productToDelete = product }) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_product))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val deleting = productToDelete
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text(stringResource(R.string.delete_product)) },
            text = { Text(stringResource(R.string.delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteProduct(deleting)
                    productToDelete = null
                }) { Text(stringResource(R.string.delete_product)) }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun DashboardCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun AdminOrderRow(order: Order, onStatusChange: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = order.id, fontWeight = FontWeight.Bold)
                StatusChip(status = order.status, label = statusLabel(order.status))
            }
            Text(
                text = order.customerName + " • " + order.customerPhone,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = formatPrice(order.totalAmount) + " • " + order.paymentMethod,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (order.status == OrderStatus.PLACED) {
                    OutlinedButton(onClick = { onStatusChange(OrderStatus.CONFIRMED) }) {
                        Text(stringResource(R.string.confirmed))
                    }
                }
                if (order.status == OrderStatus.CONFIRMED) {
                    OutlinedButton(onClick = { onStatusChange(OrderStatus.SHIPPED) }) {
                        Text(stringResource(R.string.shipped))
                    }
                }
                if (order.status == OrderStatus.SHIPPED) {
                    OutlinedButton(onClick = { onStatusChange(OrderStatus.DELIVERED) }) {
                        Text(stringResource(R.string.delivered))
                    }
                }
                if (order.status != OrderStatus.DELIVERED && order.status != OrderStatus.CANCELLED) {
                    OutlinedButton(onClick = { onStatusChange(OrderStatus.CANCELLED) }) {
                        Text(stringResource(R.string.cancelled))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProductScreen(
    viewModel: StoreViewModel,
    productId: String,
    onBack: () -> Unit
) {
    val isNew = productId == "new"
    var existing by remember { mutableStateOf<Product?>(null) }
    var name by remember { mutableStateOf("") }
    var nameBn by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var oldPrice by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Categories.GROCERY) }
    var stock by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var featured by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(isNew) }

    LaunchedEffect(productId) {
        if (!isNew) {
            val p = viewModel.getProduct(productId)
            if (p != null) {
                existing = p
                name = p.name
                nameBn = p.nameBn
                description = p.description
                price = p.price.toString()
                oldPrice = p.oldPrice?.toString() ?: ""
                category = p.category
                stock = p.stock.toString()
                imageUrl = p.imageUrl ?: ""
                featured = p.isFeatured
            }
            loaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isNew) stringResource(R.string.add_product)
                        else stringResource(R.string.edit_product)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        if (!loaded) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text(stringResource(R.string.product_name)) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = nameBn, onValueChange = { nameBn = it },
                label = { Text(stringResource(R.string.product_name_bn)) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text(stringResource(R.string.description)) },
                minLines = 2, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = price, onValueChange = { price = it },
                label = { Text(stringResource(R.string.price) + " (৳)") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = oldPrice, onValueChange = { oldPrice = it },
                label = { Text(stringResource(R.string.price) + " (old, optional)") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Text(text = stringResource(R.string.category), fontWeight = FontWeight.SemiBold)
            Categories.all.forEach { cat ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = category == cat,
                        onClick = { category = cat }
                    )
                    Text(text = viewModel.categoryName(cat), modifier = Modifier.padding(start = 8.dp))
                }
            }
            OutlinedTextField(
                value = stock, onValueChange = { stock = it },
                label = { Text(stringResource(R.string.stock)) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = imageUrl, onValueChange = { imageUrl = it },
                label = { Text(stringResource(R.string.image_url)) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = featured, onCheckedChange = { featured = it })
                Text(text = stringResource(R.string.featured))
            }
            Button(
                onClick = {
                    val priceValue = price.toDoubleOrNull() ?: 0.0
                    val stockValue = stock.toIntOrNull() ?: 0
                    val product = Product(
                        id = existing?.id ?: "p-" + UUID.randomUUID().toString().take(8),
                        name = name.trim(),
                        nameBn = nameBn.trim().ifBlank { name.trim() },
                        description = description.trim(),
                        price = priceValue,
                        oldPrice = oldPrice.toDoubleOrNull(),
                        category = category,
                        imageUrl = imageUrl.trim().ifBlank { null },
                        stock = stockValue,
                        rating = existing?.rating ?: 4.5f,
                        isFeatured = featured
                    )
                    viewModel.saveProduct(product)
                    onBack()
                },
                enabled = name.isNotBlank() && price.toDoubleOrNull() != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
