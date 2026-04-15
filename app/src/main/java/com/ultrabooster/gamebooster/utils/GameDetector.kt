package com.ultrabooster.gamebooster.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameDetector(private val context: Context) {
    
    private val packageManager = context.packageManager
    
    // Game-related keywords and categories
    private val gameKeywords = setOf(
        "game", "games", "gaming", "play", "player", "arcade", "puzzle", "action",
        "adventure", "rpg", "strategy", "simulation", "sports", "racing", "shooter",
        "fps", "moba", "mmorpg", "minecraft", "pubg", "call", "duty", "fortnite",
        "clash", "clans", "pokemon", "zelda", "mario", "sonic", "dragon", "ball"
    )
    
    private val gameCategories = setOf(
        "GAME",
        "GAME_ACTION",
        "GAME_ADVENTURE",
        "GAME_ARCADE",
        "GAME_BOARD",
        "GAME_CARD",
        "GAME_CASINO",
        "GAME_CASUAL",
        "GAME_EDUCATIONAL",
        "GAME_MUSIC",
        "GAME_PUZZLE",
        "GAME_RACING",
        "GAME_ROLE_PLAYING",
        "GAME_SIMULATION",
        "GAME_SPORTS",
        "GAME_STRATEGY",
        "GAME_TRIVIA",
        "GAME_WORD"
    )
    
    // Known game packages
    private val knownGamePackages = setOf(
        "com.tencent.ig", // PUBG Mobile
        "com.epicgames.fortnite", // Fortnite
        "com.king.candycrushsaga", // Candy Crush
        "com.supercell.clashofclans", // Clash of Clans
        "com.supercell.clashroyale", // Clash Royale
        "com.riotgames.league.wildrift", // League of Legends: Wild Rift
        "com.miHoYo.GenshinImpact", // Genshin Impact
        "com.mojang.minecraftpe", // Minecraft
        "com.ea.fifamobile", // FIFA Mobile
        "com.gameloft.android.ANMP.GloftA8HM", // Asphalt 8
        "com.ea.game.pvz2_row", // Plants vs. Zombies 2
        "com.kiloo.subwaysurfers", // Subway Surfers
        "com.outfit7.talkingtomgoldrun", // Talking Tom Gold Run
        "com.nianticlabs.pokemongo", // Pokémon GO
        "com.zeptolab.ctr2.ccr", // Cut the Rope 2
        "com.rovio.angrybirds", // Angry Birds
        "com.ubisoft.marcusquest", // Assassin's Creed
        "com.activision.callofduty.shooter", // Call of Duty Mobile
        "com.garena.game.gnt", // Garena Free Fire
        "com.netmarble.mhglobal", // Marvel Future Fight
        "com.squareenix.motif", // Final Fantasy
        "com.bandainamcoent.tekkenmobile", // Tekken Mobile
        "com.sega.sonic1px", // Sonic
        "com.nintendo.zara", // Nintendo games
        "com.wb.goog.batman", // Batman games
        "com.spacetime.starlegends", // Star Legends
        "com.ea.games.r3_row", // Real Racing 3
        "com.gameloft.android.GloftDMHM", // Dungeon Hunter
        "com.miniclip.8ballpool", // 8 Ball Pool
        "com.zynga.livepoker", // Zynga Poker
        "com.playrix.gardenscapes", // Gardenscapes
        "com.playrix.homescapes", // Homescapes
        "com.playrix.township", // Township
        "com.king.candycrushsodasaga", // Candy Crush Soda
        "com.king.farmheroessaga", // Farm Heroes Saga
        "com.king.diamonddiaries", // Diamond Diaries
        "com.king.petrescuesaga", // Pet Rescue Saga
        "com.king.bubblewitch3saga" // Bubble Witch 3 Saga
    )
    
    suspend fun getAllGames(): List<GameInfo> = withContext(Dispatchers.IO) {
        val games = mutableListOf<GameInfo>()
        
        try {
            val installedPackages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            
            for (appInfo in installedPackages) {
                if (isGame(appInfo.packageName)) {
                    val gameInfo = getGameInfo(appInfo.packageName)
                    if (gameInfo != null) {
                        games.add(gameInfo)
                    }
                }
            }
            
        } catch (e: Exception) {
            // Handle exceptions silently
        }
        
        games.sortedBy { it.name.lowercase() }
    }
    
    suspend fun getRecentlyPlayedGames(limit: Int = 10): List<GameInfo> = withContext(Dispatchers.IO) {
        val allGames = getAllGames()
        // In a real implementation, you would use UsageStatsManager to get recent usage
        // For now, return all games limited
        allGames.take(limit)
    }
    
    fun isGame(packageName: String): Boolean {
        // Check if it's a known game package
        if (knownGamePackages.contains(packageName)) {
            return true
        }
        
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            
            // Check category
            if (isGameCategory(appInfo)) {
                return true
            }
            
            // Check app name and package name for game keywords
            if (containsGameKeywords(appInfo)) {
                return true
            }
            
            // Check permissions (games often have specific permissions)
            if (hasGamePermissions(appInfo)) {
                return true
            }
            
            // Check if it's a high-performance app
            if (isHighPerformanceApp(appInfo)) {
                return true
            }
            
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }
        
        return false
    }
    
    private fun isGameCategory(appInfo: ApplicationInfo): Boolean {
        return try {
            val category = packageManager.getApplicationInfo(appInfo.packageName, PackageManager.GET_META_DATA)
                .metaData?.getString("android.intent.category.GAME")
            
            category != null || gameCategories.any { cat ->
                packageManager.getApplicationInfo(appInfo.packageName, PackageManager.GET_META_DATA)
                    .metaData?.getString("android.intent.category") == cat
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun containsGameKeywords(appInfo: ApplicationInfo): Boolean {
        val appName = packageManager.getApplicationLabel(appInfo).toString().lowercase()
        val packageName = appInfo.packageName.lowercase()
        
        return gameKeywords.any { keyword ->
            appName.contains(keyword) || packageName.contains(keyword)
        }
    }
    
    private fun hasGamePermissions(appInfo: ApplicationInfo): Boolean {
        return try {
            val permissions = packageManager.getPackageInfo(appInfo.packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions
            
            permissions?.any { permission ->
                permission.contains("game") ||
                permission.contains("vibration") ||
                permission.contains("audio") ||
                permission.contains("camera") ||
                permission.contains("location") ||
                permission.contains("bluetooth")
            } == true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun isHighPerformanceApp(appInfo: ApplicationInfo): Boolean {
        return try {
            // Check if app has high memory requirements
            val memoryClass = packageManager.getApplicationInfo(appInfo.packageName, PackageManager.GET_META_DATA)
                .metaData?.getInt("android.app.required_memory", 0) ?: 0
            
            memoryClass > 50000000 // 50MB+ memory requirement
            
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getGameInfo(packageName: String): GameInfo? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val name = packageManager.getApplicationLabel(appInfo).toString()
            val icon = packageManager.getApplicationIcon(appInfo)
            val versionCode = packageManager.getPackageInfo(packageName, 0).versionCode
            val versionName = packageManager.getPackageInfo(packageName, 0).versionName
            
            GameInfo(
                packageName = packageName,
                name = name,
                icon = icon,
                versionCode = versionCode,
                versionName = versionName ?: "Unknown",
                isKnownGame = knownGamePackages.contains(packageName),
                installTime = packageManager.getPackageInfo(packageName, 0).firstInstallTime,
                updateTime = packageManager.getPackageInfo(packageName, 0).lastUpdateTime
            )
            
        } catch (e: Exception) {
            null
        }
    }
    
    fun addToKnownGames(packageName: String) {
        // In a real implementation, you would save this to SharedPreferences
    }
    
    fun removeFromKnownGames(packageName: String) {
        // In a real implementation, you would remove this from SharedPreferences
    }
    
    data class GameInfo(
        val packageName: String,
        val name: String,
        val icon: android.graphics.drawable.Drawable,
        val versionCode: Int,
        val versionName: String,
        val isKnownGame: Boolean,
        val installTime: Long,
        val updateTime: Long
    )
}
