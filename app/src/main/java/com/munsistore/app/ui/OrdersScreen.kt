package com.munsistore.app.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
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
import com.munsistore.app.data.Order
import com.munsistore.app.data.OrderItem
import com.munsistore.app.data.OrderStatus
import com.munsistore.app.data.ReturnRequest
import com.munsistore.app.data.ReturnStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

fun returnStatusLabel(status: String): String {
    return when (status) {
        ReturnStatus.REQUESTED -> "Requested"
        ReturnStatus.APPROVED -> "Approved"
        ReturnStatus.REJECTED -> "Rejected"
        ReturnStatus.REFUNDED -> "Refunded"
        else -> status
    }
}
fun statusLabel(status: String): String {
    return when (status) {
        OrderStatus.PLACED -> "Placed"
        OrderStatus.CONFIRMED -> "Confirmed"
        OrderStatus.PACKED -> "Packed"
        OrderStatus.SHIPPED -> "Shipped"
        OrderStatus.OUT_FOR_DELIVERY -> "Out for delivery"
        OrderStatus.DELIVERED -> "Delivered"
        OrderStatus.CANCELLED -> "Cancelled"
        else -> status
    }
}

fun formatDateTime(time: Long): String {
    return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(time))
}

@Composable
fun OrdersScreen(
    viewModel: StoreViewModel,
    onOrderClick: (String) -> Unit,
    onLoginClick: () -> Unit
) {
    val orders by viewModel.myOrders.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.my_orders),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
        if (!viewModel.isLoggedIn) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.login_required))
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onLoginClick) { Text(stringResource(R.string.login)) }
            }
            return@Column
        }
        if (orders.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.no_orders))
            }
            return@Column
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
        ) {
            items(orders) { order ->
                OrderRow(order = order, onClick = { onOrderClick(order.id) })
            }
        }
    }
}

@Composable
fun OrderRow(order: Order, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.id,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                StatusChip(status = order.status, label = statusLabel(order.status))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatDateTime(order.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = order.paymentMethod, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = formatPrice(order.totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    viewModel: StoreViewModel,
    orderId: String,
    onBack: () -> Unit
) {
    var order by remember { mutableStateOf<Order?>(null) }
    var items by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    var existingReturn by remember { mutableStateOf<ReturnRequest?>(null) }
    var showReturnDialog by remember { mutableStateOf(false) }
    var returnReason by remember { mutableStateOf("") }
    var returnMessage by remember { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val returnRequestedText = stringResource(R.string.return_requested)
    val returnAlreadyText = stringResource(R.string.return_already_requested)

    LaunchedEffect(orderId) {
        order = viewModel.getOrder(orderId)
        items = viewModel.getOrderItems(orderId)
        existingReturn = viewModel.getReturnForOrder(orderId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.track_order)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        val o = order
        if (o == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.no_orders))
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = o.id,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                StatusChip(status = o.status, label = statusLabel(o.status))
            }
            Text(text = formatDateTime(o.createdAt), style = MaterialTheme.typography.bodySmall)
            Text(
                text = stringResource(R.string.order_status),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            // Status timeline (Placed → Confirmed → Packed → Shipped → Out for delivery → Delivered)
            val steps = OrderStatus.timeline
            val currentIndex = steps.indexOf(o.status)
            steps.forEachIndexed { index, step ->
                val done = o.status == OrderStatus.CANCELLED || index <= currentIndex
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        text = statusLabel(step),
                        modifier = Modifier.padding(start = 8.dp),
                        color = if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontWeight = if (done) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
            if (!o.courierName.isNullOrBlank() && o.status != OrderStatus.CANCELLED) {
                Text(
                    text = stringResource(R.string.courier) + ": " + o.courierName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (o.voucherCode != null) {
                Text(
                    text = stringResource(R.string.voucher) + ": " + o.voucherCode +
                        " (−" + formatPrice(o.discountAmount) + ")",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (o.coinsUsed > 0) {
                Text(
                    text = stringResource(R.string.coins_used) + ": " + o.coinsUsed,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = stringResource(R.string.products),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "${item.productName} × ${item.quantity}")
                    Text(text = formatPrice(item.price * item.quantity))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = stringResource(R.string.total), fontWeight = FontWeight.Bold)
                Text(
                    text = formatPrice(o.totalAmount),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = stringResource(R.string.payment_method) + ": " + o.paymentMethod,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.customer) + ": " + o.customerName + " • " + o.customerPhone,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.address) + ": " + o.customerAddress,
                style = MaterialTheme.typography.bodyMedium
            )
            // Return / refund (Daraz-style) — only for delivered orders.
            val ret = existingReturn
            if (ret != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.return_request) + ": " + returnStatusLabel(ret.status),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(text = ret.reason, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else if (o.status == OrderStatus.DELIVERED) {
                OutlinedButton(
                    onClick = { showReturnDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.request_return))
                }
            }
            if (returnMessage != null) {
                Text(text = returnMessage!!, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (showReturnDialog) {
        AlertDialog(
            onDismissRequest = { showReturnDialog = false },
            title = { Text(stringResource(R.string.request_return)) },
            text = {
                Column {
                    Text(stringResource(R.string.return_reason_hint))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = returnReason,
                        onValueChange = { returnReason = it },
                        label = { Text(stringResource(R.string.return_reason)) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (returnReason.isBlank()) return@TextButton
                    viewModel.requestReturn(orderId, returnReason) { ok ->
                        returnMessage = if (ok) returnRequestedText else returnAlreadyText
                        if (ok) {
                            scope.launch { existingReturn = viewModel.getReturnForOrder(orderId) }
                        }
                    }
                    showReturnDialog = false
                }) { Text(stringResource(R.string.submit)) }
            },
            dismissButton = {
                TextButton(onClick = { showReturnDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
