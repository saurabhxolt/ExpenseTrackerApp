You are an expert Android Native systems architect, senior Kotlin/Jetpack Compose lead engineer, and offline privacy & security auditor working with me on this repository.

Before writing feature code, you must initialize our project documentation, security guardrails, and governance framework following these strict engineering standards:

1. LEAN ACTIVE TRACKER (< 150 lines):
   - Create `PROJECT_TRACKER.md`. It must serve as our active cockpit: active sprint tasks, architecture subsystem health table, Gradle build commands (`assembleDebug`, `test`), and 1-click device/emulator launcher instructions.
   - It must NEVER exceed 150 lines. As tasks are completed, move detailed logs to `CHANGELOG.md` to prevent AI context/token bloat.

2. HISTORICAL CHANGELOG ARCHIVE:
   - Create `CHANGELOG.md` to store our permanent chronological engineering history, past refactors, schema migrations, and resolved bug fixes following the Keep a Changelog standard.

3. MASTER FEATURE CATALOG:
   - Create `FEATURES.md`. Document 100% of all user-facing capabilities across each subsystem persona (e.g., Personal Finance End-User, Autonomous Ingestion Daemon, Privacy & Security Guardian, Storage & Export Engine).
   - Any time a feature, DAO, parser, or screen is created, changed, or removed, `FEATURES.md` must be updated in the same commit.

4. MODULAR ARCHITECTURE CHEATSHEETS (< 80 lines each):
   - In `docs/architecture/`, create concise technical guides for each core subsystem:
     - `SMS_INGESTION_AND_NLP.md`: Dual-channel broadcast receiver & notification listener, regex matching, word boundaries (`\b`), noise rejection, and merchant classification memory.
     - `DATABASE_AND_SQLCIPHER.md`: Room entities, DAOs, relations, SQLCipher AES-256 Android Keystore integration, and schema migrations.
     - `COMPOSE_UI_AND_THEME.md`: Material3 design system, color tokens, typography, dark/light contrast, and MVI/MVVM StateFlow rendering.
     - `SECURITY_AND_BIOMETRICS.md`: Zero network permissions (`android.permission.INTERNET` forbidden), BiometricPrompt fallback, and SAF storage sandboxing.
   - Create `PLATFORM_ARCHITECTURE.md` with visual Mermaid diagrams illustrating system tiers (Presentation -> Domain -> Data), dual-channel ingestion flow, and database ERD.

5. TEAM ONBOARDING & QA TESTING PLAYBOOKS:
   - In `docs/development/`, create:
     - `DEVELOPER_ONBOARDING.md`: Day-1 setup checklist (JDK 17, Android SDK 34, Gradle wrapper), 1-click build commands, folder tour, and Kotlin coding conventions.
     - `QA_TESTING_PLAYBOOK.md`: Unit test suite commands, SMS test vectors (HDFC, SBI, ICICI, Axis, Kotak, CRED, UPI), ADB SMS emulation (`adb emu sms send`), mock notification tests, biometric fallback edge cases, and zero false positive validation.
     - `INGESTION_CONTRACTS.md`: Core data models (`ParsedTransaction`, SMS PDU extras, Notification bundle extras), category classification rules, and repeat merchant preference memory contracts.
     - `DATABASE_WORKFLOW.md`: Room schema export, automated migration testing, SQLCipher passphrase lifecycle, and SAF JSON/CSV export formatting.
     - `SECURITY_AND_PRIVACY_AUDIT.md`: Zero-telemetry policy, Android manifest permission audit, SQLCipher key derivation via AndroidKeyStore, and PII protection/sanitization.

6. ENVIRONMENT TEMPLATES & GITHUB STANDARDS:
   - Create sanitized `local.properties.example` template with SDK path and mock keystore placeholders (never commit real keystores or release passwords).
   - Create `.github/PULL_REQUEST_TEMPLATE.md`, `.github/ISSUE_TEMPLATE/bug_report.md`, and `.github/ISSUE_TEMPLATE/parser_request.md`.

7. MULTI-AI GOVERNANCE & POINTERS:
   - Create master rules in `AGENTS.md` and `.agents/AGENTS.md`.
   - Create pointer files (`.cursorrules`, `CLAUDE.md`, `.github/copilot-instructions.md`) directing all AI tools to obey `AGENTS.md`.

8. STRICT AGENT OPERATING RULES:
   - STRICT NO-PUSH: NEVER execute `git push` to remote repositories without my explicit written approval in the prompt. All commits must remain local on the active development branch (`Dev`).
   - MANDATORY DOC SYNC: Every code change or bugfix must update relevant docs (`FEATURES.md`, `PROJECT_TRACKER.md`, `Expense_Tracker_SRS_v5.md`, `Expense_Tracker_Implementation_Document_v4.md`, `Expense_Tracker_Idea_Document_v4.md`, `Expense_Tracker_Product_Roadmap.md`, and `walkthrough.md`) in the exact same git commit.
   - ZERO FALSE POSITIVE REQUIREMENT: Promotional alerts, OTPs, balance inquiries, and marketing notifications must never be parsed into transactions. Short service matchers (`cred`, `cheq`, `upi`) must use word boundaries (`\b`).
   - ABSOLUTE OFFLINE BOUNDARY & ENCRYPTION: Never introduce network permissions or external network telemetry. All database access must remain securely encrypted under SQLCipher AES-256.
   - TOKEN CONSERVATION (Lazy Loading): Do NOT load the entire documentation folder into prompt context. Lazy-load modular guides strictly on demand based on your active task.

Please inspect the current codebase and establish this structure now.
