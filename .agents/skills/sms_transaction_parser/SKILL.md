---
name: sms_transaction_parser
description: Specialized agent skill for auditing, benchmarking, and refining SMS transaction parsing, merchant extraction, and non-transaction noise rejection rules in ExpenseTrackerApp.
---

# Dedicated SMS Transaction Parser Agent Skill

## Overview
This skill equips AI agents and subagents with the domain rules, test vectors, regex pattern safety standards, and evaluation protocols required to audit and enhance financial SMS parsing in `ExpenseTrackerApp`.

## Capabilities & Responsibilities

1. **False Positive Prevention & Noise Rejection**:
   - Differentiate actual completed financial transactions (Debits, Credits, Transfers, Investments) from marketing offers, loan approvals, festive promos, EMI conversion prompts, e-mandates, and OTPs.
   - Reject promotional loan phrases: `festive offer`, `festive loan`, `digital authorization`, `is 1 step away`, `convert your transaction`, `no paperwork needed`, `pre-approved credit`, `apply via link`.

2. **Regex Word Boundary Safety**:
   - Enforce exact word boundaries (`\b`) on short keyword matchers (e.g. `\bcred\b` instead of substring matching `" to cred"`, which collides with `"to credit"`).
   - Ensure amount extraction ignores pre-approved limits, current outstanding balances, and total credit limits.

3. **Merchant & Category Classification**:
   - Standardize merchant extraction for UPI VPAs (`@ybl`, `@okaxis`, `@paytm`, `@icici`).
   - Categorize payments accurately into `Food & Dining`, `Bills & Utilities`, `Shopping`, `Transportation`, `Investments`, `Income`, `Insurance`, `Government & Fees`.

4. **Continuous Quality & Test Suite Auditing**:
   - Every modified parser pattern MUST be backed by a corresponding unit test in `RegexTransactionParserTest.kt`.
   - Maintain 100% pass rate across all historical test vectors.

## Execution Checklist for Parser Auditing

```mermaid
graph TD
    A["Input Raw SMS Text"] --> B{"Stage 1: Rejection Filter"}
    B -- "Matches Promo / OTP / Failed / Informational" --> C["Return Null (Ignore)"]
    B -- "Passes Filter" --> D["Stage 2: Text Sanitization"]
    D --> E{"Stage 3: Action Classification"}
    E -- "CC Bill Payment / Self Transfer / Investment" --> F["Return ParsedTransaction"]
    E -- "Credit Pattern Match" --> F
    E -- "Debit Pattern Match" --> F
    E -- "No Match" --> C
```

## Running Automated Verification
To verify all parser rules and test vectors:
```powershell
cmd /c gradlew.bat testDebugUnitTest --tests "com.expensetracker.app.ingestion.parser.RegexTransactionParserTest"
```
