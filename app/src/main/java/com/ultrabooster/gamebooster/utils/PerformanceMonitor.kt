package com.ultrabooster.gamebooster.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.os.SystemClock
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.concurrent.thread

class PerformanceMonitor(private val context: Context) {
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    data class RAMInfo(
        val total: Long,
        val used: Long,
        val free: Long,
        val percentage: Int
    )
    
    data class CPUInfo(
        val cores: Int,
        val percentage: Int,
        val frequency: Long
    )
    
    data class TemperatureInfo(
        val celsius: Int,
        val fahrenheit: Int,
        val percentage: Int,
        val status: String
    )
    
    data class PingInfo(
        val ms: Int,
        val percentage: Int,
        val status: String
    )
    
    fun getRAMInfo(): RAMInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalMemory = memoryInfo.totalMem
        val availableMemory = memoryInfo.availMem
        val usedMemory = totalMemory - availableMemory
        val percentage = ((usedMemory * 100) / totalMemory).toInt()
        
        return RAMInfo(
            total = totalMemory,
            used = usedMemory,
            free = availableMemory,
            percentage = percentage
        )
    }
    
    fun getCPUInfo(): CPUInfo {
        val cores = Runtime.getRuntime().availableProcessors()
        val percentage = getCpuUsage()
        val frequency = getCpuFrequency()
        
        return CPUInfo(
            cores = cores,
            percentage = percentage,
            frequency = frequency
        )
    }
    
    fun getTemperatureInfo(): TemperatureInfo {
        val celsius = getCpuTemperature()
        val fahrenheit = (celsius * 9/5) + 32
        val percentage = minOf(celsius * 2, 100) // Scale to percentage (50°C = 100%)
        val status = when {
            celsius < 40 -> "Normal"
            celsius < 60 -> "Warm"
            celsius < 75 -> "Hot"
            else -> "Critical"
        }
        
        return TemperatureInfo(
            celsius = celsius,
            fahrenheit = fahrenheit,
            percentage = percentage,
            status = status
        )
    }
    
    fun getPingInfo(): PingInfo {
        val ping = measurePing()
        val percentage = when {
            ping < 20 -> 100
            ping < 50 -> 80
            ping < 100 -> 60
            ping < 200 -> 40
            else -> 20
        }
        val status = when {
            ping < 20 -> "Excellent"
            ping < 50 -> "Good"
            ping < 100 -> "Fair"
            ping < 200 -> "Poor"
            else -> "Very Poor"
        }
        
        return PingInfo(
            ms = ping,
            percentage = percentage,
            status = status
        )
    }
    
    private fun getCpuUsage(): Int {
        try {
            var total = 0L
            var idle = 0L
            
            val reader = BufferedReader(FileReader("/proc/stat"))
            val line = reader.readLine()
            reader.close()
            
            if (line != null && line.startsWith("cpu ")) {
                val parts = line.trim().split(" +".toRegex())
                if (parts.size >= 5) {
                    val user = parts[1].toLong()
                    val nice = parts[2].toLong()
                    val system = parts[3].toLong()
                    idle = parts[4].toLong()
                    
                    total = user + nice + system + idle
                }
            }
            
            // Wait a moment and read again
            Thread.sleep(100)
            
            var total2 = 0L
            var idle2 = 0L
            
            val reader2 = BufferedReader(FileReader("/proc/stat"))
            val line2 = reader2.readLine()
            reader2.close()
            
            if (line2 != null && line2.startsWith("cpu ")) {
                val parts = line2.trim().split(" +".toRegex())
                if (parts.size >= 5) {
                    val user = parts[1].toLong()
                    val nice = parts[2].toLong()
                    val system = parts[3].toLong()
                    idle2 = parts[4].toLong()
                    
                    total2 = user + nice + system + idle2
                }
            }
            
            val totalDiff = total2 - total
            val idleDiff = idle2 - idle
            
            return if (totalDiff > 0) {
                ((totalDiff - idleDiff) * 100 / totalDiff).toInt()
            } else {
                0
            }
            
        } catch (e: Exception) {
            return 0
        }
    }
    
    private fun getCpuFrequency(): Long {
        try {
            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    // Use modern APIs for Android 12+
                    try {
                        val freqClass = Class.forName("android.os.Process")
                        val method = freqClass.getMethod("getElapsedCpuTime")
                        (method.invoke(null) as Long) / 1000000 // Convert to MHz
                    } catch (e: Exception) {
                        2000L // Default fallback
                    }
                }
                else -> {
                    // Read from /proc/cpuinfo for older versions
                    val reader = BufferedReader(FileReader("/proc/cpuinfo"))
                    var line: String?
                    var maxFreq = 0L
                    
                    while (reader.readLine().also { line = it } != null) {
                        line?.let {
                            if (it.contains("cpu MHz")) {
                                val freqStr = it.split(":").last().trim()
                                maxFreq = maxOf(maxFreq, freqStr.toDouble().toLong())
                            }
                        }
                    }
                    reader.close()
                    maxFreq
                }
            }
        } catch (e: Exception) {
            return 2000L // Default fallback
        }
    }
    
    private fun getCpuTemperature(): Int {
        try {
            // Try different temperature file paths
            val tempPaths = arrayOf(
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/class/thermal/thermal_zone1/temp",
                "/sys/devices/virtual/thermal/thermal_zone0/temp",
                "/sys/devices/virtual/thermal/thermal_zone1/temp"
            )
            
            for (path in tempPaths) {
                try {
                    val reader = BufferedReader(FileReader(path))
                    val temp = reader.readLine()?.trim()?.toLongOrNull()
                    reader.close()
                    
                    if (temp != null) {
                        // Some devices report temperature in millidegrees
                        return if (temp > 1000) (temp / 1000).toInt() else temp.toInt()
                    }
                } catch (e: Exception) {
                    continue
                }
            }
            
            // Fallback: estimate based on CPU usage
            val cpuUsage = getCpuUsage()
            return (30 + (cpuUsage * 0.3)).toInt() // Rough estimation
            
        } catch (e: Exception) {
            return 35 // Default fallback
        }
    }
    
    private fun measurePing(): Int {
        return try {
            val startTime = System.currentTimeMillis()
            val socket = Socket()
            socket.connect(InetSocketAddress("8.8.8.8", 53), 1000) // Google DNS
            val endTime = System.currentTimeMillis()
            socket.close()
            (endTime - startTime).toInt()
        } catch (e: Exception) {
            999 // Connection failed
        }
    }
    
    fun getRunningProcesses(): List<String> {
        val runningApps = mutableListOf<String>()
        val runningTasks = activityManager.getRunningTasks(100)
        
        for (taskInfo in runningTasks) {
            val packageName = taskInfo.topActivity?.packageName
            if (packageName != null && packageName != context.packageName) {
                runningApps.add(packageName)
            }
        }
        
        return runningApps.distinct()
    }
    
    fun getSystemMemoryInfo(): Map<String, Long> {
        val memoryInfo = mutableMapOf<String, Long>()
        
        try {
            val reader = BufferedReader(FileReader("/proc/meminfo"))
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                line?.let {
                    val parts = it.split(":")
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim().split(" ")[0].toLongOrNull() ?: 0L
                        memoryInfo[key] = value * 1024 // Convert KB to bytes
                    }
                }
            }
            reader.close()
            
        } catch (e: Exception) {
            // Fallback to ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            memoryInfo["MemTotal"] = memInfo.totalMem
            memoryInfo["MemAvailable"] = memInfo.availMem
        }
        
        return memoryInfo
    }
    
    fun isHighPerformanceMode(): Boolean {
        return try {
            // Check if device is in performance mode
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                powerManager.isPowerSaveMode.not()
            } else {
                // For older versions, check various indicators
                val cpuInfo = getCPUInfo()
                cpuInfo.percentage > 70
            }
        } catch (e: Exception) {
            false
        }
    }
}
