# Godot Android Health Plugin

A Godot 4.6+ Android plugin that provides native step counting and activity tracking for Android games using on-device sensors (Step Counter & Step Detector).

This plugin completely replaces any previous Google Health Connect implementations with a 100% native, permission-light step sensor approach. It offers an API that exactly mirrors the iOS HealthKit plugin for easy cross-platform Godot integration.

[![GitHub stars](https://img.shields.io/github/stars/SomniGameStudios/godot-healthconnect-plugin.svg?style=social&label=Star)](https://github.com/SomniGameStudios/godot-healthconnect-plugin)

## Features

- **100% Native Sensors** — Uses Android's hardware `TYPE_STEP_COUNTER` and `TYPE_STEP_DETECTOR`. No external apps (like Google Fit or Health Connect) required.
- **Midnight Baseline** — Automatically captures a daily baseline exactly at midnight using Android's native `ACTION_DATE_CHANGED` intent. No battery-draining background workers needed.
- **Cross-Platform Parity** — Method and signal names exactly mirror the `godot-healthkit-plugin` for iOS, meaning you can write your game logic once.
- **Today's Steps Query** — Accurate "midnight to now" steps query out of the box.
- **Historical Data** — Query step counts for previous days.
- **Mock Data in Editor** — When running in the Godot Editor (non-Android), the GDScript wrapper provides realistic mock data so you can test your UI without deploying to a device.

## Quick Start (Pre-built)

1. Download the latest release `.aar` files and the `addons` folder from the Releases page.
2. Place the `addons/healthconnect_plugin` folder into your Godot project's `addons/` directory.
3. Place the `.aar` files into your Android plugins directory (e.g., `addons/AndroidPlugin/bin/`).
4. In Godot, enable the plugin in **Project > Project Settings > Plugins**.
5. In **Project > Export > Android**, ensure your custom export plugin includes the new `.aar` binaries.

## Building from Source

To build the plugin yourself:

```bash
cd platforms/android
./gradlew assemble
```
The built `.aar` files will be available in `platforms/android/healthconnect_plugin/build/outputs/aar/`.

## API Reference

The plugin is exposed via the `HealthConnect` autoload singleton.

### Permissions
- `request_permission()`: Prompts the user for `ACTIVITY_RECOGNITION` permission.
- `get_permission_status()`: Returns the permission status (AuthorizationStatus enum).
- `open_settings()`: Opens the app's settings page in Android OS.
- **Signal**: `permission_result(granted: bool)`

### Observers (Live Tracking)
- `start_step_observer()` / `start_pedometer_observer()`: Starts the background listener for real-time step events.
- `stop_step_observer()` / `stop_pedometer_observer()`: Stops the listener.
- **Signal**: `steps_updated(steps: int)`
- **Signal**: `pedometer_steps_updated(steps: int)`
- **Signal**: `step_detected()`

### Data Queries (Async)
*Note: Queries are asynchronous and return their results via signals.*
- `run_today_steps_query()` -> emits `today_steps_ready(steps: int)`
- `run_total_steps_query()` -> emits `total_steps_ready(steps: int)`
- `run_period_steps_query(days: int)` -> emits `period_steps_ready(steps_dict: Dictionary)`

### Data Getters (Sync)
*Note: These return the last cached value from a query or observer.*
- `get_today_steps() -> int`
- `get_total_steps() -> int`
- `get_period_steps_dict() -> Dictionary`
- `get_live_pedometer_steps() -> int`

## Demo Project
A demo UI exercising the full API is available in `platforms/godot_editor/`. Open this folder as a project in Godot to test the mock data implementation, or export it to Android to test the native sensors.

## License

MIT License. See [LICENSE](LICENSE) for details.

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=SomniGameStudios/godot-healthconnect-plugin&type=Date)](https://star-history.com/#SomniGameStudios/godot-healthconnect-plugin&Date)
