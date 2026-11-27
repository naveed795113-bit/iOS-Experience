# iOS Experience

An iPhone-inspired Android launcher for Android 10+ devices, with the OnePlus 9 as the initial device-testing target.

## Implemented in the current launcher

- iPhone-inspired home screen with live clock and date
- Installed-app grid with app search and two swipeable pages
- Four-slot bottom dock with long-press app assignment
- App Library with app categories and search
- Built-in wallpaper gallery with six selectable gradient wallpapers
- Android system wallpaper picker shortcut
- 3, 4, or 5-column home-screen grid
- Dark and light appearance modes
- Long-press app shortcuts to Android app information
- Persistent wallpaper, appearance, grid, and dock preferences

## Build

The project uses Android Gradle Plugin 8.7.3, Kotlin 2.0.21, and Java 17. GitHub Actions builds a debug APK on pushes to `main` and manual workflow runs.

## Platform limitations

This is a third-party Android launcher. It can replace the home screen, app grid, dock, and supported launcher surfaces. It cannot fully replace Android's lock screen, notification shade, or system Control Center. Native iOS widgets and Apple's proprietary icon assets are not included.

## Independence

iOS Experience is an independent project inspired by iOS visual patterns. It is not affiliated with or endorsed by Apple.
