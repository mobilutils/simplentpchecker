package io.github.mobilutils.simplentpchecker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mobilutils.simplentpchecker.ui.theme.SimpleNTPCheckerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleNTPCheckerTheme {
                SimpleNtpCheckerApp()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Root composable
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleNtpCheckerApp(viewModel: SimpleNtpViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.NetworkCheck,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("NTP Reachability Checker", fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Server Address Input ──────────────────────────────────────
            OutlinedTextField(
                value = uiState.serverAddress,
                onValueChange = viewModel::onServerAddressChange,
                label = { Text("NTP Server Address") },
                placeholder = { Text("e.g. pool.ntp.org") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = ImeAction.Go.let { KeyboardType.Uri },
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        focusManager.clearFocus()
                        viewModel.checkReachability()
                    },
                ),
                isError = uiState.result is NtpResult.Error ||
                        uiState.result is NtpResult.DnsFailure,
                supportingText = {
                    when (val r = uiState.result) {
                        is NtpResult.DnsFailure ->
                            Text("Cannot resolve \"${r.host}\"", color = MaterialTheme.colorScheme.error)
                        is NtpResult.Error ->
                            Text(r.message, color = MaterialTheme.colorScheme.error)
                        else -> {}
                    }
                },
            )

            // ── Check Button ─────────────────────────────────────────────
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.checkReachability()
                },
                enabled = !uiState.isLoading && uiState.serverAddress.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Querying…")
                } else {
                    Text("Check Reachability", fontWeight = FontWeight.Medium)
                }
            }

            // ── Results Card ─────────────────────────────────────────────
            AnimatedVisibility(
                visible = uiState.result != null,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 3 },
                exit  = fadeOut(tween(200)),
            ) {
                uiState.result?.let { result ->
                    ResultCard(result)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Result card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ResultCard(result: NtpResult) {
    val isSuccess = result is NtpResult.Success
    val cardColor = if (isSuccess)
        MaterialTheme.colorScheme.secondaryContainer
    else
        MaterialTheme.colorScheme.errorContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header row
            StatusHeader(result)

            if (result is NtpResult.Success) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))

                MetricRow(label = "Server Time",       value = result.serverTime)
                Spacer(Modifier.height(10.dp))
                MetricRow(label = "Clock Offset",      value = "${result.offsetMs} ms")
                Spacer(Modifier.height(10.dp))
                MetricRow(label = "Round-Trip Delay",  value = "${result.delayMs} ms")
            }

            if (result is NtpResult.NoNetwork) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Please check your internet connection and try again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            if (result is NtpResult.Timeout) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "The server did not respond within the timeout window (5 s). " +
                           "It may be offline, firewalled, or unreachable from this network.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun StatusHeader(result: NtpResult) {
    val (icon, label, tint) = when (result) {
        is NtpResult.Success    ->
            Triple(Icons.Filled.CheckCircle, "Reachable",   MaterialTheme.colorScheme.secondary)
        is NtpResult.NoNetwork  ->
            Triple(Icons.Filled.WifiOff,     "No Network",  MaterialTheme.colorScheme.error)
        is NtpResult.Timeout    ->
            Triple(Icons.Filled.Error,       "Unreachable – Timeout", MaterialTheme.colorScheme.error)
        is NtpResult.DnsFailure ->
            Triple(Icons.Filled.Error,       "Unreachable – DNS Failure", MaterialTheme.colorScheme.error)
        is NtpResult.Error      ->
            Triple(Icons.Filled.Error,       "Error",        MaterialTheme.colorScheme.error)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = "Status",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1.5f),
        )
    }
}
