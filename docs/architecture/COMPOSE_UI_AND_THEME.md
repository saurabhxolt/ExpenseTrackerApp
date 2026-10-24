# 🎨 Jetpack Compose UI & Theme Architecture

> **Rule**: Keep under 80 lines. UI tokens in [Theme.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/core/ui/theme/Theme.kt).

## 1. Material3 Design System & Dynamic Theming
- Built exclusively with Jetpack Compose Material3 (`androidx.compose.material3`).
- Dynamic Color support enabled for Android 12+ (Material You) with curated dark/light fallback schemes.
- **Contrast Integrity**: Tested against WCAG AA standards in both Dark and Light modes.
- Gradient fills: Subtle mesh gradients used for hero cashflow cards (`0xFF0F2942` for positive, `0xFF3B1E2B` for negative in Dark theme).

## 2. Unidirectional Data Flow (UDF) & MVI/MVVM
- ViewModels expose immutable `StateFlow<ScreenUiState>` to Composables.
- Composables emit user intents/events upwards via lambdas (`onAction`, `onFilterChanged`, `onDelete`).
- ViewModels launch coroutines within `viewModelScope` and perform repository operations asynchronously.

## 3. Component Standards & Layout Rules
- **44dp Category Avatars**: Circular container avatars with category emoji icons (`CategoryEmojiProvider.kt`).
- **Amount Pill Badges**: Styled soft container chips (`-₹1,250.00` in crimson, `+₹1,173.00` in emerald).
- **FAB Collision Protection**: All scrollable lists (`LazyColumn`) MUST provide generous bottom padding (`140.dp` to `160.dp`) and a bottom spacer item (`100.dp`) to prevent Floating Action Button overlap.
- **System Back Handling**: Use Compose `BackHandler` on all sub-views/settings to ensure intuitive back navigation to Dashboard.
