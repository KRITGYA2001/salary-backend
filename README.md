# ACME Salary Management: Backend

Spring Boot (Java 17) + SQLite API for managing salaries of about 10,000 employees and answering "how do we pay people?".

> **Status:** backend and UI complete and deployed. See [docs/tasks.md](docs/tasks.md) for the plan and [docs/performance.md](docs/performance.md) for measured results.

## Documentation
| Doc | Purpose |
|-----|---------|
| [requirements](docs/requirements.md) | One-page requirements: goal, scope, exclusions with reasons |
| [design](docs/design.md) | Architecture, ER diagram, API, sequence diagrams, deployment, trade-offs |
| [tasks](docs/tasks.md) | Task plan and incremental commit roadmap |
| [test-strategy](docs/test-strategy.md) | Test pyramid, cases, fixtures, coverage |
| [performance](docs/performance.md) | Budgets, techniques, verification |

## Related repo
Frontend: `salary-frontend` (React + MUI).

## Live
- App: deployed on an Ubuntu VM behind Nginx (URL is given in the submission email)
- Demo video: _tbd_

## Features
- Employee list with search, country, department, job title and status filters, sorting and pagination. CSV export of the filtered view.
- Hire, edit, deactivate and salary change (each salary change is kept in a history with effective date and reason).
- Pay insights for active employees in US dollars: headcount, average, median, 90th percentile, range, breakdown by country, department and job title, distribution, highest and lowest paid.
- 10,000 employees across 10 countries are seeded on first start of an empty database.

## Run locally
Needs JDK 17 and the [frontend](https://github.com/KRITGYA2001/salary-frontend) for the UI.
```bash
./mvnw spring-boot:run     # API on http://127.0.0.1:8080/api/v1, seeds 10,000 employees on first run
./mvnw test                # fast, deterministic suite (temporary SQLite file per run)
./mvnw test -Pperf -Dtest=PerformanceBudgetTest   # 10k benchmark and query plan checks
```
Then run the frontend (`npm install && npm run dev`) and open http://localhost:5173.

| Setting | Default | Purpose |
|---------|---------|---------|
| `SALARY_DB_PATH` | `./data/salary.db` | SQLite file |
| `SERVER_PORT` / `SERVER_ADDRESS` | `8080` / `127.0.0.1` | Bind address |
| `app.seed.enabled`, `app.seed.count` | `true`, `10000` | Seeding on an empty database |

## API
Base path `/api/v1`; errors use `{"error": {"code", "message", "details?"}}`. Endpoints: `GET/POST /employees`, `GET/PATCH /employees/{id}`, `POST /employees/{id}/salary`, `GET /employees/{id}/salary-history`, `POST /employees/{id}/deactivate`, `GET /employees/export.csv`, `GET /meta/filters`, `GET /insights/summary|by/{country|department|jobTitle}|distribution|top`. Full contract in [design](docs/design.md#6-api-contract-restjson-base-apiv1).

## Deploy
`./deploy.sh` runs tests, builds the jar and deploys it to the VM. One-time VM setup is in `deploy/provision-vm.sh`. Host, user and SSH key path come from a git-ignored `.env` (see `.env.example`); nothing environment specific is stored in the repo.
