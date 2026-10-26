# 🐛 Comprehensive Bug Audit & Remediation Report

> **Source Media**: 9 visual regression screenshots located in [`E:\ExpenseTrackerApp\bugs\`](file:///E:/ExpenseTrackerApp/bugs)  
> **Audited By**: Antigravity Android Systems Architect  
> **Scope**: Dashboard, History, Split Bills, Budgets, Analytics, App Settings, and Recurring Mandates / Inflows / Policies

---

## 📸 Executive Summary & Screenshot Mapping

| # | Screenshot Filename | Screen Tested | Primary Defects Identified |
|---|---------------------|---------------|----------------------------|
| 1 | `Screenshot_20261026_201227_VaultExpense.jpg` | **Dashboard** | Bottom nav label `"Dashboa..."` truncated; monthly stash badge `₹27,067/mo` collapsed into a 1-character vertical text column; `"Pending: ₹120,000"` badge multi-line wrap; unstripped `Upi-` merchant prefixes; FAB overlaps bottom item. |
| 2 | `Screenshot_20261026_201259_VaultExpense.jpg` | **Transaction History** | Bottom nav label truncated; Category filter chip clipped horizontally (`🏷️`); unstripped `Upi-` merchant prefixes; Period filter chip active state logic bug. |
| 3 | `Screenshot_20261026_201320_VaultExpense.jpg` | **Split Bills & Ledger** | Bottom nav label truncated; duplicate action triggers (center button + bottom-right FAB); FAB icon (`person_add`) mismatch for a group ledger screen. |
| 4 | `Screenshot_20261026_201331_VaultExpense.jpg` | **Budgets & Caps** | Bottom nav label truncated; FAB (+) placed directly over empty-state description text, obscuring instructions (`"Tap (+) to set category spending caps ar..."`). |
| 5 | `Screenshot_20261026_201340_VaultExpense.jpg` | **Financial Analytics** | Bottom nav label truncated; Section 80D limit breaks across 3 lines (`₹25,00` / `0`); Cashflow bar labels collide horizontally; unformatted currency (no commas, e.g. `₹1018096`); 64.5% unclassified "General Expense". |
| 6 | `Screenshot_20261026_201408_VaultExpense.jpg` | **App Settings** | Massive marketing copy inside settings; branding mismatch (`Arivtech Software Solutions` in banner vs `ArivTek LLP` in settings); text cutoff at bottom. |
| 7 | `Screenshot_20261026_201438_VaultExpense.jpg` | **Mandates (Outflows)** | Mandate titled literally `"Rs"` (SMS parser noise leak); FAB overlaps 3rd mandate item, hiding amount and delete/edit buttons; unparsed bank account text. |
| 8 | `Screenshot_20261026_201444_VaultExpense.jpg` | **Inflows (Income)** | Plural grammar bug (`"1 Streams"`); trailing whitespace in subtitle `"Salary (SSH )"`; lowercase stream title `"salary"`. |
| 9 | `Screenshot_20261026_201448_VaultExpense.jpg` | **Policies & Stash** | Duplicate "Term Life 1Cr" card in list (badge says "3 Policies", total pace is ₹27,067/mo for 3, but 4 cards rendered); FAB overlaps bottom card. |

---

## 🔍 Detailed Bug Catalog & Code Remediation

### 1. Bottom Navigation Tab Label Truncation (`"Dashboa..."`)
* **Screenshots**: Screenshots 1, 2, 3, 4, 5.
* **Component**: Bottom Navigation Bar.
* **File Reference**: [MainScreen.kt:L187-L195](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/main/MainScreen.kt#L187-L195)
* **Severity**: Medium (Visual Polish & Usability)
* **Visual Symptom**: The first tab displays `"Dashboa..."` instead of `"Dashboard"`.
* **Root Cause**:
  `NavigationBarItem` renders `Text(text = item.title, maxLines = 1, softWrap = false, fontSize = 11.sp, overflow = TextOverflow.Ellipsis)`. With 5 tabs on standard mobile screen widths (360–390dp), `"Dashboard"` (9 characters) exceeds the item container width and is truncated by Compose.
* **Code Fix**:
  Reduce font size to `10.sp` or `9.5.sp` and remove rigid horizontal text bounds:
  ```kotlin
  // MainScreen.kt (lines 188-194)
  label = {
      Text(
          text = item.title,
          maxLines = 1,
          softWrap = false,
          fontSize = 10.sp, // Reduced from 11.sp to guarantee single-line fit across devices
          overflow = TextOverflow.Ellipsis
      )
  }
  ```

---

### 2. Vertical Text / Collapsed Width Bug in "Annual Policies & Salary Stash" Card
* **Screenshot**: Screenshot 1 (Dashboard).
* **Component**: Policy Stash Header Card.
* **File Reference**: [DashboardScreen.kt:L1027-L1072](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/dashboard/DashboardScreen.kt#L1027-L1072)
* **Severity**: **Critical (Major Visual Defect)**
* **Visual Symptom**: The monthly stash pace badge `₹27,067/mo` is collapsed into a vertical 1-character-wide column of characters:
  ```
  ₹
  2
  7
  ,
  0
  6
  7
  /
  m
  o
  ```
* **Root Cause**:
  The left `Row` containing the shield icon and the title `"Annual Policies & Salary Stash"` lacks `Modifier.weight(1f)`. In Compose flex measurement, an unweighted child inside `Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween)` consumes all available horizontal space. The right badge `Card` is squeezed down to minimal width, forcing the `Text` component to wrap every single character onto a new line.
* **Code Fix**:
  Assign `Modifier.weight(1f)` to the left column and enforce `softWrap = false` on the badge text:
  ```kotlin
  // DashboardScreen.kt (lines 1027-1072)
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
  ) {
      Row(
          modifier = Modifier.weight(1f), // <-- Prevents left side from starving the badge of width
          verticalAlignment = Alignment.CenterVertically
      ) {
          Box(
              modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(PrimaryBlue.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
          ) {
              Text("🛡️", fontSize = 18.sp)
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
              Text(
                  text = "Annual Policies & Salary Stash",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                  text = "${insurancePolicies.size} policies • ₹${totalMonthlyPace.toInt()}/mo stash pace",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
              )
          }
      }

      Spacer(modifier = Modifier.width(8.dp))

      if (totalMonthlyPace > 0) {
          Card(
              colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.15f)),
              shape = RoundedCornerShape(10.dp)
          ) {
              Text(
                  text = if (isPrivacy) "₹•••••/mo" else "₹${String.format(Locale.getDefault(), "%,.0f", totalMonthlyPace)}/mo",
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  softWrap = false, // <-- Enforces single-line rendering
                  color = PrimaryBlue
              )
          }
      }
  }
  ```

---

### 3. Squeezed Multi-line Wrapping in "Scheduled Inflows & Salary" Badge
* **Screenshot**: Screenshot 1 (Dashboard).
* **Component**: Inflow Header Card.
* **File Reference**: [DashboardScreen.kt:L887-L932](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/dashboard/DashboardScreen.kt#L887-L932)
* **Severity**: High (Visual Defect)
* **Visual Symptom**: The status badge wraps into 4 awkward fragments:
  ```
  Pend
  ing:
  ₹120,
  000
  ```
* **Root Cause**:
  Identical layout bug as Bug 2. The left content row lacks `Modifier.weight(1f)`, starving the right badge card of space and causing `"Pending: ₹120,000"` to break across multiple lines.
* **Code Fix**:
  Add `Modifier.weight(1f)` to the left row and add `softWrap = false` and `maxLines = 1` to the badge text.

---

### 4. Floating Action Button (FAB) Overlapping & Obscuring Content
* **Screenshots**: Screenshots 1, 4, 7, 9.
* **Component**: Global & Screen Scaffolds.
* **File References**:
  - [BudgetsScreen.kt:L76-L94, L305-L340](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/budgets/BudgetsScreen.kt#L76-L94)
  - [SubscriptionsScreen.kt:L108-L146, L296, L560](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/subscriptions/SubscriptionsScreen.kt#L108-L146)
* **Severity**: High (Usability / Accessibility)
* **Visual Symptom**:
  - In Budgets (Screenshot 4): FAB (+) sits directly on top of the empty-state description text (`"Tap (+) to set category spending caps ar[FAB]"`).
  - In Mandates (Screenshot 7): FAB covers the right side of the 3rd mandate (`"Flipkart UPI AutoPay, a06"`), hiding amount, edit, and delete buttons.
  - In Policies (Screenshot 9): FAB covers the bottom of the `"Car Insurance"` card.
  - In Dashboard (Screenshot 1): FAB overlaps the bottom transaction item (`"Upi-deluscio..."`).
* **Root Cause**:
  1. Nested `Scaffold` instances without proper inner padding propagation.
  2. Insufficient bottom content padding in `LazyColumn` (`88.dp` is not enough when bottom bar + FAB are present).
  3. In empty states, cards occupy the exact vertical slot where the FAB anchors.
* **Code Fix**:
  1. Increase `contentPadding` on all lists containing a FAB to `PaddingValues(bottom = 120.dp)`.
  2. In empty states, add vertical spacing and center content:
  ```kotlin
  // BudgetsScreen.kt (around line 312)
  Column(
      modifier = Modifier
          .fillMaxWidth()
          .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 48.dp), // Extra bottom padding for clearance
      horizontalAlignment = Alignment.CenterHorizontally
  ) {
      // Icon + Texts
  }
  ```

---

### 5. Mandate Titled Literally `"Rs"` (SMS Parser Noise Leak)
* **Screenshot**: Screenshot 7 (Mandates tab).
* **Component**: SMS Ingestion / Subscription Detector.
* **File References**:
  - [RegexTransactionParser.kt:L546-L553, L570-L607](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/ingestion/parser/RegexTransactionParser.kt#L546-L553)
  - [SubscriptionDetector.kt:L50-L75](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/ingestion/parser/SubscriptionDetector.kt#L50-L75)
* **Severity**: High (Data Quality / False Ingestion)
* **Visual Symptom**: A recurring investment mandate is named `"Rs"` with amount `-₹1,495.00`.
* **Root Cause**:
  In `extractMerchantName()` Pattern 8 (`\s+to\s+([A-Za-z0-9\s@._\-*]+?)`), SMS text like `"debited to Rs 1495.00"` or `"transferred to Rs ..."` matched `"Rs"`. `cleanMerchantName()` lacked a blacklist of currency symbols and stop words, allowing `"Rs"` to be treated as a valid merchant name.
* **Code Fix**:
  Add validation rejecting currency keywords and short tokens in [RegexTransactionParser.kt](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/ingestion/parser/RegexTransactionParser.kt):
  ```kotlin
  private fun cleanMerchantName(raw: String): String {
      var clean = raw.trim()
      
      // Blacklist currency and generic transactional noise
      val forbiddenTokens = setOf("rs", "rs.", "inr", "inr.", "re", "amount", "payment", "bank", "debit", "credit", "transfer")
      if (clean.lowercase() in forbiddenTokens || clean.length < 2) {
          return "Bank Transaction"
      }
      // ... rest of merchant cleaning
  }
  ```

---

### 6. Unstripped `"Upi-"` Prefix on Merchant Names
* **Screenshots**: Screenshots 1, 2.
* **Component**: SMS Transaction Parser.
* **File Reference**: [RegexTransactionParser.kt:L579-L591](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/ingestion/parser/RegexTransactionParser.kt#L579-L591)
* **Severity**: Medium (Data Quality)
* **Visual Symptom**: Merchant names in transactions list appear with ugly `"Upi-"` prefixes: `"Upi-sweet R..."`, `"Upi-vishal M..."`, `"Upi-sudathi"`, `"Upi-pilgrim I..."`, `"Upi-anoj Yad..."`.
* **Root Cause**:
  In `cleanMerchantName()`, the regex stripping `^upi-` was placed **inside** `if (clean.contains("@"))`. For plain text merchants without an `@` VPA handle (e.g. `"UPI-SWEET RASOI"`), the code only checked `startsWith("Upi-m ")`, missing plain `"Upi-"` and `"UPI "`.
* **Code Fix**:
  Strip UPI prefixes unconditionally:
  ```kotlin
  // RegexTransactionParser.kt (in cleanMerchantName)
  clean = clean
      .replace(Regex("(?i)^upi[-_\s]+"), "")
      .replace(Regex("(?i)[-_\s]+upi$"), "")
      .trim()
  ```

---

### 7. Tax Deduction Limit Awkward Line Breaking (`₹25,00` / `0`)
* **Screenshot**: Screenshot 5 (Analytics).
* **Component**: Indian Tax Deductions Card.
* **File Reference**: [TaxSeasonDeductionsCard.kt:L275-L294](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/analytics/TaxSeasonDeductionsCard.kt#L275-L294)
* **Severity**: Medium (Visual Polish)
* **Visual Symptom**: Under Section 80D, the fraction amount renders across 3 lines:
  ```
  ₹0 /
  ₹25,00
  0
  ```
* **Root Cause**:
  In `SectionProgressRow()`, the label `"Section 80D (Health Insurance / Mediclaim)"` has no width constraint, pushing the right text `"$currentStr / $limitStr"` into a narrow sliver.
* **Code Fix**:
  Add `Modifier.weight(1f, fill = false)` to the section label and `softWrap = false` to the fraction text:
  ```kotlin
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
  ) {
      Text(
          text = sectionName,
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.weight(1f, fill = false),
          color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(Modifier.width(8.dp))
      val currentStr = if (isPrivacyMode) "₹•••••" else String.format(Locale.getDefault(), "₹%,.0f", currentAmount)
      val limitStr = String.format(Locale.getDefault(), "₹%,.0f", limitAmount)
      Text(
          text = "$currentStr / $limitStr",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          softWrap = false, // <-- Prevents digit breaking
          color = if (currentAmount >= limitAmount) GreenSuccess else MaterialTheme.colorScheme.onSurfaceVariant
      )
  }
  ```

---

### 8. Colliding Labels in Cashflow Bar Comparison
* **Screenshot**: Screenshot 5 (Analytics).
* **Component**: Analytics Cashflow Bar Comparison.
* **File Reference**: [AnalyticsScreen.kt:L359-L380](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/analytics/AnalyticsScreen.kt#L359-L380)
* **Severity**: Medium (Formatting / Layout)
* **Visual Symptom**: `Incomes: ₹102201 Expenses: ₹1018096 Investments: ₹65413` are crammed together horizontally, colliding with each other, unformatted, and misaligned with their corresponding canvas bars (which are drawn at 20%, 50%, and 80%).
* **Root Cause**:
  The labels are placed in a single `Row` with `Arrangement.SpaceBetween` rather than aligning under each bar center. Amounts use `%.0f` without commas.
* **Code Fix**:
  Use a 3-column weighted row and Indian currency comma formatting:
  ```kotlin
  // AnalyticsScreen.kt (lines 359-380)
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceEvenly
  ) {
      Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
          Text("Income", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(String.format(Locale.getDefault(), "₹%,.0f", uiState.totalIncome), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
      }
      Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
          Text("Expenses", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(String.format(Locale.getDefault(), "₹%,.0f", uiState.totalExpenses), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
      }
      Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
          Text("Investments", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(String.format(Locale.getDefault(), "₹%,.0f", uiState.totalInvestments), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
      }
  }
  ```

---

### 9. 64.5% of Expenses Categorized as "General Expense"
* **Screenshots**: Screenshots 1, 2, 5.
* **Component**: Category Classification Engine.
* **File Reference**: [CategoryClassifier.kt:L33-L108](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/ingestion/parser/CategoryClassifier.kt#L33-L108)
* **Severity**: High (Feature Accuracy / Taxonomy)
* **Visual Symptom**: ₹6,56,442.66 out of ₹10,18,096 (64.5%) is lumped into "General Expense". Real merchants like Sweet Rasoi (sweets), Cherry Bell (bakery), Vishal Mega Mart (clothing/groceries), Pilgrim (cosmetics), Deluscious (bakery/cafe), and Khan Chiken (meat/food) are all categorized as "General Expense".
* **Root Cause**:
  1. The dictionary lacks common Indian food and retail keywords (`sweet`, `rasoi`, `mithai`, `chiken`, `chicken`, `canteen`, `dhaba`, `deluscious`, `mart`, `megamart`, `bazaar`, `pilgrim`).
  2. The unstripped `Upi-` prefix prevents exact match lookups against user preference memory in `MerchantCategoryMappingDao`.
* **Code Fix**:
  Expand taxonomy keyword matching in [CategoryClassifier.kt](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/ingestion/parser/CategoryClassifier.kt):
  ```kotlin
  // Food & Dining
  lowerMerchant.contains("sweet") || lowerMerchant.contains("mithai") ||
  lowerMerchant.contains("rasoi") || lowerMerchant.contains("bhojan") ||
  lowerMerchant.contains("dhaba") || lowerMerchant.contains("canteen") ||
  lowerMerchant.contains("chiken") || lowerMerchant.contains("chicken") ||
  lowerMerchant.contains("mutton") || lowerMerchant.contains("fish") ||
  lowerMerchant.contains("biryani") || lowerMerchant.contains("snack") ||
  lowerMerchant.contains("deluscio") || lowerMerchant.contains("cherry bell") -> "Food & Dining"

  // Shopping & Retail
  lowerMerchant.contains("mart") || lowerMerchant.contains("megamart") ||
  lowerMerchant.contains("bazaar") || lowerMerchant.contains("pilgrim") ||
  lowerMerchant.contains("store") || lowerMerchant.contains("retail") -> "Shopping"
  ```

---

### 10. Grammar Flaw (`"1 Streams"`) & Whitespace Glitch in Recurring Inflows
* **Screenshot**: Screenshot 8 (Inflows tab).
* **Component**: Recurring Inflows Tab.
* **File References**:
  - [SubscriptionsScreen.kt:L465](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/subscriptions/SubscriptionsScreen.kt#L465)
  - [SubscriptionsScreen.kt:L776](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/subscriptions/SubscriptionsScreen.kt#L776)
* **Severity**: Low (Polish)
* **Visual Symptom**:
  - Badge says `"1 Streams"`.
  - Subtitle says `"Monthly • Salary (SSH )"` with an awkward space before the closing parenthesis.
  - Inflow name is rendered all-lowercase (`"salary"`).
* **Code Fix**:
  ```kotlin
  // Pluralization
  val activeCount = uiState.recurringIncomes.count { it.entity.isActive }
  Text(text = "$activeCount ${if (activeCount == 1) "Stream" else "Streams"}")

  // Trim payer and capitalize title
  val payer = item.entity.payerPpiOrBank.trim()
  val payerSuffix = if (payer.isNotBlank()) " ($payer)" else ""
  Text(text = "${item.entity.frequency} • ${item.entity.category}$payerSuffix")
  ```

---

### 11. Cut-off Filter Chip in Unified Filter Bar & Logic Inversion
* **Screenshot**: Screenshot 2 (History).
* **Component**: Unified Filter Bar.
* **File References**:
  - [UnifiedFilterBar.kt:L45](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/core/ui/components/UnifiedFilterBar.kt#L45)
  - [TransactionsScreen.kt:L228-L260](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/transactions/TransactionsScreen.kt#L228-L260)
* **Severity**: Medium (UI / Usability)
* **Visual Symptom**: The Category filter chip is cut off at the right edge showing only half the `🏷️` emoji. Also, the Period chip (`"📅 Period: All Time ▾"`) is highlighted in active blue even when default "All Time" is selected.
* **Root Cause**:
  `selectedOption != "ALL"` evaluates to `true` for `"All Time"`, falsely highlighting the chip as active.
* **Code Fix**:
  ```kotlin
  val isFilterActive = selectedOption != "ALL" && selectedOption != "All Time"
  Card(
      colors = CardDefaults.cardColors(
          containerColor = if (isFilterActive) PrimaryBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
      ),
      border = androidx.compose.foundation.BorderStroke(
          1.dp,
          if (isFilterActive) PrimaryBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
      )
  )
  ```

---

### 12. Negative Savings Rate Clamped at `-100.0%`
* **Screenshot**: Screenshot 1 (Dashboard).
* **Component**: Cashflow & Savings Calculation.
* **File Reference**: [DashboardViewModel.kt:L166-L167](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/dashboard/DashboardViewModel.kt#L166-L167)
* **Severity**: Medium (Financial Presentation)
* **Visual Symptom**: Dashboard displays `Savings: -100.0%` with a red alert badge.
* **Root Cause**:
  `((totalInc - totalExp) / totalInc * 100.0).coerceIn(-100.0, 100.0)` clamps negative cashflow to `-100.0%`. Early in the month before salary credits, expenses routinely exceed mid-cycle income. Showing `-100.0%` causes undue concern.
* **Code Fix**:
  When expenses exceed income, display `0.0%` with a badge `"Over Budget"` or `"Deficit: ₹X"` rather than a negative percentage clamp.

---

### 13. Duplicate "Term Life 1Cr" Item in Policies List
* **Screenshot**: Screenshot 9 (Policies tab).
* **Component**: Insurance Policies LazyColumn.
* **File Reference**: [SubscriptionsScreen.kt:L636](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/subscriptions/SubscriptionsScreen.kt#L636)
* **Severity**: Medium (Visual Consistency)
* **Visual Symptom**: Four cards appear in the list (two identical "Term Life 1Cr" cards), while the header badge strictly states `"3 Policies"`, and the monthly pace math (`₹27,067/mo`) matches exactly 3 policies (`17,500 + 7,900 + 1,667 = 27,067`).
* **Root Cause**:
  In `SubscriptionsScreen.kt:636`, `items(plan.items)` does not specify a `key = { it.policy.id }`. Lack of stable keys can cause Compose item duplication glitches during scroll/recomposition, and Android OS scroll capture (stitching screenshots) frequently duplicates identical cards.
* **Code Fix**:
  Provide an explicit unique key:
  ```kotlin
  items(plan.items, key = { it.policy.id }) { item ->
      PolicyItemCard(...)
  }
  ```

---

### 14. Intrusive Top Promo Banner & Branding Discrepancy
* **Screenshots**: Screenshots 1, 2, 3, 4, 5, 6.
* **Component**: Global Header Banner.
* **File Reference**: [MainScreen.kt:L274-L281, L315-L430](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/main/MainScreen.kt#L274-L281)
* **Severity**: Medium (UX / Branding)
* **Visual Symptom**:
  - Persistent header banner takes ~65dp of vertical space across all main tabs, pushing core financial data down.
  - Text in the banner is truncated: `"Arivtech Software Solutio..."` and `"Custom mobile, web & AI softwa..."`.
  - Branding inconsistency: Banner says `"Arivtech Software Solutions"` while Settings says `"ArivTek LLP"`.
* **Code Fix**:
  - Allow user to dismiss or collapse the banner, or relocate it to the Settings / About screen.
  - Unify branding across strings to `"ArivTek LLP"`.

---

### 15. Raw Unformatted Numbers in `AnimatedCurrencyText` (Zero Commas)
* **Screenshot**: Screenshot 1 (Dashboard).
* **Component**: Hero Currency Ticker.
* **File Reference**: [DashboardScreen.kt:L2050-L2076](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/dashboard/DashboardScreen.kt#L2050-L2076)
* **Severity**: High (Financial Presentation)
* **Visual Symptom**:
  All primary dashboard numbers are displayed as raw digits without commas:
  - Net Cashflow: `-₹218612.27` (should be `-₹2,18,612.27`)
  - Income: `₹15096.34` (should be `₹15,096.34`)
  - Expenses: `₹233708.59` (should be `₹2,33,708.59`)
  - Invested: `₹20708.00` (should be `₹20,708.00`)
  - Safe-to-Spend: `₹0.00`
* **Root Cause**:
  In `AnimatedCurrencyText()`, line 2070 uses raw formatting:
  `val formatted = if (animatedValue >= 0) "$prefix${String.format("%.2f", animatedValue)}" else "-₹${String.format("%.2f", -animatedValue)}"`
  This completely ignores number grouping separators.
* **Code Fix**:
  Use `NumberFormat` or Indian locale formatting:
  ```kotlin
  val indianFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
      maximumFractionDigits = 2
      currency = Currency.getInstance("INR")
  }
  ```

---

### 16. Inverted / Misleading Positive Emojis on Zero/Deficit Values
* **Screenshots**: Screenshot 1 (Dashboard), Screenshot 8 (Inflows).
* **Component**: Status Indicator Badges.
* **File References**:
  - [DashboardScreen.kt:L672, L937](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/dashboard/DashboardScreen.kt#L672)
  - [SubscriptionsScreen.kt:L486](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/subscriptions/SubscriptionsScreen.kt#L486)
* **Severity**: Medium (Visual Semantics)
* **Visual Symptom**:
  - Subscriptions card shows: `Paid: ₹0 🟢` (Green check emoji for zero payments made!).
  - Inflows card shows: `Realized: ₹0 🟢` (Green check emoji for zero income received!).
  - Safe-to-Spend card shows: `Cap Reached 🔴` with `Unspent: ₹0` rendered in bright positive green!
* **Root Cause**:
  The emoji `🟢` and the color `GreenSuccess` are hardcoded strings rather than conditionally rendered based on whether the amount is positive:
  ```kotlin
  text = if (isPrivacy) "Realized: ₹•••••" else "Realized: ₹${String.format(Locale.getDefault(), "%,.0f", realizedThisCycle)} 🟢"
  ```
* **Code Fix**:
  Make the indicator conditional:
  ```kotlin
  val indicator = if (realizedThisCycle > 0) "🟢" else "⚪"
  val paidIndicator = if (totalPaidComm > 0) "🟢" else "⏳"
  val unspentColor = if (safeSpendToday <= 0.0) MaterialTheme.colorScheme.error else GreenSuccess
  ```

---

### 17. Redundant Dual Cash Logging Actions in Dashboard
* **Screenshot**: Screenshot 1 (Dashboard).
* **Component**: Top App Bar & Header Row.
* **File References**:
  - [MainScreen.kt:L247-L249](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/main/MainScreen.kt#L247-L249)
  - [DashboardScreen.kt:L543-L555](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/dashboard/DashboardScreen.kt#L543-L555)
* **Severity**: Low (UI Clutter)
* **Visual Symptom**:
  Within 50dp of vertical space, the user sees two cash logging triggers:
  1. Top App Bar: Raw emoji `IconButton { Text("💵") }`.
  2. Dashboard Header: Styled Button `Button { Text("💵 Cash") }`.
* **Code Fix**:
  Keep the top app bar clean and uncluttered (logo + Privacy Mode toggle + Settings gear), and preserve the explicit, labeled `💵 Cash` button in the Dashboard action row.

---

### 18. Aggressive Merchant Name Truncation Due to Row-Level Action Buttons
* **Screenshots**: Screenshots 1, 2.
* **Component**: Transaction Item Card.
* **File Reference**: [TransactionsScreen.kt:L375-L425](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/transactions/TransactionsScreen.kt#L375-L425)
* **Severity**: Medium (Information Density)
* **Visual Symptom**:
  Merchant names truncate after only 12–15 characters (e.g. `"Upi-sweet R..."`, `"Bharat Petro..."`, `"Upi-vishal M..."`), despite large phone screens.
* **Root Cause**:
  Each transaction row contains:
  `[Icon (40dp)] + [Merchant & Date Column] + [Amount Text (80dp)] + [Pencil Icon (40dp)] + [Trash Icon (40dp)]`
  The action buttons permanently consume 80dp of horizontal space on every single row.
* **Code Fix**:
  Remove permanent row-level action buttons. Clicking any card already opens the rich `TransactionDetailsDialog` (which features Edit, Delete, Undo, and View SMS). This immediately frees up 80dp per row, allowing merchant names to display fully without truncation.

---

### 19. Date Formatting Inconsistency Across Tabs
* **Screenshots**: Screenshot 1 vs Screenshot 2.
* **Component**: Transaction Date Formatter.
* **Severity**: Low (Consistency)
* **Visual Symptom**:
  - In Dashboard: `"24 Oct, 10:09 pm"` (omits year).
  - In History: `"24 Oct 2026, 10:09 pm"` (includes year).
* **Code Fix**:
  Standardize across all transaction lists:
  Show year only when the transaction is from a previous calendar year:
  ```kotlin
  fun formatTransactionDate(timestampMs: Long): String {
      val txCal = Calendar.getInstance().apply { timeInMillis = timestampMs }
      val nowCal = Calendar.getInstance()
      val isCurrentYear = txCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)
      val pattern = if (isCurrentYear) "dd MMM, hh:mm a" else "dd MMM yyyy, hh:mm a"
      return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestampMs))
  }
  ```

---

### 20. Split Bills Screen Action & Icon Semantics Mismatch
* **Screenshot**: Screenshot 3 (Split Bills).
* **Component**: Split Screen Scaffold & FAB.
* **File Reference**: [SplitScreen.kt](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/split/SplitScreen.kt)
* **Severity**: Low (UX Semantics)
* **Visual Symptom**:
  The FAB icon is `Icons.Default.PersonAdd` (a single person silhouette with a plus sign) on a screen titled `"Split Bills & Group Ledger"`.
* **Code Fix**:
  Replace with `Icons.Default.GroupAdd` to semantically convey creating a shared group ledger.

---

## 🚀 Comprehensive Product & UI/UX Improvement Blueprint

### A. Typography & Indian Numbering Formatting Engine
1. **Centralized Currency Formatter**:
   Create a dedicated object `IndianCurrencyFormatter`:
   ```kotlin
   object IndianCurrencyFormatter {
       fun format(amount: Double, includeDecimals: Boolean = false): String {
           val isNegative = amount < 0
           val absAmount = Math.abs(amount)
           val formatted = if (includeDecimals) {
               String.format(Locale("en", "IN"), "%,.2f", absAmount)
           } else {
               String.format(Locale("en", "IN"), "%,.0f", absAmount)
           }
           return if (isNegative) "-₹$formatted" else "₹$formatted"
       }
   }
   ```
2. **Standardize all UI displays**:
   Replace raw `%.0f` and `%.2f` calls in Dashboard, Analytics Donut, Bar Comparison, Credit Cards, and Mandates with `IndianCurrencyFormatter.format()`.

### B. Categorization & Merchant Intelligence Overhaul (The 64.5% Solution)
1. **Unconditional Prefix Cleaning**:
   Strip `UPI-`, `UPI `, `IMPS-`, `NEFT-`, `POS ` before passing merchant names to both `CategoryClassifier` and `MerchantCategoryMappingDao`.
2. **Indian Merchant & Taxonomy Expansion**:
   Add keyword coverage for:
   - **Dining / Food**: `sweet`, `sweets`, `mithai`, `rasoi`, `bhojan`, `dhaba`, `canteen`, `roll`, `biryani`, `chiken`, `chicken`, `mutton`, `fish`, `shawarma`, `dessert`, `snack`, `deluscio`, `cherry bell`, `halwai`, `juice`, `tea`, `chai`, `coffee`, `tiffin`.
   - **Shopping & Retail**: `vishal`, `megamart`, `mart`, `bazaar`, `bazar`, `pilgrim`, `store`, `fashions`, `cloth`, `apparel`, `footwear`, `jewel`, `optics`, `trends`, `pantaloons`, `max`, `decathlon`, `lenskart`.
   - **Groceries**: `dmart`, `more`, `nature's basket`, `spencer`, `smart bazaar`, `kirana`, `provisions`, `dairy`, `milk`.
3. **Fuzzy Memory Matching**:
   Ensure user edits to a merchant category (e.g. assigning "Food & Dining" to "Sweet Rasoi") match future variations like "UPI-SWEET RASOI" by storing normalized canonical merchant tokens.

### C. Dashboard & Information Hierarchy Polish
1. **Hero Cashflow Card**:
   - Change minus sign display to an en-dash `−₹2,18,612.27` for typographic elegance.
   - Separate investment outflows cleanly from general expenses so users understand whether money was consumed or saved/invested.
2. **Safe-to-Spend Card**:
   - When cap is reached, display `Unspent: ₹0` in neutral grey or amber rather than vibrant green.
   - Add a micro-explanation: `"Monthly expenses exceeded income by ₹X. Resets on 1st Nov."`
3. **Banner Dismissibility**:
   - Provide an `[✕]` dismiss button on the top carousel banner so users who want an uncluttered financial cockpit can hide promotional banners.

### D. Empty State Design & Guided Onboarding
Currently, the Budgets, Split Bills, and Inflows screens show large black voids when no items exist:
1. **Split Bills Empty State**:
   Add starter template chips: `[🏖️ Goa Trip]` `[🏠 Apartment Rent & Wifi]` `[🍕 Friday Dinner Split]`. Tapping a chip immediately creates a pre-configured group ledger.
2. **Budgets Empty State**:
   Add a 1-tap `[Auto-Generate Budgets based on Past 30 Days Spend]` button so users don't have to manually configure 10 categories.
3. **Inflows Empty State**:
   Add quick presets: `[🏢 Add Monthly Salary]` `[🏠 Add House Rent Received]` `[📈 Add SIP Dividend]`.

---

## 📋 Comprehensive Remediation Priority Matrix

| Priority | ID | Defect / Improvement | Target Component & File | User Impact |
|:---|:---|:---|:---|:---|
| **P0 (Critical)** | 2 | Vertical `₹27,067/mo` text collapse | [DashboardScreen.kt:1027](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/dashboard/DashboardScreen.kt#L1027) | Fixes broken layout on the primary home screen |
| **P0 (Critical)** | 1 | Bottom Navigation `"Dashboa..."` truncation | [MainScreen.kt:187](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/main/MainScreen.kt#L187) | Fixes global navigation visual defect on all screens |
| **P0 (Critical)** | 3 | Multi-line `Pending: ₹120,000` badge | [DashboardScreen.kt:887](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/dashboard/DashboardScreen.kt#L887) | Prevents badge text from splintering into 4 lines |
| **P1 (High)** | 4 | FAB covering empty-state text & list items | [BudgetsScreen.kt:76](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/budgets/BudgetsScreen.kt#L76), [SubscriptionsScreen.kt:296](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/subscriptions/SubscriptionsScreen.kt#L296) | Restores readability and action button accessibility |
| **P1 (High)** | 5 | Mandate titled literally `"Rs"` | [RegexTransactionParser.kt:546](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/ingestion/parser/RegexTransactionParser.kt#L546) | Prevents currency stop words from being ingested as merchants |
| **P1 (High)** | 9 | 64.5% unclassified General Expense | [CategoryClassifier.kt:33](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/ingestion/parser/CategoryClassifier.kt#L33) | Restores meaningful insights to analytics and budgeting |
| **P1 (High)** | 15 | Raw unformatted currency (`₹218612.27`) | [DashboardScreen.kt:2050](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/dashboard/DashboardScreen.kt#L2050), [AnalyticsScreen.kt:359](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/analytics/AnalyticsScreen.kt#L359) | Formats all financial amounts with proper Indian grouping commas |
| **P2 (Medium)** | 6 | Strip leading `Upi-` from merchants | [RegexTransactionParser.kt:579](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/ingestion/parser/RegexTransactionParser.kt#L579) | Cleans merchant names and enables preference matching |
| **P2 (Medium)** | 7 | 80D Tax limit 3-line break (`₹25,00` / `0`) | [TaxSeasonDeductionsCard.kt:275](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/analytics/TaxSeasonDeductionsCard.kt#L275) | Eliminates digit breaking in tax deductions summary |
| **P2 (Medium)** | 8 | Cashflow bar label collision | [AnalyticsScreen.kt:359](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/analytics/AnalyticsScreen.kt#L359) | Aligns labels under bars with 3-column layout |
| **P2 (Medium)** | 16 | Misleading positive emojis on 0/deficit | [DashboardScreen.kt:672](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/dashboard/DashboardScreen.kt#L672), [SubscriptionsScreen.kt:486](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/subscriptions/SubscriptionsScreen.kt#L486) | Corrects visual indicator semantics across cards |
| **P2 (Medium)** | 18 | Aggressive merchant truncation | [TransactionsScreen.kt:375](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/transactions/TransactionsScreen.kt#L375) | Frees 80dp per row by shifting actions to tap dialog |
| **P3 (Low)** | 10 | Plural grammar `"1 Streams"` & whitespace | [SubscriptionsScreen.kt:465](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/subscriptions/SubscriptionsScreen.kt#L465) | Linguistic and string trim polish |
| **P3 (Low)** | 11 | Cut-off filter chips in Unified Filter Bar | [UnifiedFilterBar.kt:45](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/core/ui/components/UnifiedFilterBar.kt#L45) | Fixes active state logic and compacts chip labels |
| **P3 (Low)** | 17 | Redundant dual cash logging buttons | [MainScreen.kt:247](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/main/MainScreen.kt#L247) | Removes visual duplication in dashboard header |
| **P3 (Low)** | 20 | Split Bills FAB icon mismatch | [SplitScreen.kt](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/split/SplitScreen.kt) | Changes `person_add` to `group_add` |
