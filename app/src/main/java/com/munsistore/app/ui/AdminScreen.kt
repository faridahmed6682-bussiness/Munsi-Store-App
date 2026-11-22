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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
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
import com.munsistore.app.data.ReturnRequest
import com.munsistore.app.data.ReturnStatus
import com.munsistore.app.data.Voucher
import com.munsistore.app.data.VoucherType
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: StoreViewModel,
    onBack: () -> Unit,
    onAddProduct: () -> Unit,
    onEditProduct: (String) -> Unit
) {
    var tab by remember { mutableStateOf(0) } // 0 orders, 1 products, 2 vouchers, 3 returns
    val allOrders by viewModel.allOrders.collectAsState()
    val products by viewModel.products.collectAsState()
    val vouchers by viewModel.vouchers.collectAsState()
    val returns by viewModel.allReturns.collectAsState()
    val totalSales by viewModel.totalSales.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var voucherToDelete by remember { mutableStateOf<Voucher?>(null) }

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
            ScrollableTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.manage_orders)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.manage_products)) })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(stringResource(R.string.vouchers)) })
                Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text(stringResource(R.string.returns)) })
            }
            when (tab) {
                0 -> OrdersTab(allOrders = allOrders, viewModel = viewModel)
                1 -> ProductsTab(
                    products = products,
                    onEditProduct = onEditProduct,
                    onDeleteProduct = { productToDelete = it }
                )
                2 -> VouchersTab(
                    vouchers = vouchers,
                    viewModel = viewModel,
                    onDeleteVoucher = { voucherToDelete = it }
                )
                3 -> ReturnsTab(returns = returns, viewModel = viewModel)
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
    val deletingVoucher = voucherToDelete
    if (deletingVoucher != null) {
        AlertDialog(
            onDismissRequest = { voucherToDelete = null },
            title = { Text(stringResource(R.string.delete_voucher)) },
            text = { Text(stringResource(R.string.delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteVoucher(deletingVoucher)
                    voucherToDelete = null
                }) { Text(stringResource(R.string.delete_voucher)) }
            },
            dismissButton = {
                TextButton(onClick = { voucherToDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun OrdersTab(allOrders: List<Order>, viewModel: StoreViewModel) {
    if (allOrders.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(stringResource(R.string.no_orders))
        }
        return
    }
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

@Composable
private fun ProductsTab(
    products: List<Product>,
    onEditProduct: (String) -> Unit,
    onDeleteProduct: (Product) -> Unit
) {
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
                                stringResource(R.string.stock) + ": " + product.stock +
                                " • " + product.brand,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(onClick = { onEditProduct(product.id) }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_product))
                    }
                    IconButton(onClick = { onDeleteProduct(product) }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_product))
                    }
                }
            }
        }
    }
}

@Composable
private fun VouchersTab(
    vouchers: List<Voucher>,
    viewModel: StoreViewModel,
    onDeleteVoucher: (Voucher) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(VoucherType.PERCENT) }
    var value by remember { mutableStateOf("") }
    var minOrder by remember { mutableStateOf("") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.create_voucher),
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase() },
                        label = { Text(stringResource(R.string.voucher_code_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            VoucherType.PERCENT to stringResource(R.string.voucher_percent),
                            VoucherType.FIXED to stringResource(R.string.voucher_fixed),
                            VoucherType.FREE_DELIVERY to stringResource(R.string.voucher_free_delivery)
                        ).forEach { (value, label) ->
                            androidx.compose.material3.FilterChip(
                                selected = type == value,
                                onClick = { type = value },
                                label = { Text(label) }
                            )
                        }
                    }
                    if (type != VoucherType.FREE_DELIVERY) {
                        OutlinedTextField(
                            value = value,
                            onValueChange = { value = it.filter { c -> c.isDigit() } },
                            label = { Text(if (type == VoucherType.PERCENT) stringResource(R.string.voucher_percent_value) else stringResource(R.string.voucher_amount_value)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = minOrder,
                        onValueChange = { minOrder = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.voucher_min_order)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (code.isBlank()) return@Button
                            viewModel.saveVoucher(
                                Voucher(
                                    code = code.trim(),
                                    type = type,
                                    value = value.toDoubleOrNull() ?: 0.0,
                                    minOrder = minOrder.toDoubleOrNull() ?: 0.0,
                                    active = true
                                )
                            )
                            code = ""; value = ""; minOrder = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.create_voucher))
                    }
                }
            }
        }
        items(vouchers) { voucher ->
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
                        Text(text = voucher.code, fontWeight = FontWeight.Bold)
                        Text(
                            text = when (voucher.type) {
                                VoucherType.PERCENT -> voucher.value.toInt().toString() + "% off"
                                VoucherType.FIXED -> "−" + formatPrice(voucher.value)
                                else -> stringResource(R.string.voucher_free_delivery)
                            } + " • " + stringResource(R.string.voucher_min_order) + ": " + formatPrice(voucher.minOrder),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = voucher.active,
                        onCheckedChange = { viewModel.setVoucherActive(voucher.code, it) }
                    )
                    IconButton(onClick = { onDeleteVoucher(voucher) }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_voucher))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReturnsTab(returns: List<ReturnRequest>, viewModel: StoreViewModel) {
    if (returns.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(stringResource(R.string.no_returns))
        }
        return
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
    ) {
        items(returns) { req ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = req.orderId, fontWeight = FontWeight.Bold)
                        StatusChip(status = req.status, label = returnStatusLabel(req.status))
                    }
                    Text(text = req.reason, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = req.userEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (req.status == ReturnStatus.REQUESTED) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { viewModel.updateReturnStatus(req.id, ReturnStatus.APPROVED) }) {
                                Text(stringResource(R.string.approve))
                            }
                            OutlinedButton(onClick = { viewModel.updateReturnStatus(req.id, ReturnStatus.REJECTED) }) {
                                Text(stringResource(R.string.reject))
                            }
                        }
                    }
                    if (req.status == ReturnStatus.APPROVED) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.updateReturnStatus(req.id, ReturnStatus.REFUNDED) }) {
                            Text(stringResource(R.string.mark_refunded))
                        }
                    }
                }
            }
        }
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
                text = formatPrice(order.totalAmount) + " • " + order.paymentMethod +
                    (if (!order.trxId.isNullOrBlank()) " • Trx: " + order.trxId else "") +
                    (if (!order.courierName.isNullOrBlank()) " • " + order.courierName else ""),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val next = OrderStatus.next(order.status)
                if (next != null) {
                    OutlinedButton(onClick = { onStatusChange(next) }) {
                        Text(statusLabel(next))
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
    var brand by remember { mutableStateOf("") }
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
                brand = p.brand
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
            OutlinedTextField(
                value = brand, onValueChange = { brand = it },
                label = { Text(stringResource(R.string.brand)) },
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
                        brand = brand.trim().ifBlank { "Munsi" },
                        imageUrl = imageUrl.trim().ifBlank { null },
                        stock = stockValue,
                        rating = existing?.rating ?: 4.5f,
                        soldCount = existing?.soldCount ?: 0,
                        createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                        sellerId = existing?.sellerId ?: (viewModel.currentUserEmail ?: "admin"),
                        isFeatured = featured,
                        isFlashSale = existing?.isFlashSale ?: false,
                        flashPrice = existing?.flashPrice
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
