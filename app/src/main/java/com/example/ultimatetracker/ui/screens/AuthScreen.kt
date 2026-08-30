package com.example.ultimatetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ultimatetracker.R
import com.example.ultimatetracker.data.repository.AccountResult
import com.example.ultimatetracker.viewmodel.AccountViewModel

@Composable
fun AuthScreen(viewModel: AccountViewModel, onContinueAsGuest: (() -> Unit)? = null) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var register by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall)
        Text(stringResource(R.string.account_welcome), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Row {
            TextButton(onClick = { register = false; viewModel.clearError() }) { Text(stringResource(R.string.sign_in)) }
            TextButton(onClick = { register = true; viewModel.clearError() }) { Text(stringResource(R.string.register)) }
        }
        if (register) OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.display_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.email)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password)) },
            supportingText = { if (register) Text(stringResource(R.string.password_hint)) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let { Text(accountError(it), color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { if (register) viewModel.register(email, password, name) else viewModel.login(email, password) },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(if (register) R.string.create_account else R.string.sign_in)) }
        OutlinedButton(
            onClick = {
                viewModel.continueAsGuest()
                onContinueAsGuest?.invoke()
            },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.continue_as_guest)) }
        if (state.busy) CircularProgressIndicator(Modifier.padding(8.dp))
        Text(stringResource(R.string.local_account_notice), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun accountError(result: AccountResult): String = stringResource(when (result) {
    AccountResult.InvalidEmail -> R.string.error_invalid_email
    AccountResult.WeakPassword -> R.string.error_weak_password
    AccountResult.EmailAlreadyUsed -> R.string.error_email_used
    AccountResult.InvalidCredentials -> R.string.error_invalid_credentials
    is AccountResult.Locked -> R.string.error_account_locked
    AccountResult.Conflict -> R.string.error_conflict
    AccountResult.NotAllowed -> R.string.error_not_allowed
    AccountResult.NotFound -> R.string.error_not_found
    AccountResult.Success -> R.string.done
})
