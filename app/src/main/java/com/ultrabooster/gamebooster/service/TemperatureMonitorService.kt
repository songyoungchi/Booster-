package com.ultrabooster.gamebooster.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.ultrabooster.gamebooster.R
import com.ultrabooster.gamebooster.utils.TemperatureMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TemperatureMonitorService : Service() {
    
    private lateinit var temperatureMonitor: TemperatureMonitor
    private val handler = Handler(Looper.getMainLooper())
    private var monitoringRunnable: Runnable? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var notificationManager: NotificationManager? = null
    
    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val NOTIFICATION_CHANNEL_ID = "temperature_monitor"
        const val NOTIFICATION_ID = 2001
    }
    
    override fun onCreate() {
        super.onCreate()
        temperatureMonitor = TemperatureMonitor(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startMonitoring()
            ACTION_STOP -> stopMonitoring()
        }
        return START_STICKY
    }
    
    private fun startMonitoring() {
        try {
            temperatureMonitor.startMonitoring()
            startPeriodicChecks()
            startForeground(NOTIFICATION_ID, createNotification())
        } catch (e: Exception) {
            stopSelf()
        }
    }
    
    private fun stopMonitoring() {
        try {
            temperatureMonitor.stopMonitoring()
            stopPeriodicChecks()
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            stopSelf()
        }
    }
    
    private fun startPeriodicChecks() {
        monitoringRunnable = object : Runnable {
            override fun run() {
                serviceScope.launch {
                    try {
                        checkTemperature()
                    } catch (e: Exception) {
                        // Handle exceptions silently
                    }
                }
                handler.postDelayed(this, 5000) // Check every 5 seconds
            }
        }
        handler.post(monitoringRunnable!!)
    }
    
    private fun stopPeriodicChecks() {
        monitoringRunnable?.let { handler.removeCallbacks(it) }
        monitoringRunnable = null
    }
    
    private suspend fun checkTemperature() {
        val tempInfo = temperatureMonitor.getCurrentTemperature()
        
        when {
            tempInfo.celsius >= 80 -> {
                // Critical temperature - emergency cooling
                handleCriticalTemperature(tempInfo)
            }
            tempInfo.celsius >= 70 -> {
                // High temperature - aggressive cooling
                handleHighTemperature(tempInfo)
            }
            tempInfo.celsius >= 60 -> {
                // Warm temperature - moderate cooling
                handleWarmTemperature(tempInfo)
            }
            tempInfo.celsius <= 40 -> {
                // Normal temperature - can boost performance
                handleNormalTemperature(tempInfo)
            }
        }
        
        updateNotification(tempInfo)
    }
    
    private suspend fun handleCriticalTemperature(tempInfo: TemperatureMonitor.TemperatureInfo) {
        // Emergency cooling measures
        try {
            // Show critical warning
            showCriticalTemperatureWarning(tempInfo)
            
            // Apply maximum cooling
            temperatureMonitor.applyMaximumCooling()
            
            // Reduce performance drastically
            reducePerformanceToMinimum()
            
            // Notify user
            showNotification(
                "Critical Temperature Warning",
                "Device temperature: ${tempInfo.celsius}°C - Performance reduced to prevent damage",
                NotificationCompat.PRIORITY_HIGH
            )
            
        } catch (e: Exception) {
            // Handle exceptions silently
        }
    }
    
    private suspend fun handleHighTemperature(tempInfo: TemperatureMonitor.TemperatureInfo) {
        // Aggressive cooling measures
        try {
            // Show high temperature warning
            showHighTemperatureWarning(tempInfo)
            
            // Apply aggressive cooling
            temperatureMonitor.applyAggressiveCooling()
            
            // Reduce performance significantly
            reducePerformanceSignificantly()
            
            // Notify user
            showNotification(
                "High Temperature Warning",
                "Device temperature: ${tempInfo.celsius}°C - Performance reduced",
                NotificationCompat.PRIORITY_DEFAULT
            )
            
        } catch (e: Exception) {
            // Handle exceptions silently
        }
    }
    
    private suspend fun handleWarmTemperature(tempInfo: TemperatureMonitor.TemperatureInfo) {
        // Moderate cooling measures
        try {
            // Apply moderate cooling
            temperatureMonitor.applyModerateCooling()
            
            // Slightly reduce performance
            reducePerformanceModerately()
            
        } catch (e: Exception) {
            // Handle exceptions silently
        }
    }
    
    private suspend fun handleNormalTemperature(tempInfo: TemperatureMonitor.TemperatureInfo) {
        // Temperature is normal, can boost performance
        try {
            // Restore normal performance
            restoreNormalPerformance()
            
            // Stop cooling measures
            temperatureMonitor.stopCooling()
            
        } catch (e: Exception) {
            // Handle exceptions silently
        }
    }
    
    private suspend fun reducePerformanceToMinimum() {
        try {
            // Set CPU to lowest frequency
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo powersave > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"
            ))
            
            // Limit CPU cores
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo 0 > /sys/devices/system/cpu/cpu1/online"
            ))
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo 0 > /sys/devices/system/cpu/cpu2/online"
            ))
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo 0 > /sys/devices/system/cpu/cpu3/online"
            ))
            
            // Disable GPU performance mode
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo 0 > /sys/class/kgsl/kgsl-3d0/max_gpuclk"
            ))
            
        } catch (e: Exception) {
            // Not rooted or permission denied
        }
    }
    
    private suspend fun reducePerformanceSignificantly() {
        try {
            // Set CPU to conservative governor
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo conservative > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"
            ))
            
            // Limit CPU frequency
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo 800000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq"
            ))
            
        } catch (e: Exception) {
            // Not rooted or permission denied
        }
    }
    
    private suspend fun reducePerformanceModerately() {
        try {
            // Set CPU to ondemand governor
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo ondemand > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"
            ))
            
        } catch (e: Exception) {
            // Not rooted or permission denied
        }
    }
    
    private suspend fun restoreNormalPerformance() {
        try {
            // Restore CPU governor
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo interactive > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"
            ))
            
            // Enable all CPU cores
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo 1 > /sys/devices/system/cpu/cpu1/online"
            ))
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo 1 > /sys/devices/system/cpu/cpu2/online"
            ))
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo 1 > /sys/devices/system/cpu/cpu3/online"
            ))
            
        } catch (e: Exception) {
            // Not rooted or permission denied
        }
    }
    
    private fun showCriticalTemperatureWarning(tempInfo: TemperatureMonitor.TemperatureInfo) {
        // Could show a dialog or overlay warning
        // For now, just show notification
    }
    
    private fun showHighTemperatureWarning(tempInfo: TemperatureMonitor.TemperatureInfo) {
        // Could show a dialog or overlay warning
        // For now, just show notification
    }
    
    private fun showNotification(title: String, message: String, priority: Int) {
        try {
            val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_temperature_warning)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(priority)
                .setAutoCancel(true)
                .build()
            
            notificationManager?.notify(System.currentTimeMillis().toInt(), notification)
            
        } catch (e: Exception) {
            // Notification permission not granted
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Temperature Monitor",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Monitors device temperature and provides cooling notifications"
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_temperature)
            .setContentTitle("Temperature Monitor Active")
            .setContentText("Monitoring device temperature")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification(tempInfo: TemperatureMonitor.TemperatureInfo) {
        try {
            val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_temperature)
                .setContentTitle("Temperature Monitor")
                .setContentText("Current: ${tempInfo.celsius}°C (${tempInfo.status})")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build()
            
            notificationManager?.notify(NOTIFICATION_ID, notification)
            
        } catch (e: Exception) {
            // Handle exceptions silently
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        serviceScope.cancel()
    }
}
