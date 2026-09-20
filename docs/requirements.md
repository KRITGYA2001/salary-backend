# Requirements: ACME Salary Management (one page)

**Persona:** HR Manager at ACME (single persona, ~10,000 employees, multiple countries)
**Problem:** Salary data lives in Excel files. It is slow to edit, easy to corrupt, and hard to query ("how do we pay people?").

## 1. Goal
Replace the spreadsheets with a fast web app where the HR Manager can **(a) maintain employee salary records** and **(b) answer questions about how ACME pays people**, with no Excel involved.

Success looks like:
- Find any employee in under 5 seconds, out of 10,000.
- Edit a salary in under 3 clicks, with the change recorded.
- Answer "what do we pay Engineers in India vs. Germany?" in one screen.

## 2. In scope (MVP)

| # | Feature | Why it matters |
|---|---------|----------------|
| F1 | **Employee list**: server-side pagination, sorting, search (name/email/code), filters (country, department, job title, status) | 10k rows cannot be loaded or rendered at once |
| F2 | **Create / view / edit / deactivate employee** with validation | Replaces the Excel edit workflow |
| F3 | **Salary change with history**: every change stores old/new value, effective date, reason | HR needs to know who was paid what, and when |
| F4 | **Pay insights dashboard**: headcount and min / avg / median / p90 / max salary, sliced by country, department, job title; salary distribution histogram; top/bottom earners | The "how do we pay people" questions |
| F5 | **Multi-currency**: salary stored in local currency, insights normalised to a single reporting currency (USD) using a static rate table | Multiple countries make raw comparison meaningless |
| F6 | **CSV export** of the current filtered list | HR will still share data with Finance |
| F7 | **Seed script**: 10,000 realistic employees, deterministic (fixed seed) | Required by the assessment; enables perf testing |
| F8 | **Deployed app + demo video** | Required by the assessment |

## 3. Deliberately left out (and why)

| Excluded | Reasoning |
|----------|-----------|
| Authentication / RBAC | The brief has one persona. Auth adds a lot of surface area and no product insight. Mitigation: the demo uses fictional data, and the API is structured so Spring Security can be added later (first hardening step). |
| Payroll, tax, payslips, bonuses, equity | This is *salary management*, not payroll. Different domain, legal risk, out of the stated problem. |
| Live FX rates | Adds an external dependency and non-determinism. Static, dated rate table is enough; editable later. |
| Excel/CSV **import** | Real need, but a one-time migration concern. The seed script covers the demo. Listed as the top "next step". |
| Approval workflows / multi-approver changes | Only one user exists; workflow needs a second persona. |
| Org chart, performance reviews, salary bands and compa-ratio | Valuable follow-ons, but they need data the brief doesn't give us. Bands are the #1 roadmap item. |
| Multi-tenancy, i18n, mobile-native | Not requested; responsive layout is enough. |

## 4. Key assumptions
1. One legal entity view: all 10,000 employees are visible to the one HR Manager.
2. Salary means **annual base gross salary** in the employee's local currency.
3. Country determines currency (one currency per country).
4. The data volume is fixed at roughly 10k, so SQLite with indexes is enough. Larger scale would mean a move to PostgreSQL (see design.md §12).
5. Stack (given): Java 17 + Spring Boot backend, React frontend, SQLite. Deployed on a single Ubuntu VM.

## 5. Non-functional requirements
- List and search API responds in **< 300 ms p95** on 10k rows.
- Insights API responds in **< 500 ms** (SQL aggregation, not in-app loops).
- Salary changes are atomic: the employee update and the history row commit together or not at all.
- Money is stored as **integer minor units**, never floats.
- Core logic is covered by fast, deterministic unit tests (no network, no real clock, in-memory or temp-file SQLite).
- Every code change is deployed to the VM (see design.md §11).
