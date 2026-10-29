package com.mokamusic.player.playback

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.mokamusic.player.network.PrivateVpnNetworkGate
import com.mokamusic.player.network.PrivateVpnServerRules
import okhttp3.OkHttpClient
import java.io.IOException
import java.net.Proxy
import java.util.concurrent.TimeUnit

/**
 * Keeps the existing Media3 HTTP/HTTPS engine for every normal stream.
 * Only the specifically allowed private Navidrome HTTP endpoint uses a VPN-
 * bound socket factory. A disconnect makes those sockets fail, not fall back.
 */
@OptIn(UnstableApi::class)
internal class MokaRoutingHttpDataSourceFactory(context: Context) : DataSource.Factory {
    private val appContext = context.applicationContext
    private val standard = DefaultHttpDataSource.Factory()
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(45_000)
        .setAllowCrossProtocolRedirects(false)
        .setUserAgent("MokaMusicPlayer/4.0")

    override fun createDataSource(): DataSource = SelectedDataSource(appContext, standard)

    private class SelectedDataSource(
        private val context: Context,
        private val normalFactory: DataSource.Factory
    ) : DataSource {
        private var active: DataSource? = null
        private val listeners = mutableListOf<TransferListener>()

        override fun addTransferListener(transferListener: TransferListener) {
            listeners.add(transferListener)
            active?.addTransferListener(transferListener)
        }

        override fun open(dataSpec: DataSpec): Long {
            check(active == null) { "HTTP data source already open" }
            val uri = dataSpec.uri
            val next = if (uri.scheme.equals("http", ignoreCase = true)) {
                if (!PrivateVpnServerRules.isPrivateWireGuard(uri)) {
                    throw IOException("Insecure HTTP stream is not an approved WireGuard endpoint")
                }
                val vpn = PrivateVpnNetworkGate.requireVpnNetwork(context)
                val client = OkHttpClient.Builder()
                    .socketFactory(vpn.socketFactory)
                    .proxy(Proxy.NO_PROXY)
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .retryOnConnectionFailure(false)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(45, TimeUnit.SECONDS)
                    .build()
                OkHttpDataSource.Factory(client)
                    .setUserAgent("MokaMusicPlayer/4.0")
                    .createDataSource()
            } else {
                normalFactory.createDataSource()
            }
            listeners.forEach(next::addTransferListener)
            active = next
            try {
                return next.open(dataSpec)
            } catch (failure: Exception) {
                active = null
                runCatching { next.close() }
                throw failure
            }
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            (active ?: throw IOException("HTTP data source is not open"))
                .read(buffer, offset, length)

        override fun getUri(): Uri? = active?.uri

        override fun getResponseHeaders(): Map<String, List<String>> =
            active?.responseHeaders ?: emptyMap()

        override fun close() {
            val old = active
            active = null
            old?.close()
        }
    }
}
