# Meals AI - Code Structure & Architecture

## 📱 Application Overview

Meals AI is an Android application built with **Kotlin** that helps users **generate**, **plan**, and **manage** their meals using **AI**. The app uses a bottom navigation pattern with multiple fragments and is backed by **Firebase** (Authentication + Firestore) and **OpenAI** (text and vision APIs).

---

## 🛠 Tech Stack Summary

| Layer | Technology |
|-------|------------|
| **Auth** | Firebase Authentication (email/password) |
| **Database** | Cloud Firestore (saved meals, planned meals, user profile) |
| **Meal generation** | OpenAI Chat API (GPT-3.5-turbo) via OkHttp |
| **Image / food scan** | OpenAI Vision API (GPT-4o) via OkHttp |
| **App** | Kotlin, Android SDK, Coroutines, Material Components |

---

## 🚀 Application Startup Flow

### 1. **App Launch → MealsAIApplication → LoginActivity**

- **Application class**: `MealsAIApplication.kt` — initializes **Firebase** before any activity.
- **Entry activity**: `LoginActivity` is the launcher in `AndroidManifest.xml`.
- **Files**:
  - `app/src/main/java/com/mealsai/app/MealsAIApplication.kt`
  - `app/src/main/java/com/mealsai/app/LoginActivity.kt`
  - `app/src/main/res/layout/activity_login.xml`

**On launch:**

1. Firebase is initialized in `MealsAIApplication.onCreate()`.
2. If user is already logged in (`PreferenceManager` + `FirebaseAuthService.isUserLoggedIn()`), app goes directly to **MainActivity**.
3. Otherwise, login screen is shown (email, password, Login, Create Account).

---

## 🔐 Authentication Flow (Firebase)

### **LoginActivity**

- **Login**: `btnLogin` → `handleLogin()`:
  - Validates email/password.
  - Calls **FirebaseAuthService.signIn(email, password)** (coroutine).
  - On success: saves state via **PreferenceManager** (loggedIn, userId, email), then navigates to **MainActivity** and finishes.
  - On failure: shows error toast.

- **Create Account**: `btnCreateAccount` → starts **SignupActivity**.

### **SignupActivity**

- **Create Account**: `btnCreateAccount` → `handleSignup()`:
  1. Validates name, email, password (min 6 chars).
  2. **FirebaseAuthService.signUp(email, password)** creates Firebase Auth user.
  3. **FirestoreService.createUser(userId, user)** saves user profile (name, email, etc.) to Firestore `users` collection.
  4. Saves login state via **PreferenceManager**, then navigates to **MainActivity** and finishes.

- **Login links** (top/bottom): navigate back to **LoginActivity** and finish.

### **MainActivity**

- On create: if user is not logged in (`PreferenceManager` + `FirebaseAuthService`), redirects to **LoginActivity** and finishes.
- **Logout** (e.g. from Generate header): `FirebaseAuthService.signOut()`, clears **PreferenceManager**, then starts **LoginActivity** and finishes.

---

## 🏠 Main Application Flow

### **MainActivity** — Central Hub

- **File**: `app/src/main/java/com/mealsai/app/MainActivity.kt`
- **Layout**: `activity_main.xml`
- **Structure**: Fragment container + **BottomNavigationView** (4 tabs).
- **Default fragment**: **GenerateMealFragment** (loaded in `onCreate`).

---

## 📍 Bottom Navigation

| Tab | Icon | Fragment | File |
|-----|------|----------|------|
| **Generate** (default) | `nav_generate` | GenerateMealFragment | `ui/generate/GenerateMealFragment.kt` |
| **Plan** | `nav_plan` | WeeklyPlanFragment | `WeeklyPlanFragment.kt` |
| **Saved** | `nav_saved` | SavedMealsFragment | `SavedMealsFragment.kt` |
| **Grocery** | `nav_grocery` | GroceryListFragment | `GroceryListFragment.kt` |

Navigation: `MainActivity` → `bottomNavigation.setOnItemSelectedListener` → load corresponding fragment (replacement, no back stack).

---

## 🍽️ Generate Meal Flow (OpenAI API)

### **GenerateMealFragment**

- **Initial state**: Splash with “Generate Meal” button; results section hidden.
- **Generate button**:
  1. Requires logged-in user (**FirebaseAuthService.getCurrentUser()**); otherwise toast “Please login to generate meals”.
  2. Hides splash, shows results area.
  3. Builds prompt via **MealParser.createMealGenerationPrompt()**.
  4. Calls **OpenAIService.generateMeal(prompt)** on IO dispatcher (OpenAI Chat Completions API, GPT-3.5-turbo).
  5. **MealParser.parseMealsFromText(response)** parses AI response into `Meal` list.
  6. Uses parsed meals (up to 6) or **fallback meals** if parse fails or API errors.
  7. Sets **MealAdapter** on RecyclerView; shows “Meals Generated!” or error toast with fallback message.

- **Camera button** (header): starts **CameraActivity** (scan food flow).
- **Logout** (header): delegates to **MainActivity.handleLogout()**.

### **Meal card actions (MealAdapter)**

- **Save**: Uses **MealRepository.addSavedMeal(meal)** (persists to Firestore per user); updates bookmark; toasts “Meal Saved!” / “Already Saved!”.
- **Plan**: Opens plan dialog (date + meal type); on Add → **MealRepository.addPlannedMeal(date, meal, type)** (Firestore); toast “Added to Plan!”.
- **Details**: Opens **MealDetailsFragment** or details dialog (title, description, time, servings, difficulty, tags, nutrition, ingredients, recommendations).

---

## 📷 Camera / Food Scan Flow (OpenAI Vision API)

- **CameraActivity**: Capture or pick image → pass image to **MealScanResultActivity**.
- **MealScanResultActivity**:
  1. Encodes image (e.g. Base64) using **ImageUtils** (compression for API).
  2. Calls **OpenAIVisionService.analyzeFoodImage(imageBase64)** — **OpenAI Vision API (GPT-4o)**.
  3. Parses JSON: meal identification, nutrition, health score, recommendations, ingredients.
  4. Displays analysis (identification, nutrition, health score, recommendations).
  5. User can **save analyzed meal to weekly plan** (same Firestore path as planned meals).

**Services / utils:**

- `OpenAIVisionService.kt` — Vision API client (OkHttp, Bearer token from **BuildConfig.OPENAI_API_KEY**).
- `ImageUtils.kt` — Image encoding/compression for API.

---

## 📅 Weekly Plan Fragment Flow

- **Load**: Day selector (7 days from today) + meals list for selected day.
- **Data**: **MealRepository.getPlannedMealsForDate(date)** (backs with **FirestoreService.getPlannedMeals(userId)** and in-memory cache).
- **Remove planned meal**: **MealRepository.removePlannedMeal(plannedMeal)** (Firestore + cache); list refreshes; toast “Meal removed from plan”.
- **onResume**: Refreshes planned meals for selected day.

---

## 💾 Saved Meals Fragment Flow

- **Load**: **MealRepository.getSavedMeals()** (from Firestore via **FirestoreService.getSavedMeals(userId)** + cache); RecyclerView with **MealAdapter**.
- **onResume**: Reloads saved meals.
- Same card actions: Save/Unsave, Plan, Details.

---

## 🛒 Grocery List Fragment Flow

- **Data**: **MealRepository.getGroceryItems()** — aggregates ingredients from **planned meals cache**, grouped by category (Produce, Meat, Dairy & Eggs, Other).
- **onResume**: Refreshes list when user returns.

---

## 📦 Data Layer

### **MealRepository** (single source of truth)

- **File**: `app/src/main/java/com/mealsai/app/data/MealRepository.kt`
- **Role**: Coordinates **Firebase Auth**, **Firestore**, and in-memory cache. All meal operations go through here.
- **Auth**: Uses **FirebaseAuthService.getCurrentUser()?.uid**; operations that need a user return failure or empty when not logged in.
- **Cache**: `savedMealsCache`, `plannedMealsCache`; **StateFlow**s for reactive updates (`savedMealsFlow`, `plannedMealsFlow`).

**Main methods:**

| Method | Backend | Description |
|--------|---------|-------------|
| `addSavedMeal(meal)` | Firestore | Save meal to user’s `savedMeals` subcollection; update cache & flow. |
| `getSavedMeals()` | Firestore | Load user’s saved meals; update cache and return. |
| `removeSavedMeal(meal)` | Firestore | Remove by meal title; update cache. |
| `addPlannedMeal(date, meal, type)` | Firestore | Add to user’s `plannedMeals`; refresh planned cache. |
| `getPlannedMealsForDate(date)` | Firestore + cache | Load planned meals (via `loadPlannedMeals()`), filter by date. |
| `removePlannedMeal(plannedMeal)` | Firestore | Delete by document id; update cache. |
| `loadPlannedMeals()` | Firestore | Fetches all planned meals for user into cache. |
| `getGroceryItems()` | Local | From `plannedMealsCache`; group ingredients by category. |

### **FirestoreService**

- **File**: `app/src/main/java/com/mealsai/app/services/FirestoreService.kt`
- **Collections**: `users`, and per-user `savedMeals`, `plannedMeals`.
- **Operations**: createUser, getUser, updateUser; saveMeal, getSavedMeals, removeSavedMeal; addPlannedMeal, getPlannedMeals, removePlannedMeal.
- **Data**: Meal ↔ Map conversion for Firestore (title, description, time, calories, servings, tags, difficulty, ingredients).

### **FirebaseAuthService**

- **File**: `app/src/main/java/com/mealsai/app/services/FirebaseAuthService.kt`
- **Methods**: `signUp`, `signIn` (suspend, return `Result`), `signOut`, `getCurrentUser`, `isUserLoggedIn`.

### **Models**

- **Meal** (`model/Meal.kt`): title, description, time, calories, servings, tags, difficulty, ingredients, nutritionDetails, recommendations (Serializable).
- **Ingredient**: name, quantity, category, sourceMealTitle.
- **NutritionDetails**: calories, protein, carbs, fat, fiber, sugars, sodium, cholesterol.
- **Recommendation**: title, description.
- **User** (`model/User.kt`): Firestore user profile (e.g. name, email, id).

---

## 🔌 API Layer

### **OpenAIService** (text / meal generation)

- **File**: `app/src/main/java/com/mealsai/app/services/OpenAIService.kt`
- **Endpoint**: `https://api.openai.com/v1/chat/completions`
- **Model**: GPT-3.5-turbo.
- **Auth**: `Authorization: Bearer <API_KEY>`; API key from **BuildConfig.OPENAI_API_KEY** (e.g. `local.properties`: `OPENAI_API_KEY=...`).
- **Method**: `generateMeal(prompt: String): Result<String>` — returns raw AI text for **MealParser** to parse into meals.

### **OpenAIVisionService** (food image analysis)

- **File**: `app/src/main/java/com/mealsai/app/services/OpenAIVisionService.kt`
- **Endpoint**: Same Chat Completions API with image content.
- **Model**: GPT-4o.
- **Method**: `analyzeFoodImage(imageBase64: String): Result<String>` — returns JSON string (mealIdentification, nutrition, healthScore, recommendations, ingredients).

---

## 🎨 UI Components

- **MealAdapter** (`ui/generate/MealAdapter.kt`): Meal cards; Save, Plan, Details.
- **PlannedMealAdapter** (`ui/plan/PlannedMealAdapter.kt`): Planned meal rows; delete.
- **GroceryAdapter** (`ui/grocery/GroceryAdapter.kt`): Grouped grocery list; check/uncheck.
- **DayAdapter**: Inside **WeeklyPlanFragment**; day selector.
- **MealDetailsFragment**: Full meal details (nutrition, ingredients, recommendations).

---

## 📁 Project Structure

```
app/src/main/
├── java/com/mealsai/app/
│   ├── MealsAIApplication.kt       # Firebase init
│   ├── LoginActivity.kt            # Login (Firebase Auth)
│   ├── SignupActivity.kt           # Sign up (Auth + Firestore user)
│   ├── MainActivity.kt             # Bottom nav, auth check, logout
│   ├── CameraActivity.kt           # Capture/pick image for scan
│   ├── MealScanResultActivity.kt   # Vision API result UI
│   ├── WeeklyPlanFragment.kt
│   ├── SavedMealsFragment.kt
│   ├── GroceryListFragment.kt
│   ├── MealDetailsFragment.kt
│   ├── data/
│   │   └── MealRepository.kt       # Firestore + cache orchestration
│   ├── model/
│   │   ├── Meal.kt
│   │   └── User.kt
│   ├── services/
│   │   ├── FirebaseAuthService.kt
│   │   ├── FirestoreService.kt
│   │   ├── OpenAIService.kt        # Text/meal generation API
│   │   └── OpenAIVisionService.kt # Food image analysis API
│   ├── ui/
│   │   ├── generate/
│   │   │   ├── GenerateMealFragment.kt
│   │   │   └── MealAdapter.kt
│   │   ├── plan/
│   │   │   └── PlannedMealAdapter.kt
│   │   ├── grocery/
│   │   │   └── GroceryAdapter.kt
│   │   └── scan/
│   │       └── MealScanViewModel.kt
│   └── utils/
│       ├── MealParser.kt           # Prompt + parse AI response
│       ├── ImageUtils.kt           # Image for Vision API
│       └── PreferenceManager.kt    # Login state (e.g. logged in, userId)
└── res/
    ├── layout/
    ├── drawable/
    ├── menu/
    └── values/
```

---

## 🔑 Navigation Summary

| From | Action | To |
|------|--------|-----|
| App launch | (not logged in) | LoginActivity |
| App launch | (logged in) | MainActivity |
| LoginActivity | Login success | MainActivity |
| LoginActivity | Create Account | SignupActivity |
| SignupActivity | Create Account success | MainActivity |
| SignupActivity | Login link | LoginActivity |
| MainActivity | Not logged in | LoginActivity |
| MainActivity | Logout | LoginActivity |
| MainActivity | Generate tab | GenerateMealFragment |
| MainActivity | Plan tab | WeeklyPlanFragment |
| MainActivity | Saved tab | SavedMealsFragment |
| MainActivity | Grocery tab | GroceryListFragment |
| GenerateMealFragment | Camera | CameraActivity → MealScanResultActivity |
| Meal card | Save / Plan / Details | Firestore update / Plan dialog / MealDetails |

---

## 📝 Presentation Notes

- **Firebase**: Authentication (email/password) and Firestore (users, saved meals, planned meals) are fully integrated; data is per-user and persistent.
- **OpenAI**: Two APIs — (1) **Chat API** for text-based meal generation, (2) **Vision API** for food image analysis and nutrition.
- **Security**: API key via **BuildConfig** from `local.properties`; Firestore rules should restrict access by `userId` (see `FIREBASE_SECURITY_RULES.md` if present).
- **Offline / errors**: Meal generation falls back to sample meals on API or parse failure; Vision flow can show error or default analysis.
- **State**: Login state is persisted with **PreferenceManager**; Firestore data is cached in **MealRepository** and exposed via **StateFlow** where used.
