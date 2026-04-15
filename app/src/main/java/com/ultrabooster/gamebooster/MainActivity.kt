package com.ultrabooster.gamebooster

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ultrabooster.gamebooster.databinding.ActivityMainBinding
import com.ultrabooster.gamebooster.service.GameDetectionService
import com.ultrabooster.gamebooster.ui.game.GameLauncherActivity
import com.ultrabooster.gamebooster.ui.settings.SettingsActivity
import com.ultrabooster.gamebooster.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var boostManager: BoostManager
    private lateinit var permissionManager: PermissionManager
    
    private val handler = Handler(Looper.getMainLooper())
    private var updateStatsRunnable: Runnable? = null
    private var isBoosting = false
    
    // Permission launchers
    private val accessibilityPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (permissionManager.isAccessibilityServiceEnabled()) {
            startGameDetectionService()
        } else {
            showPermissionDeniedDialog()
        }
    }
    
    private val usageStatsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (permissionManager.hasUsageStatsPermission()) {
            startPerformanceMonitoring()
        } else {
            showPermissionDeniedDialog()
        }
    }
    
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (permissionManager.hasOverlayPermission()) {
            // Floating window can be shown
        } else {
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initializeManagers()
        setupUI()
        checkPermissions()
        startPerformanceMonitoring()
    }

    private fun initializeManagers() {
        performanceMonitor = PerformanceMonitor(this)
        boostManager = BoostManager(this)
        permissionManager = PermissionManager(this)
    }

    private fun setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        
        // Setup tab layout
        binding.tabLayout.addOnTabSelectedListener(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> { /* Dashboard - already visible */ }
                    1 -> startActivity(Intent(this@MainActivity, GameLauncherActivity::class.java))
                    2 -> startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                }
                // Reset to dashboard tab
                binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0))
            }
        })
        
        // Setup boost button
        binding.btnBoost.setOnClickListener {
            if (!isBoosting) {
                performBoost()
            }
        }
        
        // Setup quick action buttons
        binding.btnClearRam.setOnClickListener {
            clearRAM()
        }
        
        binding.btnClearCache.setOnClickListener {
            clearCache()
        }
        
        binding.btnOptimizeNetwork.setOnClickListener {
            optimizeNetwork()
        }
        
        // Setup game mode switch
        binding.switchGameMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                enableGameMode()
            } else {
                disableGameMode()
            }
        }
    }

    private fun checkPermissions() {
        val missingPermissions = mutableListOf<String>()
        
        if (!permissionManager.isAccessibilityServiceEnabled()) {
            missingPermissions.add("accessibility")
        }
        
        if (!permissionManager.hasUsageStatsPermission()) {
            missingPermissions.add("usage_stats")
        }
        
        if (!permissionManager.hasOverlayPermission()) {
            missingPermissions.add("overlay")
        }
        
        if (missingPermissions.isNotEmpty()) {
            showPermissionRequestDialog(missingPermissions)
        } else {
            startGameDetectionService()
        }
    }

    private fun showPermissionRequestDialog(missingPermissions: List<String>) {
        val message = when {
            missingPermissions.contains("accessibility") -> getString(R.string.accessibility_description)
            missingPermissions.contains("usage_stats") -> getString(R.string.usage_stats_description)
            missingPermissions.contains("overlay") -> getString(R.string.overlay_description)
            else -> getString(R.string.permission_required)
        }
        
        binding.tvPermissionMessage.text = message
        binding.layoutPermissionRequest.visibility = View.VISIBLE
        
        binding.btnGrantPermission.setOnClickListener {
            when {
                missingPermissions.contains("accessibility") -> requestAccessibilityPermission()
                missingPermissions.contains("usage_stats") -> requestUsageStatsPermission()
                missingPermissions.contains("overlay") -> requestOverlayPermission()
            }
        }
        
        binding.btnDenyPermission.setOnClickListener {
            binding.layoutPermissionRequest.visibility = View.GONE
            showPermissionDeniedDialog()
        }
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        accessibilityPermissionLauncher.launch(intent)
    }

    private fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        usageStatsPermissionLauncher.launch(intent)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:$packageName")
        }
        overlayPermissionLauncher.launch(intent)
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.permission_denied))
            .setMessage("Some features may not work properly without the required permissions.")
            .setPositiveButton("Retry") { _, _ ->
                checkPermissions()
            }
            .setNegativeButton("Continue") { _, _ ->
                // Continue with limited functionality
            }
            .show()
    }

    private fun startGameDetectionService() {
        if (permissionManager.isAccessibilityServiceEnabled()) {
            val intent = Intent(this, GameDetectionService::class.java)
            startService(intent)
        }
    }

    private fun startPerformanceMonitoring() {
        if (permissionManager.hasUsageStatsPermission()) {
            updateStatsRunnable = object : Runnable {
                override fun run() {
                    updateStats()
                    handler.postDelayed(this, 2000) // Update every 2 seconds
                }
            }
            handler.post(updateStatsRunnable!!)
        }
    }

    private fun updateStats() {
        lifecycleScope.launch {
            try {
                // Update RAM stats
                val ramInfo = performanceMonitor.getRAMInfo()
                binding.tvRamUsage.text = formatBytes(ramInfo.used)
                binding.tvRamPercentage.text = "${ramInfo.percentage}%"
                binding.progressRam.progress = ramInfo.percentage
                
                // Update CPU stats
                val cpuInfo = performanceMonitor.getCPUInfo()
                binding.tvCpuUsage.text = "${cpuInfo.percentage}%"
                binding.tvCpuCores.text = "${cpuInfo.cores} Cores"
                binding.progressCpu.progress = cpuInfo.percentage
                
                // Update temperature
                val tempInfo = performanceMonitor.getTemperatureInfo()
                binding.tvTemperature.text = "${tempInfo.celsius}°C"
                binding.tvTempStatus.text = tempInfo.status
                binding.tvTempStatus.setTextColor(getColorForStatus(tempInfo.status))
                binding.progressTemp.progress = tempInfo.percentage
                
                // Update network ping
                val pingInfo = performanceMonitor.getPingInfo()
                binding.tvPing.text = "${pingInfo.ms}ms"
                binding.tvNetworkStatus.text = pingInfo.status
                binding.progressPing.progress = pingInfo.percentage
                
            } catch (e: Exception) {
                // Handle exceptions silently
            }
        }
    }

    private fun performBoost() {
        isBoosting = true
        binding.btnBoost.text = getString(R.string.boosting)
        binding.btnBoost.isEnabled = false
        
        lifecycleScope.launch {
            try {
                // Show before stats
                val beforeRAM = performanceMonitor.getRAMInfo()
                val beforeCPU = performanceMonitor.getCPUInfo()
                
                // Perform boost operations
                boostManager.performFullBoost()
                
                // Wait a moment for operations to complete
                delay(3000)
                
                // Show after stats
                val afterRAM = performanceMonitor.getRAMInfo()
                val afterCPU = performanceMonitor.getCPUInfo()
                
                // Update UI with results
                binding.tvBoostStatus.text = "RAM: +${formatBytes(beforeRAM.used - afterRAM.used)} | CPU: -${beforeCPU.percentage - afterCPU.percentage}%"
                
                // Show completion
                binding.btnBoost.text = getString(R.string.boost_complete)
                binding.btnBoost.setBackgroundColor(getColor(R.color.success))
                
                // Reset button after delay
                delay(2000)
                binding.btnBoost.text = getString(R.string.boost_now)
                binding.btnBoost.setBackgroundColor(getColor(R.color.boost_primary))
                binding.btnBoost.isEnabled = true
                isBoosting = false
                
            } catch (e: Exception) {
                binding.btnBoost.text = getString(R.string.boost_now)
                binding.btnBoost.isEnabled = true
                isBoosting = false
            }
        }
    }

    private fun clearRAM() {
        lifecycleScope.launch {
            try {
                boostManager.clearRAM()
                updateStats()
            } catch (e: Exception) {
                // Handle silently
            }
        }
    }

    private fun clearCache() {
        lifecycleScope.launch {
            try {
                boostManager.clearCache()
                updateStats()
            } catch (e: Exception) {
                // Handle silently
            }
        }
    }

    private fun optimizeNetwork() {
        lifecycleScope.launch {
            try {
                boostManager.optimizeNetwork()
                updateStats()
            } catch (e: Exception) {
                // Handle silently
            }
        }
    }

    private fun enableGameMode() {
        binding.tvGameModeStatus.text = getString(R.string.game_mode_active)
        binding.tvGameModeStatus.setTextColor(getColor(R.color.success))
        
        if (permissionManager.isAccessibilityServiceEnabled()) {
            startGameDetectionService()
        }
    }

    private fun disableGameMode() {
        binding.tvGameModeStatus.text = getString(R.string.game_mode_disabled)
        binding.tvGameModeStatus.setTextColor(getColor(R.color.danger))
        
        val intent = Intent(this, GameDetectionService::class.java)
        stopService(intent)
    }

    private fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1 -> String.format("%.1f GB", gb)
            mb >= 1 -> String.format("%.0f MB", mb)
            else -> String.format("%.0f KB", kb)
        }
    }

    private fun getColorForStatus(status: String): Int {
        return when (status.lowercase()) {
            "normal", "excellent" -> getColor(R.color.success)
            "warning", "good" -> getColor(R.color.warning)
            "critical", "poor" -> getColor(R.color.danger)
            else -> getColor(R.color.info)
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatsRunnable?.let { handler.post(it) }
    }

    override fun onPause() {
        super.onPause()
        updateStatsRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateStatsRunnable?.let { handler.removeCallbacks(it) }
    }
}
