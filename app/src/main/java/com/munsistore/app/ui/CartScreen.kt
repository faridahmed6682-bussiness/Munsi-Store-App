package com.munsistore.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.munsistore.app.R
import com.munsistore.app.data.PaymentMethod

private fun deliveryFeeFor(subtotal: Double): Double = if (subtotal >= 500) 0.0 else 60.0

@Composable
fun CartScreen(
    viewModel: StoreViewModel,
    onCheckout: () -> Unit,
    onProductClick: (String) -> Unit
) {
    val cart by viewModel.cartWithProducts.collectAsState()
    val subtotal = cart.sumOf { it.product.price * it.quantity }
    val deliveryFee = deliveryFeeFor(subtotal)
    val total = subtotal + deliveryFee

    Column(modifier = Modifier.fillMaxSize()) {
        if (cart.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.empty_cart))
            }
            return@Column
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
        ) {
            items(cart) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProductTile(
                            product = item.product,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (viewModel.language == "bn") item.product.nameBn else item.product.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = formatPrice(item.product.price),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    viewModel.setCartQuantity(item.product.id, item.quantity - 1)
                                }) { Icon(Icons.Filled.Remove, contentDescription = null) }
                                Text(text = item.quantity.toString())
                                IconButton(onClick = {
                                    viewModel.setCartQuantity(item.product.id, item.quantity + 1)
                                }) { Icon(Icons.Filled.Add, contentDescription = null) }
                            }
                        }
                        IconButton(onClick = { viewModel.removeFromCart(item.product.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.remove))
                        }
                    }
                }
            }
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SummaryRow(stringResource(R.string.subtotal), formatPrice(subtotal))
                SummaryRow(stringResource(R.string.delivery_fee), if (deliveryFee == 0.0) "FREE" else formatPrice(deliveryFee))
                Text(
                    text = stringResource(R.string.free_delivery),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                SummaryRow(stringResource(R.string.total), formatPrice(total), bold = true)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onCheckout,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.checkout))
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    viewModel: StoreViewModel,
    onBack: () -> Unit,
    onOrderPlaced: (String) -> Unit
) {
    val cart by viewModel.cartWithProducts.collectAsState()
    val subtotal = cart.sumOf { it.product.price * it.quantity }
    val deliveryFee = deliveryFeeFor(subtotal)
    val total = subtotal + deliveryFee

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var payment by remember { mutableStateOf(PaymentMethod.COD) }
    var error by remember { mutableStateOf<String?>(null) }
    var placing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.checkout)) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(stringResource(R.string.phone)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text(stringResource(R.string.address)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.payment_method),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            listOf(
                PaymentMethod.COD to stringResource(R.string.cod),
                PaymentMethod.BKASH to stringResource(R.string.bkash),
                PaymentMethod.NAGAD to stringResource(R.string.nagad)
            ).forEach { (value, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = payment == value,
                            onClick = { payment = value },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = payment == value, onClick = { payment = value })
                    Text(text = label, modifier = Modifier.padding(start = 8.dp))
                }
            }
            val noteRes = when (payment) {
                PaymentMethod.BKASH -> R.string.payment_note_bkash
                PaymentMethod.NAGAD -> R.string.payment_note_nagad
                else -> R.string.payment_note_cod
            }
            Text(
                text = stringResource(noteRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SummaryRow(stringResource(R.string.subtotal), formatPrice(subtotal))
                    SummaryRow(stringResource(R.string.delivery_fee), if (deliveryFee == 0.0) "FREE" else formatPrice(deliveryFee))
                    Spacer(modifier = Modifier.height(8.dp))
                    SummaryRow(stringResource(R.string.total), formatPrice(total), bold = true)
                }
            }
            if (error != null) {
                Text(text = error!!, color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = {
                    if (name.isBlank() || phone.isBlank() || address.isBlank()) {
                        error = "Please fill in all fields"
                        return@Button
                    }
                    placing = true
                    viewModel.placeOrder(name.trim(), phone.trim(), address.trim(), payment) { orderId ->
                        placing = false
                        if (orderId != null) onOrderPlaced(orderId) else error = "Order failed. Please try again."
                    }
                },
                enabled = !placing && cart.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.place_order) + " • " + formatPrice(total))
            }
        }
    }
}
