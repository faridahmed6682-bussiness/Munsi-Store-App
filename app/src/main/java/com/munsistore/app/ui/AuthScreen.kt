package com.munsistore.app.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.munsistore.app.R
import com.munsistore.app.data.FirebaseManager

@Composable
fun AuthScreen(viewModel: StoreViewModel, onDone: () -> Unit) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(0) } // 0 = login, 1 = sign up
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val googleEmail = account?.email
            if (googleEmail != null) {
                // Sign in to Firebase with the Google credential when configured.
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                val auth = FirebaseManager.auth()
                if (auth != null) {
                    auth.signInWithCredential(credential).addOnCompleteListener { }
                }
                viewModel.loginWithGoogle(googleEmail, account.displayName ?: "")
                onDone()
            } else {
                message = "Google sign-in failed. Please try again."
            }
        } catch (e: ApiException) {
            message = "Google sign-in failed (${e.statusCode}). Please try again."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.welcome),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))

        TabRow(selectedTabIndex = tab, modifier = Modifier.fillMaxWidth()) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.login)) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.signup)) })
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (tab == 1) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.email)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (message != null) {
            Text(text = message!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }
        Button(
            onClick = {
                if (email.isBlank() || password.isBlank() || (tab == 1 && name.isBlank())) {
                    message = "Please fill in all fields"
                    return@Button
                }
                busy = true
                if (tab == 0) {
                    viewModel.login(email, password) { ok, reason ->
                        busy = false
                        if (ok) onDone() else message = if (reason == "invalid") "Wrong email or password" else "Login failed"
                    }
                } else {
                    viewModel.register(name.trim(), email, password) { ok, reason ->
                        busy = false
                        if (ok) onDone() else message = if (reason == "exists") "This email is already registered" else "Sign up failed"
                    }
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (tab == 0) stringResource(R.string.login) else stringResource(R.string.signup))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.or_continue_with), style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = {
                if (!viewModel.isFirebaseConfigured) {
                    message = "Google sign-in needs Firebase. Add google-services.json (see README)."
                    return@OutlinedButton
                }
                // default_web_client_id is generated by the google-services plugin
                // only when a web OAuth client exists; resolve it dynamically so a
                // missing value can never break the build.
                val webClientIdRes = context.resources.getIdentifier(
                    "default_web_client_id", "string", context.packageName
                )
                if (webClientIdRes == 0) {
                    message = "Google sign-in is not set up in Firebase yet. Enable Google sign-in in the Firebase console."
                    return@OutlinedButton
                }
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(webClientIdRes))
                    .requestEmail()
                    .build()
                val client = GoogleSignIn.getClient(context, gso)
                googleLauncher.launch(client.signInIntent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.google_signin))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.demo_admin_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (!viewModel.isFirebaseConfigured) {
            Text(
                text = stringResource(R.string.firebase_offline_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Keep the Activity reference used (avoids an unused-import lint in some setups).
        @Suppress("UNUSED_VARIABLE")
        val activity = context as? Activity
    }
}
