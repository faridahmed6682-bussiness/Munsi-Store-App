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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.munsistore.app.R
import com.munsistore.app.data.Address
import com.munsistore.app.data.CheckoutInput
import com.munsistore.app.data.DeliveryZone
import com.munsistore.app.data.PaymentMethod
import com.munsistore.app.data.ShopConfig
import com.munsistore.app.data.Voucher
import com.munsistore.app.data.discountFor
import com.munsistore.app.data.effectivePrice
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun CartScreen(
    viewModel: StoreViewModel,
    onCheckout: () -> Unit,
    onProductClick: (String) -> Unit
) {
    val cart by viewModel.cartWithProducts.collectAsState()
    val flashOn = viewModel.isFlashActive()
    val subtotal = cart.sumOf { effectivePrice(it.product, flashOn) * it.quantity }
    val deliveryFee = if (subtotal >= ShopConfig.FREE_DELIVERY_THRESHOLD) 0.0 else DeliveryZone.fee(DeliveryZone.DHAKA_CITY)
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
                                text = formatPrice(effectivePrice(item.product, flashOn)),
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
    val flashOn = viewModel.isFlashActive()
    val subtotal = cart.sumOf { effectivePrice(it.product, flashOn) * it.quantity }
    val addresses by viewModel.myAddresses.collectAsState()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedAddress by remember { mutableStateOf<Address?>(null) }
    var showAddressPicker by remember { mutableStateOf(false) }
    var payment by remember { mutableStateOf(PaymentMethod.COD) }
    var trxId by remember { mutableStateOf("") }
    var voucherCode by remember { mutableStateOf("") }
    var appliedVoucher by remember { mutableStateOf<Voucher?>(null) }
    var voucherMessage by remember { mutableStateOf<String?>(null) }
    var useCoins by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var placing by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val voucherAppliedText = stringResource(R.string.voucher_applied)
    val voucherInvalidText = stringResource(R.string.voucher_invalid)

    LaunchedEffect(addresses) {
        if (selectedAddress == null) {
            selectedAddress = addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()
        }
    }

    val zoneFee = selectedAddress?.let { DeliveryZone.fee(it.zone) } ?: DeliveryZone.fee(DeliveryZone.DHAKA_CITY)
    val deliveryFee = if (subtotal >= ShopConfig.FREE_DELIVERY_THRESHOLD) 0.0 else zoneFee
    val voucherDiscount = appliedVoucher?.let { discountFor(it, subtotal, deliveryFee) } ?: 0.0
    val maxCoinsUsable = minOf(viewModel.myCoins, (subtotal * ShopConfig.MAX_COIN_REDEEM_FRACTION).toInt())
    val coinsUsed = if (useCoins) maxCoinsUsable else 0
    val coinsValue = coinsUsed * ShopConfig.COIN_VALUE_TAKA
    val total = (subtotal - voucherDiscount - coinsValue + deliveryFee).coerceAtLeast(0.0)
    val isMobileBanking = payment == PaymentMethod.BKASH || payment == PaymentMethod.NAGAD || payment == PaymentMethod.ROCKET

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

            // Delivery address (address book)
            Text(
                text = stringResource(R.string.delivery_address),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            val addr = selectedAddress
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (addr != null) {
                        Text(text = addr.label, fontWeight = FontWeight.SemiBold)
                        Text(text = addr.fullAddress, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = viewModel.zoneName(addr.zone) + " • " + addr.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(stringResource(R.string.no_address_yet))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showAddressPicker = true }) {
                        Text(stringResource(R.string.choose_address))
                    }
                }
            }

            // Voucher / promo code
            Text(
                text = stringResource(R.string.voucher),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            val applied = appliedVoucher
            if (applied != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = applied.code + " (−" + formatPrice(voucherDiscount) + ")",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = { appliedVoucher = null; voucherCode = "" }) {
                        Text(stringResource(R.string.remove))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = voucherCode,
                        onValueChange = { voucherCode = it.uppercase() },
                        label = { Text(stringResource(R.string.voucher_code_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = {
                        if (voucherCode.isBlank()) return@Button
                        scope.launch {
                            val (voucher, discount) = viewModel.validateVoucher(
                                voucherCode.trim(), subtotal, deliveryFee
                            )
                            appliedVoucher = voucher
                            voucherMessage = if (voucher != null) {
                                voucherAppliedText + " (−" + formatPrice(discount) + ")"
                            } else {
                                voucherInvalidText
                            }
                        }
                    }) {
                        Text(stringResource(R.string.apply))
                    }
                }
                if (voucherMessage != null) {
                    Text(
                        text = voucherMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (appliedVoucher != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }

            // Coins
            if (viewModel.isLoggedIn && viewModel.myCoins > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.use_coins),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.coins_balance) + ": " + viewModel.myCoins +
                                " • " + stringResource(R.string.coins_usable) + ": " + maxCoinsUsable,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(checked = useCoins, onCheckedChange = { useCoins = it })
                }
            }

            // Payment methods
            Text(
                text = stringResource(R.string.payment_method),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            listOf(
                PaymentMethod.COD to stringResource(R.string.cod),
                PaymentMethod.BKASH to stringResource(R.string.bkash),
                PaymentMethod.NAGAD to stringResource(R.string.nagad),
                PaymentMethod.ROCKET to stringResource(R.string.rocket)
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
            // SSLCommerz — gateway-ready placeholder (not live yet).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = false, onClick = null, enabled = false)
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = stringResource(R.string.sslcommerz),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.sslcommerz_soon),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isMobileBanking) {
                val number = when (payment) {
                    PaymentMethod.BKASH -> ShopConfig.BKASH_NUMBER
                    PaymentMethod.NAGAD -> ShopConfig.NAGAD_NUMBER
                    else -> ShopConfig.ROCKET_NUMBER
                }
                Text(
                    text = stringResource(R.string.send_money_to) + " " + number,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = trxId,
                    onValueChange = { trxId = it },
                    label = { Text(stringResource(R.string.trx_id_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.payment_note_mobile),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = stringResource(R.string.payment_note_cod),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SummaryRow(stringResource(R.string.subtotal), formatPrice(subtotal))
                    if (voucherDiscount > 0) {
                        SummaryRow(stringResource(R.string.voucher_discount), "−" + formatPrice(voucherDiscount))
                    }
                    if (coinsValue > 0) {
                        SummaryRow(stringResource(R.string.coins_discount), "−" + formatPrice(coinsValue))
                    }
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
                    val chosen = selectedAddress
                    if (name.isBlank() || phone.isBlank() || chosen == null) {
                        error = "Please fill in all fields"
                        return@Button
                    }
                    if (isMobileBanking && trxId.isBlank()) {
                        error = "Please enter the transaction ID"
                        return@Button
                    }
                    placing = true
                    viewModel.placeOrder(
                        CheckoutInput(
                            name = name.trim(),
                            phone = phone.trim(),
                            address = chosen,
                            paymentMethod = payment,
                            trxId = if (isMobileBanking) trxId.trim() else null,
                            voucher = appliedVoucher,
                            coinsUsed = coinsUsed
                        )
                    ) { orderId ->
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

    if (showAddressPicker) {
        AddressPickerDialog(
            viewModel = viewModel,
            current = selectedAddress,
            onSelect = { selectedAddress = it; showAddressPicker = false },
            onDismiss = { showAddressPicker = false }
        )
    }
}

/** Address picker + quick add form used at checkout. */
@Composable
private fun AddressPickerDialog(
    viewModel: StoreViewModel,
    current: Address?,
    onSelect: (Address) -> Unit,
    onDismiss: () -> Unit
) {
    val addresses by viewModel.myAddresses.collectAsState()
    var showAddForm by remember { mutableStateOf(false) }
    var label by remember { mutableStateOf("") }
    var fullAddress by remember { mutableStateOf("") }
    var zone by remember { mutableStateOf(DeliveryZone.DHAKA_CITY) }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_address)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (addresses.isEmpty() && !showAddForm) {
                    Text(stringResource(R.string.no_address_yet))
                }
                addresses.forEach { addr ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = current?.id == addr.id,
                                onClick = { onSelect(addr) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = current?.id == addr.id, onClick = { onSelect(addr) })
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(text = addr.label, fontWeight = FontWeight.SemiBold)
                            Text(text = addr.fullAddress, style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = viewModel.zoneName(addr.zone),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (showAddForm) {
                    OutlinedTextField(
                        value = label, onValueChange = { label = it },
                        label = { Text(stringResource(R.string.address_label)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = fullAddress, onValueChange = { fullAddress = it },
                        label = { Text(stringResource(R.string.address)) },
                        minLines = 2, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it },
                        label = { Text(stringResource(R.string.phone)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Text(text = stringResource(R.string.delivery_zone), fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DeliveryZone.all.forEach { z ->
                            FilterChip(
                                selected = zone == z,
                                onClick = { zone = z },
                                label = { Text(viewModel.zoneName(z)) }
                            )
                        }
                    }
                    Button(onClick = {
                        if (label.isBlank() || fullAddress.isBlank() || phone.isBlank()) return@Button
                        val email = viewModel.currentUserEmail ?: return@Button
                        val addr = Address(
                            id = "addr-" + UUID.randomUUID().toString().take(8),
                            userEmail = email,
                            label = label.trim(),
                            fullAddress = fullAddress.trim(),
                            zone = zone,
                            phone = phone.trim(),
                            isDefault = addresses.isEmpty()
                        )
                        viewModel.saveAddress(addr)
                        onSelect(addr)
                    }) {
                        Text(stringResource(R.string.save_address))
                    }
                } else {
                    TextButton(onClick = { showAddForm = true }) {
                        Text(stringResource(R.string.add_new_address))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
