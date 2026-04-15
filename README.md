# Ultra Game Booster

A fully functional Android application that improves gaming performance across all installed apps by optimizing CPU, RAM, GPU load, and network latency.

## Features

### Core Features
- **One-Tap Boost**: Instant performance optimization with RAM cleanup, cache clearing, and service optimization
- **Game Mode Detection**: Automatically detects when games are launched and activates boost mode
- **FPS Stabilization**: Real-time FPS monitoring and optimization
- **Network Optimizer**: DNS optimization, ping reduction, and background network blocking
- **Temperature Control**: Monitors device temperature and applies cooling measures
- **Battery Optimization**: Three modes - Performance, Balanced, and Battery Saver
- **Game Launcher**: Lists all installed games with launch-through-booster functionality

### Advanced Features
- **Material Design 3**: Modern UI with dark mode by default
- **Real-time Monitoring**: Live stats for RAM, CPU, temperature, and network ping
- **Floating Overlay**: Optional floating boost button and FPS counter
- **Auto Boost Scheduler**: Scheduled performance optimization
- **Game-Specific Profiles**: Custom settings for individual games
- **Root Support**: Enhanced features for rooted devices (optional)

## Requirements

- **Minimum SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 14 (API 34)
- **Language**: Kotlin
- **Architecture**: MVVM with Coroutines

## Permissions Required

### Essential Permissions
- `ACCESSIBILITY_SERVICE` - For automatic game detection
- `PACKAGE_USAGE_STATS` - For app usage monitoring
- `SYSTEM_ALERT_WINDOW` - For floating overlays

### Optional Permissions
- `KILL_BACKGROUND_PROCESSES` - For RAM cleanup
- `CLEAR_APP_CACHE` - For cache clearing
- `WRITE_SETTINGS` - For system optimization
- `INTERNET` - For network optimization
- `BATTERY_STATS` - For battery monitoring

## Installation

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK (API 24-34)
- Kotlin 1.9.10+

### Build Instructions

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd UltraGameBooster
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the project directory

3. **Sync Gradle**
   - Wait for Gradle to sync automatically
   - If sync fails, try: `File > Sync Project with Gradle Files`

4. **Build the APK**
   - Select `Build > Build Bundle(s) / APK(s) > Build APK(s)`
   - The APK will be generated in `app/build/outputs/apk/debug/`

5. **Run on Device**
   - Connect your Android device
   - Enable USB debugging
   - Click the Run button in Android Studio

### Manual APK Build

1. **Using Gradle**
   ```bash
   ./gradlew assembleDebug
   ```

2. **Release Build**
   ```bash
   ./gradlew assembleRelease
   ```

3. **Signed APK**
   - Generate a keystore: `keytool -genkey -v -keystore release-key.keystore -alias release -keyalg RSA -keysize 2048 -validity 10000`
   - Configure signing in `app/build.gradle`
   - Run: `./gradlew assembleRelease`

## Usage Guide

### First Time Setup
1. Install the app
2. Grant required permissions when prompted
3. Enable Accessibility Service for automatic game detection
4. Grant Overlay Permission for floating features

### Main Features

#### One-Tap Boost
1. Open the app
2. Tap the "BOOST NOW" button
3. Wait for optimization to complete
4. View before/after stats

#### Game Mode
1. Enable Game Mode toggle
2. Launch any game
3. App automatically detects and optimizes
4. FPS counter appears (if enabled)

#### Game Launcher
1. Go to Games tab
2. Browse installed games
3. Tap any game to launch with boost
4. Long-press for additional options

#### Settings
1. Access Settings tab
2. Configure boost modes
3. Set up auto-schedule
4. Customize notifications

## Technical Details

### Architecture
- **MVVM Pattern**: Clean separation of concerns
- **Coroutines**: Asynchronous operations
- **LiveData**: Reactive UI updates
- **Dependency Injection**: Manual DI setup

### Key Components

#### Core Services
- `GameDetectionService`: Accessibility service for game detection
- `FPSMonitorService`: Real-time FPS monitoring
- `TemperatureMonitorService`: Temperature monitoring and cooling
- `FloatingWindowService`: Overlay management

#### Utility Classes
- `PerformanceMonitor`: System performance monitoring
- `BoostManager`: Performance optimization logic
- `NetworkOptimizer`: Network optimization
- `BatteryOptimizer`: Battery management
- `GameDetector`: Game detection algorithms

#### UI Components
- `MainActivity`: Main dashboard
- `GameLauncherActivity`: Game list and launcher
- `GameAdapter`: RecyclerView adapter for games

### Performance Optimizations

#### CPU Optimization
- Dynamic CPU governor adjustment
- Core management based on load
- Thermal throttling control

#### Memory Management
- Background process cleanup
- Cache clearing
- Memory pressure monitoring

#### Network Optimization
- Fast DNS server selection
- TCP parameter tuning
- Background traffic blocking

#### Temperature Management
- Real-time temperature monitoring
- Automatic cooling measures
- Performance throttling when overheating

## Safety and Limitations

### Safety Measures
- Never kills system-critical apps
- Preserves essential Android services
- Safe temperature thresholds
- Backup and restore functionality

### Limitations
- Some features require root access
- FPS monitoring may not work on all devices
- Temperature accuracy varies by device
- Network optimization limited by Android permissions

### Google Play Compliance
- Follows all Google Play policies
- No misleading performance claims
- Transparent permission usage
- Privacy-compliant data handling

## Troubleshooting

### Common Issues

#### Accessibility Service Not Working
1. Go to Settings > Accessibility
2. Find "Ultra Game Booster"
3. Enable the service
4. Restart the app

#### Overlay Permission Denied
1. Go to Settings > Apps > Special Access
2. Find "Display over other apps"
3. Enable for Ultra Game Booster

#### Game Detection Not Working
1. Ensure Accessibility Service is enabled
2. Check if game is in the whitelist
3. Try manual game detection

#### High Temperature Warnings
1. Check device ventilation
2. Close background apps
3. Enable battery saver mode
4. Reduce gaming sessions

### Performance Issues
1. Clear app cache
2. Restart the app
3. Check available storage
4. Update to latest version

## Development

### Contributing
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable names
- Add comments for complex logic
- Keep methods small and focused

### Testing
- Unit tests for core logic
- Integration tests for services
- UI tests for critical flows
- Performance testing

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and feature requests:
- Create an issue on GitHub
- Check the troubleshooting section
- Review the documentation

## Changelog

### Version 1.0.0
- Initial release
- Core boost functionality
- Game detection
- FPS monitoring
- Network optimization
- Temperature control
- Battery management
- Game launcher
- Material Design 3 UI

---

**Note**: This app is designed for entertainment and optimization purposes. Performance improvements may vary based on device specifications and usage patterns.
