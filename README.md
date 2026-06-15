# Class Manager

Information technology classroom management system for semester-based teaching, task submission, grading, course resources, student drive files, exams, projects, learning analytics, and grade export.

The project is designed for a single-school intranet deployment. A typical production setup runs the frontend, backend, PostgreSQL, Redis, MinIO, and kkFileView through Docker Compose.

## Features

- Admin management for classes, teachers, students, imports, and password resets
- Teacher workflows for courses, semesters, lessons, resources, task publishing, grading inboxes, exams, projects, analytics, student profiles, and grade export
- Student workflows for courses, course resources, task submission, project submission with notes, exam entry, learning evaluation, and personal drive files
- Worksheet grading with per-question feedback, teacher comments, and dimension summaries
- File storage through MinIO and document preview through kkFileView
- Docker-first intranet deployment with a root `deploy.ps1` helper

## Tech Stack

| Layer | Technology |
| --- | --- |
| Frontend | Vue 3, TypeScript, Vite, Pinia, Vue Router, Naive UI, ECharts |
| Backend | Spring Boot 3, Java 21, MyBatis-Plus, Spring Security, Flyway |
| Data | PostgreSQL, Redis |
| Files | MinIO, kkFileView |
| Deployment | Docker Compose, Nginx |

## Repository Layout

```text
backend/    Spring Boot API, database migrations, Docker Compose
frontend/   Vue frontend and Nginx image
docs/       Product, API, deployment, middleware, and user-operation docs
scripts/    Deployment, health-check, and Docker-based test helpers
```

## Quick Start: Intranet Docker Deployment

From the repository root:

```powershell
.\deploy.ps1 init -ServerIp 192.168.1.100
```

Edit `backend/.env` and change at least:

- `DB_PASSWORD`
- `MINIO_ROOT_PASSWORD`
- `JWT_SECRET`
- `SERVER_IP`
- `KKFILEVIEW_BASE_URL`

Then start the system:

```powershell
.\deploy.ps1 start
```

Open:

```text
http://192.168.1.100
```

More details are in [docs/INTRANET_WINDOWS_DEPLOYMENT.md](docs/INTRANET_WINDOWS_DEPLOYMENT.md).

## Local Development

Backend:

```powershell
cd backend
docker compose up -d postgres redis minio kkfileview
mvn test
mvn spring-boot:run
```

Frontend:

```powershell
cd frontend
npm install
npm run dev
```

The frontend dev server runs on `http://localhost:5173` and proxies `/api` to `http://localhost:8080`.

## Testing

Frontend quality gate:

```powershell
cd frontend
npm run check
```

Backend tests can be run locally with Maven or through the cached Docker helper:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\backend-test-docker.ps1
```

## Documentation

- [Middleware configuration](docs/MIDDLEWARE_CONFIG.md)
- [Windows intranet deployment](docs/INTRANET_WINDOWS_DEPLOYMENT.md)
- [API notes](docs/API.md)
- [Specification](docs/SPECIFICATION.md)
- [Progress log](docs/PROGRESS.md)
- [User semester flow report](docs/USER_SEMESTER_FLOW_REPORT.md)

## Security

Do not commit real `.env` files, production IPs, JWT secrets, database passwords, MinIO passwords, backups, or uploaded files. See [SECURITY.md](SECURITY.md) for vulnerability reporting and supported deployment guidance.

## License

This project is licensed under the [MIT License](LICENSE).
