package com.ultrabooster.gamebooster.ui.game

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.ultrabooster.gamebooster.databinding.ActivityGameLauncherBinding
import com.ultrabooster.gamebooster.service.GameDetectionService
import com.ultrabooster.gamebooster.utils.BoostManager
import com.ultrabooster.gamebooster.utils.GameDetector
import kotlinx.coroutines.launch

class GameLauncherActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityGameLauncherBinding
    private lateinit var gameAdapter: GameAdapter
    private lateinit var gameDetector: GameDetector
    private lateinit var boostManager: BoostManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        
        binding = ActivityGameLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        gameDetector = GameDetector(this)
        boostManager = BoostManager(this)
        
        setupUI()
        loadGames()
    }
    
    private fun setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // Setup RecyclerView
        gameAdapter = GameAdapter { gameInfo ->
            launchGame(gameInfo.packageName)
        }
        
        binding.recyclerViewGames.apply {
            layoutManager = GridLayoutManager(this@GameLauncherActivity, 3)
            adapter = gameAdapter
        }
        
        // Setup refresh
        binding.swipeRefresh.setOnRefreshListener {
            loadGames()
        }
        
        // Setup search
        binding.editTextSearch.setOnTextChangedListener { text ->
            filterGames(text.toString())
        }
    }
    
    private fun loadGames() {
        binding.swipeRefresh.isRefreshing = true
        binding.progressBar.visibility = View.VISIBLE
        binding.textViewEmpty.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                val games = gameDetector.getAllGames()
                
                runOnUiThread {
                    binding.swipeRefresh.isRefreshing = false
                    binding.progressBar.visibility = View.GONE
                    
                    if (games.isEmpty()) {
                        binding.textViewEmpty.visibility = View.VISIBLE
                        gameAdapter.submitList(emptyList())
                    } else {
                        binding.textViewEmpty.visibility = View.GONE
                        gameAdapter.submitList(games)
                    }
                }
                
            } catch (e: Exception) {
                runOnUiThread {
                    binding.swipeRefresh.isRefreshing = false
                    binding.progressBar.visibility = View.GONE
                    binding.textViewEmpty.visibility = View.VISIBLE
                }
            }
        }
    }
    
    private fun filterGames(query: String) {
        val currentList = gameAdapter.currentList
        val filteredList = if (query.isBlank()) {
            currentList
        } else {
            currentList.filter { game ->
                game.name.contains(query, ignoreCase = true) ||
                game.packageName.contains(query, ignoreCase = true)
            }
        }
        
        gameAdapter.submitList(filteredList)
        
        if (filteredList.isEmpty()) {
            binding.textViewEmpty.visibility = View.VISIBLE
            binding.textViewEmpty.text = "No games found for \"$query\""
        } else {
            binding.textViewEmpty.visibility = View.GONE
        }
    }
    
    private fun launchGame(packageName: String) {
        try {
            // Add to game whitelist
            boostManager.addToGameWhitelist(packageName)
            
            // Launch game with boost
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                // Activate game mode before launching
                activateGameMode(packageName)
                
                // Launch the game
                startActivity(launchIntent)
                
                // Finish this activity
                finish()
            } else {
                // Game not launchable
                showGameNotLaunchable(packageName)
            }
            
        } catch (e: Exception) {
            // Handle launch error
            showLaunchError(packageName)
        }
    }
    
    private fun activateGameMode(packageName: String) {
        try {
            val intent = Intent(this, GameDetectionService::class.java).apply {
                action = "ACTIVATE_GAME_MODE"
                putExtra("package_name", packageName)
            }
            startService(intent)
        } catch (e: Exception) {
            // Service not available
        }
    }
    
    private fun showGameNotLaunchable(packageName: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Game Not Launchable")
            .setMessage("This game cannot be launched directly. You may need to open it from your app drawer.")
            .setPositiveButton("OK") { _, _ -> }
            .show()
    }
    
    private fun showLaunchError(packageName: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Launch Error")
            .setMessage("Failed to launch the game. Please try again.")
            .setPositiveButton("OK") { _, _ -> }
            .show()
    }
    
    fun getGameInfo(packageName: String): GameDetector.GameInfo? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val name = packageManager.getApplicationLabel(appInfo).toString()
            val icon = packageManager.getApplicationIcon(appInfo)
            val versionCode = packageManager.getPackageInfo(packageName, 0).versionCode
            val versionName = packageManager.getPackageInfo(packageName, 0).versionName
            
            GameDetector.GameInfo(
                packageName = packageName,
                name = name,
                icon = icon,
                versionCode = versionCode,
                versionName = versionName ?: "Unknown",
                isKnownGame = true,
                installTime = packageManager.getPackageInfo(packageName, 0).firstInstallTime,
                updateTime = packageManager.getPackageInfo(packageName, 0).lastUpdateTime
            )
        } catch (e: Exception) {
            null
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh games list when activity resumes
        loadGames()
    }
}
