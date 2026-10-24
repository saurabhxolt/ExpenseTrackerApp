---
name: Bank / SMS Parser Request
about: Request support for a new Bank, Card, or UPI SMS pattern
title: "[PARSER] "
labels: parser, enhancement
assignees: ''
---

**Bank / Institution Name:**
[e.g. Standard Chartered, Federal Bank, Jupiter]

**Transaction Type:**
- [ ] Debit / Outflow (Expense)
- [ ] Credit / Inflow (Income)
- [ ] Transfer / Credit Card Settlement (Transfer)
- [ ] Recurring Mandate / Subscription (Mandate)

**Sample SMS Text (ANONYMIZED - NO REAL ACCOUNT NUMBERS/OTPS):**
```
Example: Sent Rs.500.00 from Bank A/C ending 1234 to Merchant on 25-OCT-26.
```

**Sender Header / ID:**
[e.g. `VM-SCBANK`, `AD-FEDBNK`]

**Expected Extracted Fields:**
- Amount: ₹
- Merchant: 
- Account (last 4 digits): 
- Transaction Date: 
