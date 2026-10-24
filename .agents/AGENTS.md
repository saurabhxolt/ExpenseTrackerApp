# Project Rules & Customizations - Expense Tracker App

## Documentation Synchronization (Compulsory Rule)
1. **Continuous Documentation Sync**: Always maintain exact tracking of development progress. 
2. **Codebase-Document Alignment**: Whenever new code, features, architecture components, database entities, or parsers are implemented or modified, all relevant Markdown specification documents (`Expense_Tracker_SRS_v5.md`, `Expense_Tracker_Implementation_Document_v4.md`, `Expense_Tracker_Idea_Document_v4.md`, `Expense_Tracker_Product_Roadmap.md`, and `walkthrough.md`) MUST be updated immediately to reflect the latest state of the codebase.
3. **Commit Integrity**: Ensure documentation updates are tracked alongside code changes in Git commits.

## Dedicated Agents & Parser Quality Rules
1. **SMS Transaction Parser Agent Skill**: The dedicated agent skill `sms_transaction_parser` located in [.agents/skills/sms_transaction_parser/SKILL.md](file:///E:/ExpenseTrackerApp/.agents/skills/sms_transaction_parser/SKILL.md) MUST be utilized for auditing, benchmarking, and pattern validation of transaction ingestion.
2. **Zero False Positive Requirement**: Promotional offers, pre-approved loan notifications, EMI conversion marketing, OTPs, and informational statement alerts MUST NOT be ingested as financial transactions.
3. **Word Boundary Protection**: All short service matchers (such as `cred`, `cheq`, `upi`) must use word boundary checks (`\b`) to avoid colliding with natural language terms like `to credit`.
4. **Category Memory & Assignment Agent Skill**: The dedicated agent skill `category_assignment_agent` located in [.agents/skills/category_assignment_agent/SKILL.md](file:///E:/ExpenseTrackerApp/.agents/skills/category_assignment_agent/SKILL.md) MUST be utilized for category taxonomy validation, in-place category editing, and repeat merchant preference memory auditing.
