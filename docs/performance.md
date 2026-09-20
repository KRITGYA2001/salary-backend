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
3. Record measured numbers below (fill in during T4.1).

| Query | Measured | Budget | Notes |
|-------|----------|--------|-------|
| list, no filter, page 1 | _tbd_ | 300 ms | |
| list, country+dept, sort salary | _tbd_ | 300 ms | |
| insights by country | _tbd_ | 500 ms | |
| insights by jobTitle (widest) | _tbd_ | 500 ms | |
| seed 10k | _tbd_ | 5 s | |

## 4. Known limits and scale-up path
- SQLite has a single writer. This is fine for one HR user. For many concurrent editors, move to **PostgreSQL**. The repository layer isolates SQL, so this is a contained change.
- The VM has about 1 GB RAM, so builds happen on the developer machine and only the jar is deployed.
- At 1M+ rows: keyset (cursor) pagination instead of OFFSET, materialised aggregates, and FTS or a search index.
- OFFSET pagination gets slower on deep pages. At 10k rows the cost is negligible, and keyset pagination is noted as future work.
