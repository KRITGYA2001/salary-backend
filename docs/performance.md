# Performance Considerations

Scale: **10,000 employees**. This is small, so the goal is correct habits (no accidental O(n) in the browser, no N+1) rather than heroics.

## 1. Budgets
| Operation | Budget (p95, local, 10k rows) |
|-----------|------------------------------|
| `GET /employees` (any filter/sort/page) | < 300 ms |
| `GET /insights/*` | < 500 ms |
| Salary change | < 100 ms |
| Seed 10,000 rows | < 5 s |
| CSV export of all 10k | streamed; first byte < 300 ms |
| UI first meaningful render of list | < 1.5 s on deployed app |

## 2. Techniques and rationale

| Concern | Technique | Why |
|---------|-----------|-----|
| Rendering 10k rows | Server-side pagination (default 25, max 100) | Browser and payload stay small |
| Slow filters/sorts | Indexes matching real predicates (design §4) | Avoids full scans |
| Text search | Prefix `LIKE 'abc%'` on NOCASE-indexed name; separate exact match on code and email | Leading-wildcard `%abc%` cannot use an index. Prefix search is the default. Substring can be added with FTS5 if needed |
| `COUNT(*)` for pagination | Same WHERE, single query, run only when filters change or page 1 | Avoids counting on every page turn |
| Aggregates | Computed in SQL (GROUP BY, window functions), never by loading rows into JS | Moves work to the engine, keeps memory flat |
| Currency conversion | Joined at query time (`fx_rate` has about 10 rows) | Tiny join, no denormalised stale data |
| Write throughput at seed | One transaction with JDBC `batchUpdate` on a prepared statement | Orders of magnitude faster than autocommit inserts |
| Concurrency | `journal_mode=WAL` | Readers do not block the writer |
| Repeated reads | Short `Cache-Control` on `/meta/filters`. Nginx gzip for JSON and static assets | Cheap wins for the dashboard |
| Client | TanStack Query cache, debounced search, `keepPreviousData` | No flicker, no request storms |
| Export | `StreamingResponseBody` iterating a JDBC `ResultSet` | Constant memory |
| JVM on a 1 GB VM | `-Xmx384m`, headless JRE, 1 GB swapfile as headroom, small Hikari pool (SQLite is single-writer) | Keeps the VM stable |

## 3. Verification plan
1. `EXPLAIN QUERY PLAN` for each list variant and insight query. Assert **no `SCAN employee`** for indexed filters.
2. Tagged benchmark test (`./mvnw test -Pperf`) seeds 10k and asserts the budgets above. Timings are also spot-checked with `curl -w` against the deployed VM.
3. Measured numbers are recorded below (T4.1).

### Measured results
**Server side** (`./mvnw test -Pperf -Dtest=PerformanceBudgetTest`, 10,000 seeded employees, developer laptop, 30 runs after 5 warm-up runs, in-process so no network):

| Query | p95 | Budget |
|-------|-----|--------|
| list, no filter, page 1 | 10.5 ms | 300 ms |
| list, country + department, sort salary | 9.1 ms | 300 ms |
| list, name prefix search | 20.7 ms | 300 ms |
| list, deep page (page 400, sort salary) | 11.1 ms | 300 ms |
| insights summary | 8.1 ms | 500 ms |
| insights by country | 18.4 ms | 500 ms |
| insights by jobTitle (widest) | 10.6 ms | 500 ms |
| insights distribution | 8.5 ms | 500 ms |
| insights top 10 | 8.1 ms | 500 ms |
| seed 10k | 1.4 s | 5 s |

**Deployed VM** (`curl` through Nginx from the developer machine, 10 requests, p90, gzip on; includes network round trip of about 60 ms):

| Request | p90 | Response |
|---------|-----|----------|
| list, page 1 | 103 ms | 1.8 KB |
| list, country + department, sort salary | 98 ms | 1.6 KB |
| list, name prefix search | 90 ms | small |
| insights summary | 201 ms | 113 B |
| insights by country | 213 ms | 602 B |
| insights by jobTitle | 220 ms | 1.1 KB |
| insights distribution | 134 ms | 197 B |
| CSV export, all rows | first byte 237 ms, total 4.5 s | 1.41 MB raw, 346 KB gzipped |
| static page | 65 ms | |

The seed took about 5.1 s on the 1 GB VM at first boot (measured in T1.5). That is marginally over the 5 s budget on the small VM and about 1.4 s on a laptop. It runs once, only on an empty database.

**Query plans.** Filtered list queries use `idx_employee_country_department_title` and name search uses `idx_employee_full_name`. The perf test fails if an indexed filter falls back to a full `SCAN employee`. Sorting by salary sorts the filtered set in memory because the value is a computed USD amount; at 10k rows this is well within budget.

## 4. Known limits and scale-up path
- SQLite has a single writer. This is fine for one HR user. For many concurrent editors, move to **PostgreSQL**. The repository layer isolates SQL, so this is a contained change.
- The VM has about 1 GB RAM, so builds happen on the developer machine and only the jar is deployed.
- At 1M+ rows: keyset (cursor) pagination instead of OFFSET, materialised aggregates, and FTS or a search index.
- OFFSET pagination gets slower on deep pages. At 10k rows the cost is negligible, and keyset pagination is noted as future work.
