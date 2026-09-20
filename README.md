# ACME Salary Management: Backend

Spring Boot (Java 17) + SQLite API for managing salaries of about 10,000 employees and answering "how do we pay people?".

> **Status:** planning complete, implementation in progress. See [docs/tasks.md](docs/tasks.md).

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
- App: http://140.238.231.135/
- Demo video: _tbd_

## Run locally _(filled in at T1.1)_
```bash
./mvnw spring-boot:run
./mvnw test
```

## Deploy
`./deploy.sh` runs tests, builds the jar and deploys it to the VM. One-time VM setup is in `deploy/provision-vm.sh`. The SSH key is read from `~/.ssh/acme_vm.key` and is never stored in the repo.
