package com.ultrabooster.gamebooster.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Choreographer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class FPSMonitor(private val context: Context) : Choreographer.FrameCallback {
    
    private val choreographer = Choreographer.getInstance()
    private val handler = Handler(Looper.getMainLooper())
    private val isMonitoring = AtomicBoolean(false)
    private val frameCount = AtomicInteger(0)
    private val lastFrameTime = AtomicLong(0)
    private val lastUpdateTime = AtomicLong(0)
    
    // FPS calculation variables
    private val fpsBuffer = mutableListOf<Long>()
    private val maxBufferSize = 60 // Keep last 60 frames for smoothing
    private var averageFPS = 0.0
    private var minFPS = Int.MAX_VALUE
    private var maxFPS = 0
    private var droppedFrames = 0
    
    // Performance optimization
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var gyroscopeSensor: Sensor? = null
    private var gyroscopeListener: GyroscopeListener? = null
    
    data class FPSInfo(
        val currentFPS: Int,
        val averageFPS: Double,
        val minFPS: Int,
        val maxFPS: Int,
        val droppedFrames: Int,
        val isStable: Boolean,
        val frameTime: Long
    )
    
    private inner class GyroscopeListener : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            // Use gyroscope data to detect device movement and adjust FPS expectations
            event?.let {
                val movementIntensity = kotlin.math.sqrt(
                    it.values[0] * it.values[0] +
                    it.values[1] * it.values[1] +
                    it.values[2] * it.values[2]
                )
                
                // Adjust FPS targets based on movement
                if (movementIntensity > 2.0) {
                    // Device is moving rapidly, expect lower FPS
                }
            }
        }
        
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Handle accuracy changes
        }
    }
    
    fun startMonitoring() {
        if (isMonitoring.compareAndSet(false, true)) {
            resetCounters()
            choreographer.postFrameCallback(this)
            startGyroscopeMonitoring()
        }
    }
    
    fun stopMonitoring() {
        if (isMonitoring.compareAndSet(true, false)) {
            choreographer.removeFrameCallback(this)
            stopGyroscopeMonitoring()
        }
    }
    
    fun getCurrentFPS(): FPSInfo {
        val currentTime = System.currentTimeMillis()
        val frameTime = currentTime - lastUpdateTime.get()
        
        // Calculate current FPS
        val currentFPS = if (frameTime > 0) {
            (1000.0 / frameTime).toInt()
        } else {
            60 // Default to 60 FPS
        }
        
        // Update min/max
        minFPS = minOf(minFPS, currentFPS)
        maxFPS = maxOf(maxFPS, currentFPS)
        
        // Check stability
        val isStable = fpsBuffer.size >= 10 && 
                      fpsBuffer.average() > 45.0 && 
                      fpsBuffer.standardDeviation() < 10.0
        
        return FPSInfo(
            currentFPS = currentFPS,
            averageFPS = averageFPS,
            minFPS = if (minFPS == Int.MAX_VALUE) 0 else minFPS,
            maxFPS = maxFPS,
            droppedFrames = droppedFrames,
            isStable = isStable,
            frameTime = frameTime
        )
    }
    
    override fun doFrame(frameTimeNanos: Long) {
        if (!isMonitoring.get()) {
            return
        }
        
        val currentTime = System.currentTimeMillis()
        val frameTimeMs = currentTime - lastFrameTime.get()
        
        // Update counters
        frameCount.incrementAndGet()
        lastFrameTime.set(currentTime)
        
        // Calculate FPS
        if (frameTimeMs > 0) {
            val fps = 1000.0 / frameTimeMs
            fpsBuffer.add(fps.toLong())
            
            // Keep buffer size limited
            if (fpsBuffer.size > maxBufferSize) {
                fpsBuffer.removeAt(0)
            }
            
            // Update average FPS
            averageFPS = fpsBuffer.average()
            
            // Detect dropped frames
            if (frameTimeMs > 33) { // More than 33ms means dropped frame (below 30 FPS)
                droppedFrames++
            }
        }
        
        // Update every second
        if (currentTime - lastUpdateTime.get() >= 1000) {
            lastUpdateTime.set(currentTime)
            updatePerformanceStats()
        }
        
        // Continue monitoring
        choreographer.postFrameCallback(this)
    }
    
    private fun startGyroscopeMonitoring() {
        try {
            gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            gyroscopeListener = GyroscopeListener()
            
            gyroscopeSensor?.let { sensor ->
                sensorManager.registerListener(
                    gyroscopeListener,
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }
        } catch (e: Exception) {
            // Gyroscope not available
        }
    }
    
    private fun stopGyroscopeMonitoring() {
        try {
            gyroscopeListener?.let { listener ->
                sensorManager.unregisterListener(listener)
            }
            gyroscopeListener = null
        } catch (e: Exception) {
            // Handle exceptions silently
        }
    }
    
    private fun updatePerformanceStats() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Optimize based on current FPS
                val fpsInfo = getCurrentFPS()
                
                if (fpsInfo.currentFPS < 30) {
                    // Low FPS detected, apply optimizations
                    applyLowFPSOptimizations()
                } else if (fpsInfo.currentFPS < 45) {
                    // Medium FPS, apply moderate optimizations
                    applyMediumFPSOptimizations()
                } else if (fpsInfo.currentFPS > 60) {
                    // High FPS, can reduce some optimizations for battery
                    applyHighFPSOptimizations()
                }
                
            } catch (e: Exception) {
                // Handle exceptions silently
            }
        }
    }
    
    private fun applyLowFPSOptimizations() {
        try {
            // Aggressive optimizations for very low FPS
            val boostManager = BoostManager(context)
            
            // Clear RAM
            boostManager.clearRAM()
            
            // Optimize CPU
            boostManager.optimizeCPU()
            
            // Disable animations completely
            disableAnimations()
            
            // Limit background processes
            limitBackgroundProcesses()
            
        } catch (e: Exception) {
            // Handle exceptions silently
        }
    }
    
    private fun applyMediumFPSOptimizations() {
        try {
            // Moderate optimizations
            val boostManager = BoostManager(context)
            
            // Light RAM cleanup
            boostManager.clearRAM()
            
            // Reduce animation scale
            reduceAnimations()
            
        } catch (e: Exception) {
            // Handle exceptions silently
        }
    }
    
    private fun applyHighFPSOptimizations() {
        try {
            // Reduce optimizations for better battery life when FPS is good
            restoreAnimations()
            
        } catch (e: Exception) {
            // Handle exceptions silently
        }
    }
    
    private fun disableAnimations() {
        try {
            android.provider.Settings.Global.putFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                0.0f
            )
            android.provider.Settings.Global.putFloat(
                context.contentResolver,
                android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE,
                0.0f
            )
            android.provider.Settings.Global.putFloat(
                context.contentResolver,
                android.provider.Settings.Global.WINDOW_ANIMATION_SCALE,
                0.0f
            )
        } catch (e: Exception) {
            // Permission denied
        }
    }
    
    private fun reduceAnimations() {
        try {
            android.provider.Settings.Global.putFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                0.5f
            )
            android.provider.Settings.Global.putFloat(
                context.contentResolver,
                android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE,
                0.5f
            )
            android.provider.Settings.Global.putFloat(
                context.contentResolver,
                android.provider.Settings.Global.WINDOW_ANIMATION_SCALE,
                0.5f
            )
        } catch (e: Exception) {
            // Permission denied
        }
    }
    
    private fun restoreAnimations() {
        try {
            android.provider.Settings.Global.putFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            android.provider.Settings.Global.putFloat(
                context.contentResolver,
                android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE,
                1.0f
            )
            android.provider.Settings.Global.putFloat(
                context.contentResolver,
                android.provider.Settings.Global.WINDOW_ANIMATION_SCALE,
                1.0f
            )
        } catch (e: Exception) {
            // Permission denied
        }
    }
    
    private fun limitBackgroundProcesses() {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            
            // Set background process limit to minimum
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                activityManager.isLowRamMode = true
            }
            
        } catch (e: Exception) {
            // Permission denied or not supported
        }
    }
    
    private fun resetCounters() {
        frameCount.set(0)
        lastFrameTime.set(System.currentTimeMillis())
        lastUpdateTime.set(System.currentTimeMillis())
        fpsBuffer.clear()
        averageFPS = 0.0
        minFPS = Int.MAX_VALUE
        maxFPS = 0
        droppedFrames = 0
    }
    
    fun getPerformanceReport(): Map<String, Any> {
        return mapOf(
            "average_fps" to averageFPS,
            "min_fps" to (if (minFPS == Int.MAX_VALUE) 0 else minFPS),
            "max_fps" to maxFPS,
            "dropped_frames" to droppedFrames,
            "total_frames" to frameCount.get(),
            "monitoring_active" to isMonitoring.get()
        )
    }
    
    // Extension function for standard deviation
    private fun List<Long>.standardDeviation(): Double {
        if (size < 2) return 0.0
        
        val mean = average()
        val variance = map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
}
