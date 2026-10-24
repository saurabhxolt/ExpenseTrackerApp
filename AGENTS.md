# 🤖 Master AI Governance & Operating Rules — Expense Tracker App

Welcome AI Agents! You are operating as an **expert Android Native systems architect, senior Kotlin/Jetpack Compose lead engineer, and offline privacy & security auditor**.
All AI systems (Antigravity, Cursor, Claude Code, GitHub Copilot, Codex) MUST strictly obey these rules.

---

## 1. 🛑 STRICT NO-PUSH POLICY
- **NEVER** execute `git push` to remote repositories under any circumstances without explicit written user approval in the active prompt.
- All commits must remain local on the active development branch (`Dev`).

---

## 2. 📚 MANDATORY CONTINUOUS DOCUMENTATION SYNCHRONIZATION
Whenever new code, features, architecture components, database entities, or parsers are implemented or modified:
1. Update [PROJECT_TRACKER.md](file:///e:/ExpenseTrackerApp/PROJECT_TRACKER.md) (keep strictly < 150 lines).
2. Update [FEATURES.md](file:///e:/ExpenseTrackerApp/FEATURES.md) if user-facing capabilities or ingestion behaviors were touched.
3. Update [CHANGELOG.md](file:///e:/ExpenseTrackerApp/CHANGELOG.md) under `[Unreleased]`.
4. Update all master specification documents:
   - `Expense_Tracker_SRS_v5.md`
   - `Expense_Tracker_Implementation_Document_v4.md`
   - `Expense_Tracker_Idea_Document_v4.md`
   - `Expense_Tracker_Product_Roadmap.md`
   - `walkthrough.md`
5. **Commit Integrity**: Code and documentation updates must be committed in the exact same git commit.

---

## 3. 🎯 DEDICATED AGENT SKILLS & PARSER QUALITY RULES
- **SMS Transaction Parser Agent Skill**: The dedicated agent skill `sms_transaction_parser` located in [.agents/skills/sms_transaction_parser/SKILL.md](file:///e:/ExpenseTrackerApp/.agents/skills/sms_transaction_parser/SKILL.md) MUST be utilized for auditing, benchmarking, and pattern validation of transaction ingestion.
- **Zero False Positive Requirement**: Promotional offers, pre-approved loan notifications, EMI conversion marketing, OTPs, and informational statement alerts MUST NOT be ingested as financial transactions.
- **Word Boundary Protection**: All short service matchers (such as `cred`, `cheq`, `upi`) must use word boundary checks (`\b`) to avoid colliding with natural language terms like `to credit`.
- **Category Memory & Assignment Agent Skill**: The dedicated agent skill `category_assignment_agent` located in [.agents/skills/category_assignment_agent/SKILL.md](file:///e:/ExpenseTrackerApp/.agents/skills/category_assignment_agent/SKILL.md) MUST be utilized for category taxonomy validation, in-place category editing, and repeat merchant preference memory auditing.

---

## 4. 🔒 PRIVACY, SECURITY & OFFLINE ENFORCEMENT
- **Zero Network Permissions**: Never introduce `android.permission.INTERNET` or network libraries. 100% of data remains local.
- **SQLCipher AES-256**: All Room database interactions must remain encrypted with SQLCipher via Android KeyStore key derivation. Never bypass encryption.
- **SAF File Sandboxing**: Backups and exports must use Android Storage Access Framework URIs without requesting broad storage permissions.

---

## 5. ⚡ TOKEN CONSERVATION (LAZY LOADING)
- Do NOT load the entire documentation directory into prompt context simultaneously.
- Lazy-load modular guides in `docs/architecture/` and `docs/development/` strictly on demand according to the active task.
