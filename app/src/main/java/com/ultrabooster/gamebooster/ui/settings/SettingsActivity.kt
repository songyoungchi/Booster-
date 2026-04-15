package com.ultrabooster.gamebooster.ui.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.ultrabooster.gamebooster.R
import com.ultrabooster.gamebooster.utils.BatteryOptimizer
import com.ultrabooster.gamebooster.utils.PermissionManager

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        
        private lateinit var permissionManager: PermissionManager
        private lateinit var batteryOptimizer: BatteryOptimizer
        
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            
            permissionManager = PermissionManager(requireContext())
            batteryOptimizer = BatteryOptimizer(requireContext())
            
            setupPreferences()
        }
        
        private fun setupPreferences() {
            // Auto boost preference
            findPreference<SwitchPreferenceCompat>("auto_boost")?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue as Boolean) {
                    // Enable auto boost
                    true
                } else {
                    // Disable auto boost
                    true
                }
            }
            
            // Game mode preference
            findPreference<SwitchPreferenceCompat>("game_mode")?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue as Boolean) {
                    if (!permissionManager.isAccessibilityServiceEnabled()) {
                        requestAccessibilityPermission()
                        false
                    } else {
                        true
                    }
                } else {
                    true
                }
            }
            
            // FPS counter preference
            findPreference<SwitchPreferenceCompat>("fps_counter")?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue as Boolean) {
                    if (!permissionManager.hasOverlayPermission()) {
                        requestOverlayPermission()
                        false
                    } else {
                        true
                    }
                } else {
                    true
                }
            }
            
            // Battery mode preference
            findPreference<Preference>("battery_mode")?.setOnPreferenceClickListener {
                showBatteryModeDialog()
                true
            }
            
            // Permissions preference
            findPreference<Preference>("permissions")?.setOnPreferenceClickListener {
                showPermissionsDialog()
                true
            }
            
            // About preference
            findPreference<Preference>("about")?.setOnPreferenceClickListener {
                showAboutDialog()
                true
            }
        }
        
        private fun requestAccessibilityPermission() {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
        
        private fun requestOverlayPermission() {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = android.net.Uri.parse("package:${requireContext().packageName}")
            startActivity(intent)
        }
        
        private fun showBatteryModeDialog() {
            val modes = arrayOf("Performance", "Balanced", "Battery Saver")
            val currentMode = when {
                // Get current mode from battery optimizer
                true -> 0 // Default to Performance for demo
                else -> 1
            }
            
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Select Battery Mode")
                .setSingleChoiceItems(modes, currentMode) { dialog, which ->
                    // Apply selected mode
                    when (which) {
                        0 -> batteryOptimizer.setPerformanceMode()
                        1 -> batteryOptimizer.setBalancedMode()
                        2 -> batteryOptimizer.setBatterySaverMode()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        
        private fun showPermissionsDialog() {
            val missingPermissions = permissionManager.getAllMissingPermissions()
            val message = if (missingPermissions.isEmpty()) {
                "All permissions granted!"
            } else {
                val missingList = missingPermissions.joinToString("\n") { permission ->
                    "· ${permissionManager.getPermissionExplanation(permission)}"
                }
                "Missing permissions:\n\n$missingList"
            }
            
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Permissions")
                .setMessage(message)
                .setPositiveButton("Grant Permissions") { _, _ ->
                    grantMissingPermissions()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        
        private fun grantMissingPermissions() {
            val missingPermissions = permissionManager.getAllMissingPermissions()
            
            when {
                missingPermissions.contains("accessibility") -> {
                    requestAccessibilityPermission()
                }
                missingPermissions.contains("overlay") -> {
                    requestOverlayPermission()
                }
                missingPermissions.contains("usage_stats") -> {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    startActivity(intent)
                }
                else -> {
                    // All other permissions handled
                }
            }
        }
        
        private fun showAboutDialog() {
            val message = """
                Ultra Game Booster v1.0.0
                
                A comprehensive gaming performance optimization app that enhances your Android gaming experience.
                
                Features:
                - One-Tap Boost
                - Game Mode Detection
                - FPS Monitoring
                - Network Optimization
                - Temperature Control
                - Battery Management
                
                Developed with Kotlin and Material Design 3
            """.trimIndent()
            
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("About")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        }
    }
}
