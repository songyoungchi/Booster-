package com.ultrabooster.gamebooster.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import com.ultrabooster.gamebooster.R
import com.ultrabooster.gamebooster.utils.BoostManager

class FloatingWindowService : Service() {
    
    private lateinit var windowManager: WindowManager
    private var floatingButton: View? = null
    private var boostManager: BoostManager? = null
    
    companion object {
        const val ACTION_SHOW = "ACTION_SHOW"
        const val ACTION_HIDE = "ACTION_HIDE"
        const val EXTRA_GAME_PACKAGE = "EXTRA_GAME_PACKAGE"
    }
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        boostManager = BoostManager(this)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showFloatingButton()
            ACTION_HIDE -> hideFloatingButton()
        }
        return START_STICKY
    }
    
    private fun showFloatingButton() {
        if (floatingButton != null) return
        
        try {
            floatingButton = createFloatingButton()
            
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
                x = 100
                y = 200
            }
            
            windowManager.addView(floatingButton, layoutParams)
            
        } catch (e: Exception) {
            // Permission denied or overlay not allowed
        }
    }
    
    private fun hideFloatingButton() {
        try {
            floatingButton?.let { button ->
                windowManager.removeView(button)
                floatingButton = null
            }
        } catch (e: Exception) {
            // View already removed
        }
    }
    
    private fun createFloatingButton(): View {
        val inflater = LayoutInflater.from(this)
        val buttonView = inflater.inflate(R.layout.floating_boost_button, null)
        
        val boostButton = buttonView.findViewById<ImageView>(R.id.btn_boost)
        val closeButton = buttonView.findViewById<ImageView>(R.id.btn_close)
        
        // Boost button click
        boostButton.setOnClickListener {
            performQuickBoost()
        }
        
        // Close button click
        closeButton.setOnClickListener {
            hideFloatingButton()
            stopSelf()
        }
        
        // Touch handling for dragging
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        
        buttonView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = (view.layoutParams as WindowManager.LayoutParams).x
                    initialY = (view.layoutParams as WindowManager.LayoutParams).y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val layoutParams = view.layoutParams as WindowManager.LayoutParams
                    layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(view, layoutParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Handle touch up
                    true
                }
                else -> false
            }
        }
        
        return buttonView
    }
    
    private fun performQuickBoost() {
        try {
            boostManager?.let { manager ->
                // Perform quick boost operations
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    manager.clearRAM()
                    manager.optimizeNetwork()
                }
                
                // Show visual feedback
                showBoostFeedback()
            }
        } catch (e: Exception) {
            // Handle exceptions silently
        }
    }
    
    private fun showBoostFeedback() {
        // Could show a small toast or animation
        try {
            floatingButton?.let { button ->
                // Animate the button to show feedback
                button.animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(100)
                    .withEndAction {
                        button.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start()
                    }
                    .start()
            }
        } catch (e: Exception) {
            // Animation failed
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        hideFloatingButton()
    }
}
