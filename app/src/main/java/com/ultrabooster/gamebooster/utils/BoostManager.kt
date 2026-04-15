package com.ultrabooster.gamebooster.utils

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class BoostManager(private val context: Context) {
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val performanceMonitor = PerformanceMonitor(context)
    
    // System-critical packages that should never be killed
    private val criticalPackages = setOf(
        "android",
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.android.systemui",
        "com.android.phone",
        "com.android.settings"
    )
    
    // Game-related packages to preserve
    private val gamePackages = mutableSetOf<String>()
    
    suspend fun performFullBoost() = withContext(Dispatchers.IO) {
        try {
            clearRAM()
            clearCache()
            optimizeCPU()
            optimizeNetwork()
            stopUnnecessaryServices()
            optimizeAnimations()
        } catch (e: Exception) {
            // Handle exceptions silently
        }
    }
    
    suspend fun clearRAM() = withContext(Dispatchers.IO) {
        try {
            val runningApps = performanceMonitor.getRunningProcesses()
            
            for (packageName in runningApps) {
                if (!isCriticalPackage(packageName) && !gamePackages.contains(packageName)) {
                    try {
                        activityManager.killBackgroundProcesses(packageName)
                    } catch (e: Exception) {
                        // Continue with other apps
                    }
                }
            }
            
            // Force garbage collection
            System.gc()
            
        } catch (e: Exception) {
            // Handle silently
        }
    }
    
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        try {
            // Clear app cache
            val cacheDir = context.cacheDir
            deleteRecursive(cacheDir)
            
            // Clear external cache if available
            if (context.externalCacheDir != null) {
                deleteRecursive(context.externalCacheDir!!)
            }
            
            // Try to clear system cache (requires root or specific permissions)
            try {
                Runtime.getRuntime().exec("su -c 'echo 3 > /proc/sys/vm/drop_caches'")
            } catch (e: Exception) {
                // Not rooted or permission denied
            }
            
        } catch (e: Exception) {
            // Handle silently
        }
    }
    
    suspend fun optimizeCPU() = withContext(Dispatchers.IO) {
        try {
            // Set CPU governor to performance if possible (requires root)
            try {
                Runtime.getRuntime().exec("su -c 'echo performance > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor'")
            } catch (e: Exception) {
                // Not rooted or permission denied
            }
            
            // Disable thermal throttling temporarily (requires root)
            try {
                Runtime.getRuntime().exec("su -c 'echo 0 > /sys/class/thermal/thermal_zone0/mode'")
            } catch (e: Exception) {
                // Not rooted or permission denied
            }
            
        } catch (e: Exception) {
            // Handle silently
        }
    }
    
    suspend fun optimizeNetwork() = withContext(Dispatchers.IO) {
        try {
            // Optimize network settings (some require root)
            try {
                // TCP window scaling
                Runtime.getRuntime().exec("su -c 'echo 1 > /proc/sys/net/ipv4/tcp_window_scaling'")
                
                // TCP timestamps
                Runtime.getRuntime().exec("su -c 'echo 1 > /proc/sys/net/ipv4/tcp_timestamps'")
                
                // TCP selective acknowledgments
                Runtime.getRuntime().exec("su -c 'echo 1 > /proc/sys/net/ipv4/tcp_sack'")
                
                // TCP congestion control
                Runtime.getRuntime().exec("su -c 'echo bbr > /proc/sys/net/ipv4/tcp_congestion_control'")
            } catch (e: Exception) {
                // Not rooted or permission denied
            }
            
        } catch (e: Exception) {
            // Handle silently
        }
    }
    
    suspend fun stopUnnecessaryServices() = withContext(Dispatchers.IO) {
        try {
            val unnecessaryServices = listOf(
                "com.android.bluetooth",
                "com.android.location",
                "com.google.android.gms.update",
                "com.google.android.gms.analytics",
                "com.google.android.gms.ads"
            )
            
            for (service in unnecessaryServices) {
                try {
                    Runtime.getRuntime().exec("su -c 'am force-stop $service'")
                } catch (e: Exception) {
                    // Not rooted or permission denied
                }
            }
            
        } catch (e: Exception) {
            // Handle silently
        }
    }
    
    suspend fun optimizeAnimations() = withContext(Dispatchers.IO) {
        try {
            // Disable or reduce system animations for better performance
            val animationScales = floatArrayOf(0.0f, 0.0f, 0.0f)
            
            try {
                Settings.Global.putFloat(context.contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE, animationScales[0])
                Settings.Global.putFloat(context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, animationScales[1])
                Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, animationScales[2])
            } catch (e: Exception) {
                // Permission denied
            }
            
        } catch (e: Exception) {
            // Handle silently
        }
    }
    
    suspend fun restoreAnimations() = withContext(Dispatchers.IO) {
        try {
            // Restore normal animations
            val animationScales = floatArrayOf(1.0f, 1.0f, 1.0f)
            
            try {
                Settings.Global.putFloat(context.contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE, animationScales[0])
                Settings.Global.putFloat(context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, animationScales[1])
                Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, animationScales[2])
            } catch (e: Exception) {
                // Permission denied
            }
            
        } catch (e: Exception) {
            // Handle silently
        }
    }
    
    fun addToGameWhitelist(packageName: String) {
        gamePackages.add(packageName)
    }
    
    fun removeFromGameWhitelist(packageName: String) {
        gamePackages.remove(packageName)
    }
    
    fun getGameWhitelist(): Set<String> {
        return gamePackages.toSet()
    }
    
    private fun isCriticalPackage(packageName: String): Boolean {
        return criticalPackages.any { packageName.contains(it) } ||
               packageName.startsWith("com.android.") ||
               packageName.startsWith("com.google.android.") ||
               packageName.startsWith("android.")
    }
    
    private fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) {
            fileOrDirectory.listFiles()?.forEach { child ->
                deleteRecursive(child)
            }
        }
        fileOrDirectory.delete()
    }
    
    suspend fun optimizeBatteryUsage() = withContext(Dispatchers.IO) {
        try {
            // Enable battery saver mode optimizations
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                // Note: This requires system permissions
            }
            
            // Reduce background process limit
            try {
                activityManager.setBackgroundRestrictionExemptionApps(emptyList())
            } catch (e: Exception) {
                // Permission denied
            }
            
        } catch (e: Exception) {
            // Handle silently
        }
    }
    
    suspend fun enablePerformanceMode() = withContext(Dispatchers.IO) {
        try {
            optimizeCPU()
            optimizeNetwork()
            optimizeAnimations()
            
            // Set process priority to high
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY)
            } catch (e: Exception) {
                // Permission denied
            }
            
        } catch (e: Exception) {
            // Handle silently
        }
    }
    
    suspend fun enableBalancedMode() = withContext(Dispatchers.IO) {
        try {
            // Moderate optimizations
            clearRAM()
            optimizeNetwork()
            
            // Restore animations
            restoreAnimations()
            
        } catch (e: Exception) {
            // Handle silently
        }
    }
    
    suspend fun enableBatterySaverMode() = withContext(Dispatchers.IO) {
        try {
            optimizeBatteryUsage()
            stopUnnecessaryServices()
            
            // Reduce CPU frequency if possible (requires root)
            try {
                Runtime.getRuntime().exec("su -c 'echo powersave > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor'")
            } catch (e: Exception) {
                // Not rooted or permission denied
            }
            
        } catch (e: Exception) {
            // Handle silently
        }
    }
    
    fun getBoostStats(): Map<String, Any> {
        val ramInfo = performanceMonitor.getRAMInfo()
        val cpuInfo = performanceMonitor.getCPUInfo()
        
        return mapOf(
            "ram_freed" to ramInfo.free,
            "cpu_usage" to cpuInfo.percentage,
            "game_count" to gamePackages.size,
            "last_boost" to System.currentTimeMillis()
        )
    }
}
