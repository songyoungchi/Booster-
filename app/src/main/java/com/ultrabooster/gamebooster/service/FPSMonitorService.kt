package com.ultrabooster.gamebooster.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.ultrabooster.gamebooster.R
import com.ultrabooster.gamebooster.utils.FPSMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FPSMonitorService : Service() {
    
    private lateinit var fpsMonitor: FPSMonitor
    private lateinit var windowManager: WindowManager
    private var fpsOverlay: View? = null
    private var fpsTextView: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    
    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_UPDATE_POSITION = "ACTION_UPDATE_POSITION"
        const val EXTRA_X = "EXTRA_X"
        const val EXTRA_Y = "EXTRA_Y"
    }
    
    override fun onCreate() {
        super.onCreate()
        fpsMonitor = FPSMonitor(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startFPSMonitoring()
            ACTION_STOP -> stopFPSMonitoring()
            ACTION_UPDATE_POSITION -> updateOverlayPosition(intent)
        }
        return START_STICKY
    }
    
    private fun startFPSMonitoring() {
        try {
            createFPSOverlay()
            startFPSUpdates()
            fpsMonitor.startMonitoring()
        } catch (e: Exception) {
            stopSelf()
        }
    }
    
    private fun stopFPSMonitoring() {
        try {
            fpsMonitor.stopMonitoring()
            stopFPSUpdates()
            removeFPSOverlay()
            stopSelf()
        } catch (e: Exception) {
            stopSelf()
        }
    }
    
    private fun createFPSOverlay() {
        try {
            val layoutInflater = LayoutInflater.from(this)
            fpsOverlay = layoutInflater.inflate(R.layout.fps_overlay, null)
            fpsTextView = fpsOverlay?.findViewById(R.id.tv_fps)
            
            val layoutParams = WindowManager.LayoutParams().apply {
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_PHONE
                }
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 100
            }
            
            windowManager.addView(fpsOverlay, layoutParams)
            
            // Setup touch handling for dragging
            setupTouchHandling()
            
        } catch (e: Exception) {
            // Permission denied or overlay not allowed
        }
    }
    
    private fun setupTouchHandling() {
        fpsOverlay?.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    // Handle touch down
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    // Handle drag
                    updateOverlayPosition(event.rawX.toInt(), event.rawY.toInt())
                    true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    // Handle touch up
                    true
                }
                else -> false
            }
        }
    }
    
    private fun updateOverlayPosition(x: Int, y: Int) {
        try {
            fpsOverlay?.let { overlay ->
                val layoutParams = overlay.layoutParams as WindowManager.LayoutParams
                layoutParams.x = x
                layoutParams.y = y
                windowManager.updateViewLayout(overlay, layoutParams)
            }
        } catch (e: Exception) {
            // Handle exceptions silently
        }
    }
    
    private fun updateOverlayPosition(intent: Intent) {
        val x = intent.getIntExtra(EXTRA_X, 0)
        val y = intent.getIntExtra(EXTRA_Y, 0)
        updateOverlayPosition(x, y)
    }
    
    private fun startFPSUpdates() {
        updateRunnable = object : Runnable {
            override fun run() {
                serviceScope.launch {
                    try {
                        val fpsInfo = fpsMonitor.getCurrentFPS()
                        updateFPSDisplay(fpsInfo)
                    } catch (e: Exception) {
                        // Handle exceptions silently
                    }
                }
                handler.postDelayed(this, 500) // Update every 500ms
            }
        }
        handler.post(updateRunnable!!)
    }
    
    private fun stopFPSUpdates() {
        updateRunnable?.let { handler.removeCallbacks(it) }
        updateRunnable = null
    }
    
    private fun updateFPSDisplay(fpsInfo: FPSMonitor.FPSInfo) {
        fpsTextView?.let { textView ->
            val fpsText = "${fpsInfo.currentFPS} FPS"
            textView.text = fpsText
            
            // Color code based on FPS
            val color = when {
                fpsInfo.currentFPS >= 60 -> getColor(R.color.success)
                fpsInfo.currentFPS >= 45 -> getColor(R.color.warning)
                fpsInfo.currentFPS >= 30 -> getColor(R.color.info)
                else -> getColor(R.color.danger)
            }
            textView.setTextColor(color)
            
            // Update background based on performance
            val background = when {
                fpsInfo.isStable -> getColor(android.R.color.transparent)
                fpsInfo.currentFPS < 30 -> getColor(R.color.danger).let { it and 0x44000000 }
                else -> getColor(android.R.color.transparent)
            }
            textView.setBackgroundColor(background)
        }
    }
    
    private fun removeFPSOverlay() {
        try {
            fpsOverlay?.let { overlay ->
                windowManager.removeView(overlay)
                fpsOverlay = null
                fpsTextView = null
            }
        } catch (e: Exception) {
            // View already removed or never added
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopFPSMonitoring()
        serviceScope.cancel()
    }
    
    private fun getColor(resId: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getColor(resId)
        } else {
            resources.getColor(resId)
        }
    }
}
