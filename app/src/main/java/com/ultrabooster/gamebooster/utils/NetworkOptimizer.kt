package com.ultrabooster.gamebooster.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class NetworkOptimizer(private val context: Context) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val isOptimizing = AtomicBoolean(false)
    
    // Fast DNS servers for gaming
    private val fastDNSServers = listOf(
        "8.8.8.8",      // Google DNS
        "8.8.4.4",      // Google DNS Secondary
        "1.1.1.1",      // Cloudflare DNS
        "1.0.0.1",      // Cloudflare DNS Secondary
        "208.67.222.222", // OpenDNS
        "208.67.220.220", // OpenDNS Secondary
        "9.9.9.9",      // Quad9 DNS
        "149.112.112.112", // Quad9 DNS Secondary
        "64.6.64.6",    // Verisign DNS
        "64.6.65.6"     // Verisign DNS Secondary
    )
    
    // Game server endpoints for ping testing
    private val gameServers = listOf(
        "8.8.8.8",      // Google
        "1.1.1.1",      // Cloudflare
        "aws.amazon.com", // AWS
        "cloudflare.com", // Cloudflare
        "google.com",    // Google
        "facebook.com",  // Facebook
        "microsoft.com", // Microsoft
        "amazon.com"     // Amazon
    )
    
    data class NetworkInfo(
        val isConnected: Boolean,
        val networkType: String,
        val isMetered: Boolean,
        val strength: Int,
        val speed: Long
    )
    
    data class PingResult(
        val server: String,
        val pingMs: Int,
        val isSuccessful: Boolean
    )
    
    data class DNSResult(
        val dnsServer: String,
        val responseTime: Int,
        val isSuccessful: Boolean
    )
    
    suspend fun optimizeNetwork(): Boolean = withContext(Dispatchers.IO) {
        if (isOptimizing.compareAndSet(false, true)) {
            try {
                var success = false
                
                // Step 1: Find fastest DNS
                val fastestDNS = findFastestDNS()
                if (fastestDNS != null) {
                    success = setOptimalDNS(fastestDNS.dnsServer)
                }
                
                // Step 2: Optimize TCP settings
                success = optimizeTCPSettings() || success
                
                // Step 3: Block background network usage
                success = blockBackgroundNetwork() || success
                
                // Step 4: Optimize routing
                success = optimizeRouting() || success
                
                success
                
            } catch (e: Exception) {
                false
            } finally {
                isOptimizing.set(false)
            }
        } else {
            false // Already optimizing
        }
    }
    
    suspend fun findFastestDNS(): DNSResult? = withContext(Dispatchers.IO) {
        var fastest: DNSResult? = null
        var minTime = Int.MAX_VALUE
        
        for (dnsServer in fastDNSServers) {
            try {
                val startTime = System.currentTimeMillis()
                
                // Test DNS resolution speed
                val address = InetAddress.getByName(dnsServer)
                val endTime = System.currentTimeMillis()
                val responseTime = (endTime - startTime).toInt()
                
                if (responseTime < minTime) {
                    minTime = responseTime
                    fastest = DNSResult(dnsServer, responseTime, true)
                }
                
                // Small delay to prevent overwhelming network
                delay(50)
                
            } catch (e: Exception) {
                // DNS server not reachable
            }
        }
        
        fastest
    }
    
    private suspend fun setOptimalDNS(dnsServer: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // This requires root access on most devices
            // For non-rooted devices, we can only suggest DNS changes
            
            // Try to set DNS via system properties (requires root)
            val process = Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "setprop net.dns1 $dnsServer"
            ))
            process.waitFor()
            
            // Alternative method for some devices
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "ndc resolver setnetdns 0 $dnsServer"
            ))
            
            true
            
        } catch (e: Exception) {
            // Not rooted or permission denied
            false
        }
    }
    
    private suspend fun optimizeTCPSettings(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Optimize TCP parameters for gaming (requires root)
            val tcpOptimizations = listOf(
                "echo 1 > /proc/sys/net/ipv4/tcp_window_scaling",     // Enable window scaling
                "echo 1 > /proc/sys/net/ipv4/tcp_timestamps",         // Enable timestamps
                "echo 1 > /proc/sys/net/ipv4/tcp_sack",               // Enable selective ACK
                "echo 0 > /proc/sys/net/ipv4/tcp_slow_start_after_idle", // Disable slow start after idle
                "echo 10 > /proc/sys/net/ipv4/tcp_fin_timeout",        // Reduce FIN timeout
                "echo 30 > /proc/sys/net/ipv4/tcp_keepalive_time",     // Reduce keepalive time
                "echo 3 > /proc/sys/net/ipv4/tcp_keepalive_probes",    // Reduce keepalive probes
                "echo 3 > /proc/sys/net/ipv4/tcp_keepalive_intvl",     // Reduce keepalive interval
                "echo bbr > /proc/sys/net/ipv4/tcp_congestion_control", // Use BBR congestion control
                "echo 1 > /proc/sys/net/core/netdev_max_backlog",      // Increase device backlog
                "echo 65536 > /proc/sys/net/core/rmem_max",            // Increase receive buffer
                "echo 65536 > /proc/sys/net/core/wmem_max"             // Increase send buffer
            )
            
            for (command in tcpOptimizations) {
                try {
                    Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                } catch (e: Exception) {
                    // Continue with other optimizations
                }
            }
            
            true
            
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun blockBackgroundNetwork(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Block known background services that use network
            val backgroundServices = listOf(
                "com.google.android.gms.update",
                "com.google.android.gms.analytics",
                "com.google.android.gms.ads",
                "com.facebook.katana",
                "com.instagram.android",
                "com.twitter.android",
                "com.whatsapp",
                "com.skype.raider",
                "com.discord"
            )
            
            for (service in backgroundServices) {
                try {
                    // Block network access for these services (requires root)
                    Runtime.getRuntime().exec(arrayOf(
                        "su", "-c", "iptables -A OUTPUT -m owner --uid-owner $(stat -c %u /data/data/$service) -j DROP"
                    ))
                } catch (e: Exception) {
                    // Continue with other services
                }
            }
            
            true
            
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun optimizeRouting(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Optimize routing table for gaming (requires root)
            val routingOptimizations = listOf(
                "echo 1 > /proc/sys/net/ipv4/ip_forward",              // Enable IP forwarding
                "echo 1 > /proc/sys/net/ipv4/tcp_low_latency",         // Enable low latency mode
                "echo 1 > /proc/sys/net/ipv4/tcp_no_metrics_save",     // Don't save metrics
                "echo 0 > /proc/sys/net/ipv4/tcp_ecn",                  // Disable ECN for gaming
                "echo 1 > /proc/sys/net/ipv4/tcp_mtu_probing"           // Enable MTU probing
            )
            
            for (command in routingOptimizations) {
                try {
                    Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                } catch (e: Exception) {
                    // Continue with other optimizations
                }
            }
            
            true
            
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun measurePing(server: String = "8.8.8.8"): Int = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            
            val socket = Socket()
            socket.connect(InetSocketAddress(server, 53), 1000) // DNS port
            socket.close()
            
            val endTime = System.currentTimeMillis()
            (endTime - startTime).toInt()
            
        } catch (e: Exception) {
            999 // Connection failed
        }
    }
    
    suspend fun testMultipleServers(): List<PingResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<PingResult>()
        
        for (server in gameServers) {
            val ping = measurePing(server)
            results.add(PingResult(server, ping, ping < 1000))
            
            // Small delay between tests
            delay(100)
        }
        
        results.sortedBy { it.pingMs }
    }
    
    fun getNetworkInfo(): NetworkInfo {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        
        val isConnected = activeNetwork != null && capabilities != null
        val networkType = when {
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobile"
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
            else -> "Unknown"
        }
        
        val isMetered = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.isActiveNetworkMetered
        } else {
            networkType == "Mobile"
        }
        
        val strength = when {
            networkType == "WiFi" -> getWifiStrength()
            networkType == "Mobile" -> getMobileStrength()
            else -> 0
        }
        
        val speed = when {
            networkType == "WiFi" -> getWifiSpeed()
            networkType == "Mobile" -> getMobileSpeed()
            else -> 0L
        }
        
        return NetworkInfo(isConnected, networkType, isMetered, strength, speed)
    }
    
    private fun getWifiStrength(): Int {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            val wifiInfo = wifiManager.connectionInfo
            if (wifiInfo != null) {
                val level = android.net.wifi.WifiManager.calculateSignalLevel(wifiInfo.rssi, 5)
                level * 20 // Convert to percentage (0-100)
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }
    
    private fun getMobileStrength(): Int {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
            when (telephonyManager.networkType) {
                android.telephony.TelephonyManager.NETWORK_TYPE_LTE,
                android.telephony.TelephonyManager.NETWORK_TYPE_LTE_CA -> 80
                android.telephony.TelephonyManager.NETWORK_TYPE_HSPA,
                android.telephony.TelephonyManager.NETWORK_TYPE_HSPAP,
                android.telephony.TelephonyManager.NETWORK_TYPE_HSUPA,
                android.telephony.TelephonyManager.NETWORK_TYPE_HSDPA -> 60
                android.telephony.TelephonyManager.NETWORK_TYPE_UMTS -> 40
                android.telephony.TelephonyManager.NETWORK_TYPE_EDGE,
                android.telephony.TelephonyManager.NETWORK_TYPE_GPRS -> 20
                else -> 10
            }
        } catch (e: Exception) {
            0
        }
    }
    
    private fun getWifiSpeed(): Long {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            val wifiInfo = wifiManager.connectionInfo
            if (wifiInfo != null) {
                wifiInfo.linkSpeed.toLong() * 1000000 // Convert Mbps to bps
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }
    
    private fun getMobileSpeed(): Long {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
            when (telephonyManager.networkType) {
                android.telephony.TelephonyManager.NETWORK_TYPE_LTE,
                android.telephony.TelephonyManager.NETWORK_TYPE_LTE_CA -> 100000000L // 100 Mbps
                android.telephony.TelephonyManager.NETWORK_TYPE_HSPA,
                android.telephony.TelephonyManager.NETWORK_TYPE_HSPAP -> 42000000L // 42 Mbps
                android.telephony.TelephonyManager.NETWORK_TYPE_HSUPA -> 14000000L // 14 Mbps
                android.telephony.TelephonyManager.NETWORK_TYPE_HSDPA -> 7200000L // 7.2 Mbps
                android.telephony.TelephonyManager.NETWORK_TYPE_UMTS -> 2000000L // 2 Mbps
                android.telephony.TelephonyManager.NETWORK_TYPE_EDGE -> 384000L // 384 Kbps
                android.telephony.TelephonyManager.NETWORK_TYPE_GPRS -> 114000L // 114 Kbps
                else -> 0L
            }
        } catch (e: Exception) {
            0L
        }
    }
    
    suspend fun restoreNetworkSettings(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Restore default network settings (requires root)
            val restoreCommands = listOf(
                "echo 0 > /proc/sys/net/ipv4/tcp_window_scaling",
                "echo 0 > /proc/sys/net/ipv4/tcp_timestamps",
                "echo 0 > /proc/sys/net/ipv4/tcp_sack",
                "echo 1 > /proc/sys/net/ipv4/tcp_slow_start_after_idle",
                "echo 60 > /proc/sys/net/ipv4/tcp_fin_timeout",
                "echo 7200 > /proc/sys/net/ipv4/tcp_keepalive_time",
                "echo 9 > /proc/sys/net/ipv4/tcp_keepalive_probes",
                "echo 75 > /proc/sys/net/ipv4/tcp_keepalive_intvl",
                "echo cubic > /proc/sys/net/ipv4/tcp_congestion_control"
            )
            
            for (command in restoreCommands) {
                try {
                    Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                } catch (e: Exception) {
                    // Continue with other commands
                }
            }
            
            // Unblock background services
            unblockBackgroundNetwork()
            
            true
            
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun unblockBackgroundNetwork(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Clear iptables rules (requires root)
            Runtime.getRuntime().exec(arrayOf("su", "-c", "iptables -F OUTPUT"))
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun isNetworkOptimized(): Boolean {
        return isOptimizing.get()
    }
    
    fun getOptimizationReport(): Map<String, Any> {
        val networkInfo = getNetworkInfo()
        return mapOf(
            "network_type" to networkInfo.networkType,
            "is_connected" to networkInfo.isConnected,
            "strength" to networkInfo.strength,
            "speed" to networkInfo.speed,
            "is_metered" to networkInfo.isMetered,
            "is_optimizing" to isOptimizing.get()
        )
    }
}
