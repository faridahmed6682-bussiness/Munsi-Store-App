package com.munsistore.app.ui

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.munsistore.app.R

@Composable
fun ProfileScreen(
    viewModel: StoreViewModel,
    onLoginClick: () -> Unit,
    onAdminClick: () -> Unit,
    onOrdersClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.profile),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (viewModel.isLoggedIn) {
                (viewModel.currentUserEmail ?: stringResource(R.string.guest))
            } else {
                stringResource(R.string.guest)
            },
            style = MaterialTheme.typography.bodyLarge
        )
        if (viewModel.isLoggedIn) {
            OutlinedButton(onClick = onOrdersClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.my_orders))
            }
            if (viewModel.isAdmin) {
                Button(onClick = onAdminClick, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.admin_panel))
                }
            }
            OutlinedButton(
                onClick = { viewModel.logout() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.logout))
            }
        } else {
            Button(onClick = onLoginClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.login))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.language),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    viewModel.setLanguage("bn")
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("bn"))
                    (context as? Activity)?.recreate()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("বাংলা")
            }
            OutlinedButton(
                onClick = {
                    viewModel.setLanguage("en")
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                    (context as? Activity)?.recreate()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("English")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.about_shop),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(text = stringResource(R.string.shop_tagline), style = MaterialTheme.typography.bodyMedium)
        Text(
            text = if (viewModel.isFirebaseConfigured) stringResource(R.string.firebase_on_note)
            else stringResource(R.string.firebase_offline_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.demo_admin_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Munsi Store • Rangpur, Bangladesh",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
