## 1. Authoritative implementation plan

Repository instructions require Beads for task state. This artifact maps the change to durable issues; it intentionally contains no duplicate Markdown checklist. Query current state with `bd show dta-7ou` and `bd ready`. Claim and close tasks in Beads. Do not run checklist-based OpenSpec apply automation against this reference table.

| Order | Beads issue | Deliverable |
| --- | --- | --- |
| 1 | `dta-7ou.1` | Prove Android wire compatibility with the real Mac app |
| 2 | `dta-7ou.2` | Inventory the complete iOS remote image interface |
| 3 | `dta-7ou.3` | Create native Android shell and mandatory Mac connection |
| 4 | `dta-7ou.4` | Implement authenticated encrypted app-private vault |
| 5 | `dta-7ou.5` | Implement remote generation lifecycle and private result save |
| 6 | `dta-7ou.6` | Build full applicable image control interface |
| 7 | `dta-7ou.7` | Verify release on Android against official Draw Things Mac app |

Dependencies live in Beads. The wire proof and parity inventory can begin independently; the app acceptance issue depends on the completed editor path. Keep this OpenSpec change open until the implementation epic and all acceptance requirements are fulfilled.
