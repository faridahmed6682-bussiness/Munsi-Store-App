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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.munsistore.app.data.OrderStatus

/** Seller registration + seller dashboard (basic multi-vendor). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerScreen(
    viewModel: StoreViewModel,
    onBack: () -> Unit,
    onAddProduct: () -> Unit,
    onEditProduct: (String) -> Unit
) {
    val myProducts by viewModel.sellerProducts.collectAsState()
    val myOrders by viewModel.sellerOrders.collectAsState()
    var shopName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.seller_dashboard)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        if (!viewModel.isLoggedIn) {
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
            return@Scaffold
        }
        if (!viewModel.isSeller) {
            // Registration form
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Filled.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = stringResource(R.string.become_seller),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.become_seller_info),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text(stringResource(R.string.shop_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (message != null) {
                    Text(text = message!!, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = {
                        if (shopName.isBlank()) {
                            message = "Please enter your shop name"
                            return@Button
                        }
                        viewModel.registerSeller(shopName) { ok ->
                            if (!ok) message = "Registration failed. Please try again."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.register_as_seller))
                }
            }
            return@Scaffold
        }

        // Seller dashboard: own products + own orders
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = viewModel.myShopName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.my_products) + ": " + myProducts.size +
                    " • " + stringResource(R.string.my_orders) + ": " + myOrders.size,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onAddProduct, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.add_product))
            }
            Text(
                text = stringResource(R.string.my_products),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(myProducts) { product ->
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
                                Text(text = product.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = formatPrice(product.price) + " • " +
                                        stringResource(R.string.stock) + ": " + product.stock +
                                        " • " + stringResource(R.string.sold_count) + ": " + product.soldCount,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            OutlinedButton(onClick = { onEditProduct(product.id) }) {
                                Text(stringResource(R.string.edit_product))
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.my_orders),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (myOrders.isEmpty()) {
                    item { Text(stringResource(R.string.no_orders)) }
                }
                items(myOrders) { order ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = order.id, fontWeight = FontWeight.Bold)
                                StatusChip(status = order.status, label = statusLabel(order.status))
                            }
                            Text(
                                text = order.customerName + " • " + formatPrice(order.totalAmount),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            val next = OrderStatus.next(order.status)
                            if (next != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(onClick = { viewModel.updateOrderStatus(order.id, next) }) {
                                    Text(statusLabel(next))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
