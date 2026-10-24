# GitHub Copilot Instructions — Expense Tracker App

When generating code, explanations, or pull requests for this repository:
1. Adhere to [AGENTS.md](file:///e:/ExpenseTrackerApp/AGENTS.md) and [.agents/AGENTS.md](file:///e:/ExpenseTrackerApp/.agents/AGENTS.md).
2. Tech Stack: Android Native, Kotlin 1.9, Jetpack Compose Material3, Room 2.6 with SQLCipher AES-256, Hilt.
3. Offline Only: Never add `android.permission.INTERNET`. Zero network calls.
4. Parsing Engine: Always apply word boundary protection (`\b`) to short service patterns (`\bcred\b`, `\bcheq\b`, `\bupi\b`).
5. Testing: Ensure all Kotlin code is covered by unit tests in `app/src/test`.
