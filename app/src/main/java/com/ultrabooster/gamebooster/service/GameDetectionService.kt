package com.ultrabooster.gamebooster.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import com.ultrabooster.gamebooster.utils.BoostManager
import com.ultrabooster.gamebooster.utils.GameDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GameDetectionService : AccessibilityService() {
    
    private lateinit var gameDetector: GameDetector
    private lateinit var boostManager: BoostManager
    private var isGameModeActive = false
    private var currentGamePackage: String? = null
    
    override fun onCreate() {
        super.onCreate()
        gameDetector = GameDetector(this)
        boostManager = BoostManager(this)
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let { handleAccessibilityEvent(it) }
    }
    
    private fun handleAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChanged(event)
            }
        }
    }
    
    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        
        // Skip system packages
        if (isSystemPackage(packageName)) {
            return
        }
        
        // Check if this is a game
        if (gameDetector.isGame(packageName)) {
            if (!isGameModeActive || currentGamePackage != packageName) {
                // Game launched or switched
                activateGameMode(packageName)
            }
        } else {
            if (isGameModeActive) {
                // Game closed
                deactivateGameMode()
            }
        }
    }
    
    private fun activateGameMode(packageName: String) {
        isGameModeActive = true
        currentGamePackage = packageName
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Add to game whitelist
                boostManager.addToGameWhitelist(packageName)
                
                // Enable performance optimizations
                boostManager.enablePerformanceMode()
                
                // Show floating window if permission is granted
                showFloatingWindow()
                
                // Show notification
                showGameModeNotification(packageName)
                
                // Start FPS monitoring
                startFPSMonitoring()
                
            } catch (e: Exception) {
                // Handle exceptions silently
            }
        }
    }
    
    private fun deactivateGameMode() {
        isGameModeActive = false
        currentGamePackage = null
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Restore normal settings
                boostManager.restoreAnimations()
                
                // Hide floating window
                hideFloatingWindow()
                
                // Stop FPS monitoring
                stopFPSMonitoring()
                
                // Clear notification
                hideGameModeNotification()
                
            } catch (e: Exception) {
                // Handle exceptions silently
            }
        }
    }
    
    private fun showFloatingWindow() {
        try {
            val intent = Intent(this, FloatingWindowService::class.java).apply {
                action = FloatingWindowService.ACTION_SHOW
                putExtra(FloatingWindowService.EXTRA_GAME_PACKAGE, currentGamePackage)
            }
            startService(intent)
        } catch (e: Exception) {
            // Permission denied or service not available
        }
    }
    
    private fun hideFloatingWindow() {
        try {
            val intent = Intent(this, FloatingWindowService::class.java).apply {
                action = FloatingWindowService.ACTION_HIDE
            }
            startService(intent)
        } catch (e: Exception) {
            // Service not available
        }
    }
    
    private fun showGameModeNotification(packageName: String) {
        try {
            val gameName = getGameName(packageName)
            val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            
            val notification = android.app.Notification.Builder(this, "game_mode")
                .setContentTitle("Game Mode Active")
                .setContentText("Optimizing performance for $gameName")
                .setSmallIcon(R.drawable.ic_game_controller)
                .setOngoing(true)
                .build()
            
            notificationManager.notify(1001, notification)
        } catch (e: Exception) {
            // Notification permission not granted
        }
    }
    
    private fun hideGameModeNotification() {
        try {
            val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(1001)
        } catch (e: Exception) {
            // Notification manager not available
        }
    }
    
    private fun startFPSMonitoring() {
        try {
            val intent = Intent(this, FPSMonitorService::class.java).apply {
                action = FPSMonitorService.ACTION_START
            }
            startService(intent)
        } catch (e: Exception) {
            // Service not available
        }
    }
    
    private fun stopFPSMonitoring() {
        try {
            val intent = Intent(this, FPSMonitorService::class.java).apply {
                action = FPSMonitorService.ACTION_STOP
            }
            startService(intent)
        } catch (e: Exception) {
            // Service not available
        }
    }
    
    private fun isSystemPackage(packageName: String): Boolean {
        val systemPackages = setOf(
            "android",
            "com.android.systemui",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.android.settings",
            "com.android.launcher",
            "com.sec.android.app.launcher",
            "com.htc.launcher",
            "com.sonyericsson.home",
            "com.motorola.launcher"
        )
        
        return systemPackages.any { packageName.startsWith(it) }
    }
    
    private fun getGameName(packageName: String): String {
        return try {
            val packageManager = packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }
    
    override fun onInterrupt() {
        // Service interrupted
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        
        val serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
        
        serviceInfo = serviceInfo
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up when service is destroyed
        if (isGameModeActive) {
            deactivateGameMode()
        }
    }
    
    fun isGameModeCurrentlyActive(): Boolean {
        return isGameModeActive
    }
    
    fun getCurrentGamePackage(): String? {
        return currentGamePackage
    }
    
    fun manuallyActivateGameMode(packageName: String) {
        if (gameDetector.isGame(packageName)) {
            activateGameMode(packageName)
        }
    }
    
    fun manuallyDeactivateGameMode() {
        deactivateGameMode()
    }
}
