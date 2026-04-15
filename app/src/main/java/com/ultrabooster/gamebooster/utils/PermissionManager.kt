package com.ultrabooster.gamebooster.utils

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager

class PermissionManager(private val context: Context) {
    
    fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        
        val packageName = context.packageName
        return !TextUtils.isEmpty(enabledServices) && enabledServices.contains(packageName)
    }
    
    fun hasUsageStatsPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val packageManager = context.packageManager
                val applicationInfo = packageManager.getApplicationInfo(context.packageName, 0)
                val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val mode = appOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    applicationInfo.uid,
                    context.packageName
                )
                mode == AppOpsManager.MODE_ALLOWED
            } catch (e: Exception) {
                false
            }
        } else {
            true // Usage stats not available on older versions
        }
    }
    
    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true // Overlay permission not required on older versions
        }
    }
    
    fun hasWriteSettingsPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else {
            true // Write settings permission not required on older versions
        }
    }
    
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // Notification permission not required on older versions
        }
    }
    
    fun hasBatteryOptimizationWhitelist(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                !powerManager.isIgnoringBatteryOptimizations(context.packageName)
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    fun getAccessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }
    
    fun getUsageStatsSettingsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }
    
    fun getOverlaySettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        }
    }
    
    fun getWriteSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        }
    }
    
    fun getBatteryOptimizationIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        }
    }
    
    fun getNotificationSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent().apply {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        }
    }
    
    fun getAllMissingPermissions(): List<String> {
        val missing = mutableListOf<String>()
        
        if (!isAccessibilityServiceEnabled()) {
            missing.add("accessibility")
        }
        
        if (!hasUsageStatsPermission()) {
            missing.add("usage_stats")
        }
        
        if (!hasOverlayPermission()) {
            missing.add("overlay")
        }
        
        if (!hasWriteSettingsPermission()) {
            missing.add("write_settings")
        }
        
        if (!hasNotificationPermission()) {
            missing.add("notification")
        }
        
        if (!hasBatteryOptimizationWhitelist()) {
            missing.add("battery_optimization")
        }
        
        return missing
    }
    
    fun hasAllRequiredPermissions(): Boolean {
        return isAccessibilityServiceEnabled() &&
               hasUsageStatsPermission() &&
               hasOverlayPermission()
    }
    
    fun hasAllOptionalPermissions(): Boolean {
        return hasAllRequiredPermissions() &&
               hasWriteSettingsPermission() &&
               hasNotificationPermission() &&
               hasBatteryOptimizationWhitelist()
    }
    
    fun getPermissionExplanation(permission: String): String {
        return when (permission) {
            "accessibility" -> "Required to detect when games are launched and automatically activate boost mode"
            "usage_stats" -> "Required to monitor app usage and provide performance statistics"
            "overlay" -> "Required to show floating boost button and FPS counter"
            "write_settings" -> "Required to optimize system settings for better performance"
            "notification" -> "Required to show boost status and temperature warnings"
            "battery_optimization" -> "Required to prevent the app from being killed during gaming sessions"
            else -> "This permission is required for optimal functionality"
        }
    }
    
    fun isRootAvailable(): Boolean {
        return try {
            Runtime.getRuntime().exec("su")
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun canPerformSystemOptimizations(): Boolean {
        return isRootAvailable() || hasWriteSettingsPermission()
    }
}
