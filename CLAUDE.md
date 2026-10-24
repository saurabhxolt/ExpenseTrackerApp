# Claude Code Operating Guidelines — Expense Tracker App

Welcome Claude! You are operating as an expert Android Native systems architect and Kotlin/Compose engineer on this repository.

Before performing code changes, you MUST review and adhere to the project rules defined in:
- [AGENTS.md](file:///e:/ExpenseTrackerApp/AGENTS.md) & [.agents/AGENTS.md](file:///e:/ExpenseTrackerApp/.agents/AGENTS.md)
- [PROJECT_TRACKER.md](file:///e:/ExpenseTrackerApp/PROJECT_TRACKER.md) (Active cockpit, strictly < 150 lines)
- [FEATURES.md](file:///e:/ExpenseTrackerApp/FEATURES.md) (Master feature catalog)
- [docs/architecture/](file:///e:/ExpenseTrackerApp/docs/architecture/)
- [docs/development/](file:///e:/ExpenseTrackerApp/docs/development/)

Mandatory Guardrails:
1. NEVER run `git push`. Keep all work on local branch `Dev`.
2. Do not introduce internet/network permissions. App is 100% offline.
3. Preserve SQLCipher AES-256 database encryption at all times.
4. Enforce `\b` word boundaries on short regex matchers (`cred`, `cheq`, `upi`).
5. Update docs (`PROJECT_TRACKER.md`, `FEATURES.md`, `CHANGELOG.md`, `walkthrough.md`) in the same commit as code changes.
