# Meals AI - Android App

An AI-powered meal planning Android application built with Kotlin.

## Project Setup

This project has been set up with all necessary configuration files to run in Android Studio.

### Prerequisites

- Android Studio (latest version recommended)
- Android SDK (API 24 minimum, API 34 target)
- An Android device connected via USB with USB debugging enabled, OR
- An Android emulator

### How to Run the App

1. **Open the Project in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the project folder (`E:\Meals AI`)
   - Click "OK"

2. **Sync Gradle Files**
   - Android Studio will automatically detect the Gradle files
   - Click "Sync Now" if prompted, or go to `File > Sync Project with Gradle Files`
   - Wait for the sync to complete (this may take a few minutes on first run)

3. **Connect Your Android Device**
   - Enable Developer Options on your Android device:
     - Go to Settings > About Phone
     - Tap "Build Number" 7 times
   - Enable USB Debugging:
     - Go to Settings > Developer Options
     - Enable "USB Debugging"
   - Connect your device via USB
   - Accept the USB debugging prompt on your device

4. **Verify Device Connection**
   - In Android Studio, check the device dropdown (top toolbar)
   - Your device should appear in the list
   - If not visible, click the device dropdown and select your device

5. **Run the App**
   - Click the green "Run" button (▶) in the toolbar, OR
   - Press `Shift + F10` (Windows/Linux) or `Ctrl + R` (Mac), OR
   - Go to `Run > Run 'app'`
   - Select your connected device when prompted
   - The app will build and install on your device

### Project Structure

```
app/
├── src/
│   └── main/
│       ├── java/com/mealsai/app/
│       │   ├── LoginActivity.kt          # Login screen
│       │   ├── SignupActivity.kt          # Signup screen
│       │   ├── MainActivity.kt            # Main activity with bottom navigation
│       │   ├── WeeklyPlanFragment.kt      # Weekly meal plan
│       │   ├── SavedMealsFragment.kt     # Saved meals
│       │   ├── GroceryListFragment.kt    # Grocery list
│       │   ├── MealDetailsFragment.kt     # Meal details
│       │   └── ui/generate/
│       │       └── GenerateMealFragment.kt  # AI meal generator
│       ├── res/
│       │   ├── layout/                    # XML layouts
│       │   ├── drawable/                  # Icons and drawables
│       │   ├── values/                    # Strings, styles, etc.
│       │   └── menu/                      # Menu resources
│       └── AndroidManifest.xml
└── build.gradle                           # App-level build config
```

### Features

- **Login/Signup**: User authentication screens
- **AI Meal Generator**: Generate personalized meal recommendations
- **Weekly Meal Plan**: View and manage weekly meal plans
- **Saved Meals**: Browse saved meal recipes
- **Grocery List**: Manage shopping lists

### Branch Strategy

Development is organized by feature branches. **main** holds the full app; each feature has its own branch for isolated work:

| Branch | Purpose |
|--------|---------|
| **main** | Full app (source of truth) |
| **feature/login-signup** | Login and sign-up UI + Firebase Auth |
| **feature/account-management** | User profile (Firestore user CRUD, profile/settings) |
| **feature/ai-capture** | Camera + food scan (Vision API) |
| **feature/recommendation-generation** | Text-based AI meal generation |
| **feature/weekly-plan** | Weekly meal plan tab |
| **feature/saved-meals-grocery** | Saved meals + grocery list tabs |

Work on a feature in its branch, then merge into **main** when ready.

### Dependencies

The project uses:
- AndroidX libraries
- Material Design Components
- Kotlin
- ConstraintLayout
- RecyclerView
- Navigation Components

### Troubleshooting

**Issue: "Gradle sync failed"**
- Make sure you have internet connection
- Try: `File > Invalidate Caches / Restart`

**Issue: "Device not detected"**
- Check USB debugging is enabled
- Try different USB cable/port
- Install device drivers if needed
- Run: `adb devices` in terminal to verify connection

**Issue: "Build failed"**
- Clean project: `Build > Clean Project`
- Rebuild: `Build > Rebuild Project`
- Check that all dependencies are downloaded

**Issue: "App crashes on launch"**
- Check Logcat for error messages
- Verify all required resources exist
- Make sure device meets minimum SDK requirements (API 24)

### Next Steps

The app currently has basic UI and navigation. You'll need to implement:
- Backend API integration for meal generation
- Database for saving meals
- User authentication logic
- Meal generation algorithms
- Data models and ViewModels

### Notes

- The app uses Material Design 3 components
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- Built with Kotlin
