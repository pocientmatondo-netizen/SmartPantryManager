# Smart Pantry Manager

A Java Android application that helps reduce food waste. The user tracks the
ingredients they already have at home (their "pantry"), and the app suggests
only recipes they can cook right now, using strictly those ingredients.

## Core feature: strict matching
A recipe is suggested only if EVERY ingredient it requires is in the pantry,
in at least the required quantity. Recipes missing even one ingredient are
not shown.

## Database choice
SQLite (SQLiteOpenHelper) - stored locally on the device. (Reasons to be
completed later.)

## Tech stack
- Java
- Android Studio
- SQLite
- RecyclerView with a custom Adapter

## Setup / Run
1. Clone the repository:
   `git clone https://github.com/pocientmatondo-netizen/SmartPantryManager.git`
2. Open the project in Android Studio.
3. Let Gradle sync finish.
4. Run on an emulator or device (Android 7.0 / API 24 or higher).

## Author
Pocient Matondo - Mobile App Development 700