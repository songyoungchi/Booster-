package com.ultrabooster.gamebooster.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking

class BatteryOptimizer(private val context: Context) {
    
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    
    enum class BatteryMode {
        PERFORMANCE,
        BALANCED,
        BATTERY_SAVER
    }
    
    data class BatteryInfo(
        val level: Int,
        val temperature: Int,
        val voltage: Int,
        val health: String,
        val technology: String,
        val isCharging: Boolean,
        val isPowerSaveMode: Boolean,
        val estimatedTimeRemaining: Long,
        val mode: BatteryMode
    )
    
    suspend fun getCurrentBatteryInfo(): BatteryInfo = withContext(Dispatchers.IO) {
        val level = getBatteryLevel()
        val temperature = getBatteryTemperature()
        val voltage = getBatteryVoltage()
        val health = getBatteryHealth()
        val technology = getBatteryTechnology()
        val isCharging = isCharging()
        val isPowerSaveMode = isPowerSaveModeActive()
        val estimatedTimeRemaining = estimateTimeRemaining()
        val mode = getCurrentMode()
        
        BatteryInfo(
            level = level,
            temperature = temperature,
            voltage = voltage,
            health = health,
            technology = technology,
            isCharging = isCharging,
            isPowerSaveMode = isPowerSaveMode,
            estimatedTimeRemaining = estimatedTimeRemaining,
            mode = mode
        )
    }
    
    private fun getBatteryLevel(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } else {
            val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        }
    }
    
    private fun getBatteryTemperature(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_TEMPERATURE) / 10
        } else {
            val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)?.div(10) ?: -1
        }
    }
    
    private fun getBatteryVoltage(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_VOLTAGE)
        } else {
            val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        }
    }
    
    private fun getBatteryHealth(): String {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val health = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        
        return when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }
    }
    
    private fun getBatteryTechnology(): String {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return batteryStatus?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"
    }
    
    private fun isCharging(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            batteryManager.isCharging
        } else {
            val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        }
    }
    
    private fun isPowerSaveModeActive(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            powerManager.isPowerSaveMode
        } else {
            false
        }
    }
    
    private fun estimateTimeRemaining(): Long {
        // This is a rough estimation
        val level = getBatteryLevel()
        val isCharging = isCharging()
        
        return if (isCharging) {
            // Estimate time to full charge
            (100 - level) * 2 * 60 * 1000L // 2 minutes per percent
        } else {
            // Estimate time until empty
            level * 3 * 60 * 1000L // 3 minutes per percent
        }
    }
    
    private fun getCurrentMode(): BatteryMode {
        return when {
            isPowerSaveModeActive() -> BatteryMode.BATTERY_SAVER
            getBatteryLevel() > 70 -> BatteryMode.PERFORMANCE
            getBatteryLevel() > 30 -> BatteryMode.BALANCED
            else -> BatteryMode.BATTERY_SAVER
        }
    }
    
    suspend fun setPerformanceMode(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Disable power save mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                powerManager.isPowerSaveMode = false
            }
            
            // Optimize for performance
            optimizeForPerformance()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun setBalancedMode(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Enable moderate power saving
            optimizeForBalanced()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun setBatterySaverMode(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Enable power save mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                powerManager.isPowerSaveMode = true
            }
            
            // Optimize for battery
            optimizeForBattery()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun optimizeForPerformance() = withContext(Dispatchers.IO) {
        try {
            // Set CPU governor to performance
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo performance > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"
            ))
            
            // Enable all CPU cores
            val maxCores = Runtime.getRuntime().availableProcessors()
            for (core in 1 until maxCores) {
                try {
                    Runtime.getRuntime().exec(arrayOf(
                        "su", "-c", "echo 1 > /sys/devices/system/cpu/cpu$core/online"
                    ))
                } catch (e: Exception) {
                    // Core not available
                }
            }
            
            // Set GPU to maximum frequency
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo 650000000 > /sys/class/kgsl/kgsl-3d0/max_gpuclk"
            ))
            
            // Disable thermal throttling
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo 0 > /sys/class/thermal/thermal_zone0/mode"
            ))
            
            // Increase brightness (if not already max)
            increaseBrightness()
            
        } catch (e: Exception) {
            // Not rooted or permission denied
        }
    }
    
    private suspend fun optimizeForBalanced() = withContext(Dispatchers.IO) {
        try {
            // Set CPU governor to interactive
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo interactive > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"
            ))
            
            // Enable moderate CPU cores
            val maxCores = Runtime.getRuntime().availableProcessors()
            val coresToEnable = maxOf(2, maxCores / 2)
            for (core in 1 until coresToEnable) {
                try {
                    Runtime.getRuntime().exec(arrayOf(
                        "su", "-c", "echo 1 > /sys/devices/system/cpu/cpu$core/online"
                    ))
                } catch (e: Exception) {
                    // Core not available
                }
            }
            
            // Set GPU to moderate frequency
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo 450000000 > /sys/class/kgsl/kgsl-3d0/max_gpuclk"
            ))
            
            // Enable thermal throttling
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo 1 > /sys/class/thermal/thermal_zone0/mode"
            ))
            
        } catch (e: Exception) {
            // Not rooted or permission denied
        }
    }
    
    private suspend fun optimizeForBattery() = withContext(Dispatchers.IO) {
        try {
            // Set CPU governor to powersave
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo powersave > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"
            ))
            
            // Disable extra CPU cores
            val maxCores = Runtime.getRuntime().availableProcessors()
            for (core in 2 until maxCores) {
                try {
                    Runtime.getRuntime().exec(arrayOf(
                        "su", "-c", "echo 0 > /sys/devices/system/cpu/cpu$core/online"
                    ))
                } catch (e: Exception) {
                    // Core not available
                }
            }
            
            // Set GPU to minimum frequency
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo 200000000 > /sys/class/kgsl/kgsl-3d0/max_gpuclk"
            ))
            
            // Enable aggressive thermal throttling
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo 1 > /sys/class/thermal/thermal_zone0/mode"
            ))
            
            // Reduce brightness
            reduceBrightness()
            
            // Disable vibration
            disableVibration()
            
            // Limit background processes
            limitBackgroundProcesses()
            
        } catch (e: Exception) {
            // Not rooted or permission denied
        }
    }
    
    private suspend fun increaseBrightness() = withContext(Dispatchers.IO) {
        try {
            // Set brightness to maximum
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.provider.Settings.System.putInt(
                    context.contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                    255
                )
            }
        } catch (e: Exception) {
            // Permission denied
        }
    }
    
    private suspend fun reduceBrightness() = withContext(Dispatchers.IO) {
        try {
            // Set brightness to minimum comfortable level
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.provider.Settings.System.putInt(
                    context.contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                    30
                )
            }
        } catch (e: Exception) {
            // Permission denied
        }
    }
    
    private suspend fun disableVibration() = withContext(Dispatchers.IO) {
        try {
            // Disable vibration
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                android.provider.Settings.System.putInt(
                    context.contentResolver,
                    android.provider.Settings.System.VIBRATE_ON,
                    0
                )
            }
        } catch (e: Exception) {
            // Permission denied
        }
    }
    
    private suspend fun limitBackgroundProcesses() = withContext(Dispatchers.IO) {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            
            // Set background process limit
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                activityManager.isLowRamMode = true
            }
            
            // Kill background processes
            val runningApps = activityManager.getRunningAppProcesses()
            for (appProcess in runningApps) {
                if (appProcess.importance != android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    activityManager.killBackgroundProcesses(appProcess.processName)
                }
            }
            
        } catch (e: Exception) {
            // Permission denied
        }
    }
    
    suspend fun autoOptimizeBattery(): Boolean = withContext(Dispatchers.IO) {
        try {
            val batteryInfo = getCurrentBatteryInfo()
            
            when {
                batteryInfo.level <= 15 -> {
                    // Critical battery level
                    setBatterySaverMode()
                    showLowBatteryWarning()
                }
                batteryInfo.level <= 30 -> {
                    // Low battery level
                    setBalancedMode()
                }
                batteryInfo.level >= 80 && batteryInfo.isCharging -> {
                    // High battery level and charging
                    setPerformanceMode()
                }
                batteryInfo.level >= 60 -> {
                    // Good battery level
                    setBalancedMode()
                }
                else -> {
                    // Normal battery level
                    setBalancedMode()
                }
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun showLowBatteryWarning() {
        // In a real implementation, you would show a notification or dialog
        // For now, this is a placeholder
    }
    
    fun getBatteryOptimizationSuggestions(): List<String> {
        val suggestions = mutableListOf<String>()
        val batteryInfo = runBlocking { getCurrentBatteryInfo() }
        
        when {
            batteryInfo.level <= 15 -> {
                suggestions.add("Enable Battery Saver Mode")
                suggestions.add("Reduce screen brightness")
                suggestions.add("Close background apps")
                suggestions.add("Disable vibration")
                suggestions.add("Limit performance-intensive tasks")
            }
            batteryInfo.level <= 30 -> {
                suggestions.add("Use Balanced Mode")
                suggestions.add("Reduce screen brightness")
                suggestions.add("Close unnecessary apps")
            }
            batteryInfo.temperature >= 45 -> {
                suggestions.add("Device is overheating")
                suggestions.add("Reduce performance settings")
                suggestions.add("Take a break from gaming")
            }
            batteryInfo.health != "Good" -> {
                suggestions.add("Battery health is poor")
                suggestions.add("Consider battery replacement")
                suggestions.add("Avoid deep discharges")
            }
            else -> {
                suggestions.add("Battery is in good condition")
                suggestions.add("Performance Mode available")
            }
        }
        
        return suggestions
    }
    
    fun getBatteryReport(): Map<String, Any> {
        val batteryInfo = runBlocking { getCurrentBatteryInfo() }
        
        return mapOf(
            "level" to batteryInfo.level,
            "temperature" to batteryInfo.temperature,
            "voltage" to batteryInfo.voltage,
            "health" to batteryInfo.health,
            "technology" to batteryInfo.technology,
            "is_charging" to batteryInfo.isCharging,
            "is_power_save_mode" to batteryInfo.isPowerSaveMode,
            "estimated_time_remaining" to batteryInfo.estimatedTimeRemaining,
            "mode" to batteryInfo.mode.name,
            "suggestions" to getBatteryOptimizationSuggestions()
        )
    }
}
