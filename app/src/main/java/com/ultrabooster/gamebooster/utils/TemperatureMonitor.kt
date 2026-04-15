package com.ultrabooster.gamebooster.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileReader
import java.util.concurrent.atomic.AtomicBoolean

class TemperatureMonitor(private val context: Context) {
    
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val isMonitoring = AtomicBoolean(false)
    private var temperatureSensor: Sensor? = null
    private var temperatureListener: TemperatureListener? = null
    
    // Temperature file paths for different devices
    private val temperaturePaths = listOf(
        "/sys/class/thermal/thermal_zone0/temp",
        "/sys/class/thermal/thermal_zone1/temp",
        "/sys/class/thermal/thermal_zone2/temp",
        "/sys/class/thermal/thermal_zone3/temp",
        "/sys/class/thermal/thermal_zone4/temp",
        "/sys/class/thermal/thermal_zone5/temp",
        "/sys/devices/virtual/thermal/thermal_zone0/temp",
        "/sys/devices/virtual/thermal/thermal_zone1/temp",
        "/sys/devices/virtual/thermal/thermal_zone2/temp",
        "/sys/devices/virtual/thermal/thermal_zone3/temp",
        "/sys/devices/virtual/thermal/thermal_zone4/temp",
        "/sys/devices/virtual/thermal/thermal_zone5/temp",
        "/sys/devices/platform/omap/omap_temp_sensor.0/temp1_input",
        "/sys/devices/platform/tegra_tmon/temp1_input",
        "/sys/devices/virtual/thermal/thermal_zone0/subsystem/thermal_zone0/temp"
    )
    
    data class TemperatureInfo(
        val celsius: Int,
        val fahrenheit: Int,
        val kelvin: Double,
        val status: String,
        val zone: String,
        val timestamp: Long
    )
    
    private inner class TemperatureListener : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                if (it.sensor.type == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                    val celsius = it.values[0].toInt()
                    // Handle sensor temperature reading
                }
            }
        }
        
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Handle accuracy changes
        }
    }
    
    fun startMonitoring() {
        if (isMonitoring.compareAndSet(false, true)) {
            startSensorMonitoring()
        }
    }
    
    fun stopMonitoring() {
        if (isMonitoring.compareAndSet(true, false)) {
            stopSensorMonitoring()
        }
    }
    
    private fun startSensorMonitoring() {
        try {
            // Try to use hardware temperature sensor
            temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
            if (temperatureSensor != null) {
                temperatureListener = TemperatureListener()
                sensorManager.registerListener(
                    temperatureListener,
                    temperatureSensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }
        } catch (e: Exception) {
            // Hardware sensor not available, will use file reading
        }
    }
    
    private fun stopSensorMonitoring() {
        try {
            temperatureListener?.let { listener ->
                sensorManager.unregisterListener(listener)
            }
            temperatureListener = null
            temperatureSensor = null
        } catch (e: Exception) {
            // Handle exceptions silently
        }
    }
    
    suspend fun getCurrentTemperature(): TemperatureInfo = withContext(Dispatchers.IO) {
        val cpuTemp = getCPUTemperature()
        val batteryTemp = getBatteryTemperature()
        
        // Use the higher of CPU or battery temperature
        val currentTemp = maxOf(cpuTemp, batteryTemp)
        
        val status = when {
            currentTemp >= 80 -> "Critical"
            currentTemp >= 70 -> "High"
            currentTemp >= 60 -> "Warm"
            currentTemp >= 50 -> "Normal"
            else -> "Cool"
        }
        
        val zone = when {
            currentTemp >= 80 -> "Red"
            currentTemp >= 70 -> "Orange"
            currentTemp >= 60 -> "Yellow"
            currentTemp >= 50 -> "Green"
            else -> "Blue"
        }
        
        TemperatureInfo(
            celsius = currentTemp,
            fahrenheit = (currentTemp * 9/5) + 32,
            kelvin = currentTemp + 273.15,
            status = status,
            zone = zone,
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun getCPUTemperature(): Int {
        for (path in temperaturePaths) {
            try {
                val temp = readTemperatureFile(path)
                if (temp > 0) {
                    return temp
                }
            } catch (e: Exception) {
                continue
            }
        }
        
        // Fallback: estimate based on CPU usage
        return estimateTemperatureFromCPU()
    }
    
    private fun readTemperatureFile(path: String): Int {
        return try {
            val reader = BufferedReader(FileReader(path))
            val temp = reader.readLine()?.trim()?.toLongOrNull()
            reader.close()
            
            when {
                temp != null -> {
                    // Some devices report temperature in millidegrees
                    if (temp > 1000) {
                        (temp / 1000).toInt()
                    } else {
                        temp.toInt()
                    }
                }
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }
    
    private fun getBatteryTemperature(): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
                val temperature = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_TEMPERATURE)
                // Battery temperature is reported in tenths of a degree Celsius
                if (temperature > 0) temperature / 10 else 0
            } else {
                // For older versions, try to read from system files
                readBatteryTemperatureFromFiles()
            }
        } catch (e: Exception) {
            0
        }
    }
    
    private fun readBatteryTemperatureFromFiles(): Int {
        val batteryTempPaths = listOf(
            "/sys/class/power_supply/battery/temp",
            "/sys/class/power_supply/battery/batt_temp",
            "/sys/devices/platform/battery/power_supply/battery/temp"
        )
        
        for (path in batteryTempPaths) {
            try {
                val temp = readTemperatureFile(path)
                if (temp > 0) {
                    return temp
                }
            } catch (e: Exception) {
                continue
            }
        }
        
        return 0
    }
    
    private fun estimateTemperatureFromCPU(): Int {
        return try {
            val performanceMonitor = PerformanceMonitor(context)
            val cpuInfo = performanceMonitor.getCPUInfo()
            
            // Rough estimation based on CPU usage
            val baseTemp = 35 // Base temperature
            val tempIncrease = (cpuInfo.percentage * 0.4).toInt() // 0.4°C per 1% CPU usage
            baseTemp + tempIncrease
            
        } catch (e: Exception) {
            35 // Default fallback
        }
    }
    
    suspend fun applyMaximumCooling() = withContext(Dispatchers.IO) {
        try {
            // Set CPU to lowest frequency
            setCPUFrequency("powersave", 800000)
            
            // Disable CPU cores
            disableCPUCores(listOf(1, 2, 3, 4, 5, 6, 7))
            
            // Reduce GPU frequency
            reduceGPUFrequency()
            
            // Enable thermal throttling
            enableThermalThrottling()
            
            // Increase fan speed (if available)
            increaseFanSpeed()
            
        } catch (e: Exception) {
            // Handle exceptions silently
        }
    }
    
    suspend fun applyAggressiveCooling() = withContext(Dispatchers.IO) {
        try {
            // Set CPU to conservative governor
            setCPUFrequency("conservative", 1200000)
            
            // Limit CPU cores
            disableCPUCores(listOf(3, 4, 5, 6, 7))
            
            // Reduce GPU frequency moderately
            reduceGPUFrequencyModerately()
            
            // Enable thermal throttling
            enableThermalThrottling()
            
        } catch (e: Exception) {
            // Handle exceptions silently
        }
    }
    
    suspend fun applyModerateCooling() = withContext(Dispatchers.IO) {
        try {
            // Set CPU to ondemand governor
            setCPUFrequency("ondemand", 0)
            
            // Reduce GPU frequency slightly
            reduceGPUFrequencySlightly()
            
        } catch (e: Exception) {
            // Handle exceptions silently
        }
    }
    
    suspend fun stopCooling() = withContext(Dispatchers.IO) {
        try {
            // Restore CPU settings
            setCPUFrequency("interactive", 0)
            
            // Enable all CPU cores
            enableAllCPUCores()
            
            // Restore GPU frequency
            restoreGPUFrequency()
            
            // Disable thermal throttling
            disableThermalThrottling()
            
            // Reset fan speed
            resetFanSpeed()
            
        } catch (e: Exception) {
            // Handle exceptions silently
        }
    }
    
    private fun setCPUFrequency(governor: String, maxFreq: Long) {
        try {
            // Set CPU governor
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo $governor > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"
            ))
            
            // Set max frequency if specified
            if (maxFreq > 0) {
                Runtime.getRuntime().exec(arrayOf(
                    "su", "-c", "echo $maxFreq > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq"
                ))
            }
            
        } catch (e: Exception) {
            // Not rooted or permission denied
        }
    }
    
    private fun disableCPUCores(cores: List<Int>) {
        for (core in cores) {
            try {
                Runtime.getRuntime().exec(arrayOf(
                    "su", "-c", "echo 0 > /sys/devices/system/cpu/cpu$core/online"
                ))
            } catch (e: Exception) {
                // Core not available or permission denied
            }
        }
    }
    
    private fun enableAllCPUCores() {
        val maxCores = Runtime.getRuntime().availableProcessors()
        for (core in 1 until maxCores) {
            try {
                Runtime.getRuntime().exec(arrayOf(
                    "su", "-c", "echo 1 > /sys/devices/system/cpu/cpu$core/online"
                ))
            } catch (e: Exception) {
                // Core not available or permission denied
            }
        }
    }
    
    private fun reduceGPUFrequency() {
        try {
            // Set GPU to minimum frequency
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo 0 > /sys/class/kgsl/kgsl-3d0/max_gpuclk"
            ))
        } catch (e: Exception) {
            // GPU control not available
        }
    }
    
    private fun reduceGPUFrequencyModerately() {
        try {
            // Set GPU to moderate frequency
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo 300000000 > /sys/class/kgsl/kgsl-3d0/max_gpuclk"
            ))
        } catch (e: Exception) {
            // GPU control not available
        }
    }
    
    private fun reduceGPUFrequencySlightly() {
        try {
            // Set GPU to slightly reduced frequency
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo 500000000 > /sys/class/kgsl/kgsl-3d0/max_gpuclk"
            ))
        } catch (e: Exception) {
            // GPU control not available
        }
    }
    
    private fun restoreGPUFrequency() {
        try {
            // Restore GPU to maximum frequency
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo 650000000 > /sys/class/kgsl/kgsl-3d0/max_gpuclk"
            ))
        } catch (e: Exception) {
            // GPU control not available
        }
    }
    
    private fun enableThermalThrottling() {
        try {
            // Enable thermal throttling
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo 1 > /sys/class/thermal/thermal_zone0/mode"
            ))
        } catch (e: Exception) {
            // Thermal control not available
        }
    }
    
    private fun disableThermalThrottling() {
        try {
            // Disable thermal throttling
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo 0 > /sys/class/thermal/thermal_zone0/mode"
            ))
        } catch (e: Exception) {
            // Thermal control not available
        }
    }
    
    private fun increaseFanSpeed() {
        try {
            // Increase fan speed (device specific)
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo 255 > /sys/class/hwmon/hwmon0/pwm1"
            ))
        } catch (e: Exception) {
            // Fan control not available
        }
    }
    
    private fun resetFanSpeed() {
        try {
            // Reset fan speed to automatic
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "echo 2 > /sys/class/hwmon/hwmon0/pwm1_enable"
            ))
        } catch (e: Exception) {
            // Fan control not available
        }
    }
    
    fun getTemperatureHistory(): List<TemperatureInfo> {
        // In a real implementation, you would store temperature history
        return emptyList()
    }
    
    fun getTemperatureThresholds(): Map<String, Int> {
        return mapOf(
            "critical" to 80,
            "high" to 70,
            "warm" to 60,
            "normal" to 50,
            "cool" to 40
        )
    }
    
    fun isTemperatureSafe(): Boolean {
        val thresholds = getTemperatureThresholds()
        val currentTemp = getCPUTemperature()
        return currentTemp < thresholds["critical"]!!
    }
    
    fun getTemperatureReport(): Map<String, Any> {
        return mapOf(
            "cpu_temp" to getCPUTemperature(),
            "battery_temp" to getBatteryTemperature(),
            "monitoring_active" to isMonitoring.get(),
            "thresholds" to getTemperatureThresholds(),
            "is_safe" to isTemperatureSafe()
        )
    }
}
