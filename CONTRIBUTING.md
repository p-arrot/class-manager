# Contributing

Thanks for helping improve Class Manager.

## Before You Start

- Open an issue for larger changes so the scope can be discussed first.
- Keep changes focused. Avoid mixing unrelated refactors with feature work.
- Do not commit secrets, real student data, production `.env` files, backups, or generated upload files.

## Development Setup

Start middleware from the backend compose file:

```powershell
cd backend
docker compose up -d postgres redis minio kkfileview
```

Run the backend:

```powershell
cd backend
mvn spring-boot:run
```

Run the frontend:

```powershell
cd frontend
npm install
npm run dev
```

## Checks

Run frontend checks:

```powershell
cd frontend
npm run check
```

Run backend tests:

```powershell
cd backend
mvn test
```

Or use the Docker Maven cache helper from the repository root:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\backend-test-docker.ps1
```

## Pull Request Checklist

- The change has a clear reason and a focused scope.
- Tests or manual verification are included for user-facing behavior.
- Documentation is updated when workflows, deployment, API behavior, or credentials change.
- New configuration uses `.env.example` or `.env.intranet.example`; real secrets are not committed.
- Screenshots or reports are only committed when they are intentional documentation artifacts.

## Commit Style

Use concise imperative messages, for example:

```text
feat: add grading inbox status filters
fix: allow students to view graded task details
docs: update intranet deployment guide
```
