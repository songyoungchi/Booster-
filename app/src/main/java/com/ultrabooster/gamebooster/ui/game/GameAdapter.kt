package com.ultrabooster.gamebooster.ui.game

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ultrabooster.gamebooster.R
import com.ultrabooster.gamebooster.databinding.ItemGameBinding
import com.ultrabooster.gamebooster.utils.GameDetector
import java.text.SimpleDateFormat
import java.util.*

class GameAdapter(
    private val onGameClick: (GameDetector.GameInfo) -> Unit
) : ListAdapter<GameDetector.GameInfo, GameAdapter.GameViewHolder>(GameDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val binding = ItemGameBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GameViewHolder(binding, onGameClick)
    }
    
    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class GameViewHolder(
        private val binding: ItemGameBinding,
        private val onGameClick: (GameDetector.GameInfo) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        
        fun bind(gameInfo: GameDetector.GameInfo) {
            binding.apply {
                // Set game icon
                imageViewGameIcon.setImageDrawable(gameInfo.icon)
                
                // Set game name
                textViewGameName.text = gameInfo.name
                
                // Set package name
                textViewPackageName.text = gameInfo.packageName
                
                // Set version info
                textViewVersion.text = "v${gameInfo.versionName}"
                
                // Set install date
                textViewInstallDate.text = dateFormat.format(Date(gameInfo.installTime))
                
                // Set known game badge
                if (gameInfo.isKnownGame) {
                    imageViewKnownGame.visibility = View.VISIBLE
                } else {
                    imageViewKnownGame.visibility = View.GONE
                }
                
                // Set click listener
                root.setOnClickListener {
                    onGameClick(gameInfo)
                }
                
                // Set long click listener for options
                root.setOnLongClickListener {
                    showGameOptions(gameInfo)
                    true
                }
            }
        }
        
        private fun showGameOptions(gameInfo: GameDetector.GameInfo) {
            val context = binding.root.context
            val options = arrayOf(
                "Launch Game",
                "Game Info",
                "Add to Favorites",
                "Remove from Games"
            )
            
            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle(gameInfo.name)
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> onGameClick(gameInfo)
                        1 -> showGameInfo(gameInfo)
                        2 -> addToFavorites(gameInfo)
                        3 -> removeFromGames(gameInfo)
                    }
                }
                .show()
        }
        
        private fun showGameInfo(gameInfo: GameDetector.GameInfo) {
            val context = binding.root.context
            val info = """
                Game: ${gameInfo.name}
                Package: ${gameInfo.packageName}
                Version: ${gameInfo.versionName} (${gameInfo.versionCode})
                Installed: ${dateFormat.format(Date(gameInfo.installTime))}
                Updated: ${dateFormat.format(Date(gameInfo.updateTime))}
                Known Game: ${if (gameInfo.isKnownGame) "Yes" else "No"}
            """.trimIndent()
            
            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Game Information")
                .setMessage(info)
                .setPositiveButton("OK") { _, _ -> }
                .show()
        }
        
        private fun addToFavorites(gameInfo: GameDetector.GameInfo) {
            // In a real implementation, you would save this to SharedPreferences
            val context = binding.root.context
            android.widget.Toast.makeText(
                context,
                "${gameInfo.name} added to favorites",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
        
        private fun removeFromGames(gameInfo: GameDetector.GameInfo) {
            val context = binding.root.context
            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Remove from Games")
                .setMessage("Are you sure you want to remove ${gameInfo.name} from the games list?")
                .setPositiveButton("Remove") { _, _ ->
                    // In a real implementation, you would remove this from SharedPreferences
                    android.widget.Toast.makeText(
                        context,
                        "${gameInfo.name} removed from games",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                .setNegativeButton("Cancel") { _, _ -> }
                .show()
        }
    }
    
    class GameDiffCallback : DiffUtil.ItemCallback<GameDetector.GameInfo>() {
        override fun areItemsTheSame(
            oldItem: GameDetector.GameInfo,
            newItem: GameDetector.GameInfo
        ): Boolean {
            return oldItem.packageName == newItem.packageName
        }
        
        override fun areContentsTheSame(
            oldItem: GameDetector.GameInfo,
            newItem: GameDetector.GameInfo
        ): Boolean {
            return oldItem == newItem
        }
    }
}
