package com.mokamusic.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mokamusic.player.network.NetworkStreamQuality

/** Credentials belong in Settings; Library → Network is browse/play only. */
@Composable
internal fun NetworkAccountSettings(state: NetworkLibraryState) {
    LaunchedEffect(state) { state.onScreenOpened() }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Navidrome account", style = MaterialTheme.typography.titleLarge)
            Text(
                "Configure HTTPS/Tailscale or an opt-in private Wonhee WireGuard server. " +
                    "Browse music using Library → Network.",
                style = MaterialTheme.typography.bodySmall
            )
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.status?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedTextField(
                value = state.server, onValueChange = state::updateServer,
                enabled = state.client == null, singleLine = true,
                label = { Text("Navidrome server URL") },
                placeholder = { Text("https://… or http://10.77.0.1:4533") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.username, onValueChange = state::updateUsername,
                enabled = state.client == null, singleLine = true,
                label = { Text("Navidrome username") },
                modifier = Modifier.fillMaxWidth()
            )
            if (state.client == null) {
                OutlinedTextField(
                    value = state.password, onValueChange = { state.password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
            Text("Streaming quality", style = MaterialTheme.typography.titleMedium)
            NetworkStreamQuality.entries.forEach { option ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = state.streamQuality == option,
                        onClick = { state.updateStreamQuality(option) }
                    )
                    Text(option.label)
                }
            }
            Text(
                "Original requests the source file without transcoding or a bitrate limit. " +
                    "Lossless files stay lossless; original MP3 files remain MP3. " +
                    "WMA uses an MP3 compatibility stream. Restart playback after changing quality.",
                style = MaterialTheme.typography.bodySmall
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = state.rememberLogin,
                    onCheckedChange = state::updateRememberLogin
                )
                Text("Remember login securely on this device")
            }
            if (state.client == null) {
                Button(
                    enabled = !state.busy && state.server.isNotBlank() &&
                        state.username.isNotBlank() &&
                        (state.password.isNotBlank() || state.savedLoginAvailable),
                    onClick = state::connect,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.savedLoginAvailable && state.password.isBlank())
                        "Reconnect to saved server" else "Connect")
                }
            } else {
                Text(
                    "Connected. Your session stays active when you leave Settings.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedButton(
                    onClick = state::testStream,
                    enabled = !state.streamCheckBusy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (state.streamCheckBusy) "Testing audio…" else "Test Navidrome audio stream") }
                state.streamCheckStatus?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(
                    onClick = state::disconnect,
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Disconnect (keep saved login)") }
            }
            OutlinedButton(
                onClick = state::forgetServer,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Forget server and saved login") }
            Text(
                "Wonhee mode: connect the phone to Wonhee VPN first, then enter " +
                    "http://10.77.0.1:4533. This single private HTTP address is allowed " +
                    "only over an Android VPN route; it never falls back to Wi-Fi/cellular. " +
                    "WireGuard encrypts the tunnel but this URL does not use HTTPS. " +
                    "Existing Tailscale HTTPS connections remain supported. " +
                    "Do not expose Navidrome through public port forwarding.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
