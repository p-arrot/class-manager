# Security Policy

## Supported Versions

The `main` branch is the supported branch for security fixes.

## Reporting a Vulnerability

Please do not open public issues for vulnerabilities that expose credentials, student data, file access, authentication bypasses, or deployment secrets.

Report privately by contacting the repository owner through GitHub. Include:

- Affected version or commit
- Clear reproduction steps
- Expected impact
- Any relevant logs or screenshots with sensitive data removed

## Sensitive Data Rules

Never commit:

- `backend/.env` or other real environment files
- JWT secrets
- Database, Redis, or MinIO passwords
- Production IP addresses when they are sensitive
- Database backups
- MinIO data backups or uploaded user files
- Real student personal data

Use the provided example files instead:

- `backend/.env.example`
- `backend/.env.intranet.example`

## Deployment Hardening

Before production or campus intranet use:

- Change the default application administrator password.
- Change `DB_PASSWORD`, `MINIO_ROOT_PASSWORD`, and `JWT_SECRET`.
- Keep PostgreSQL, Redis, MinIO API, and the backend bound to localhost unless there is a clear reason to expose them.
- Expose only the frontend port and kkFileView preview port to normal campus clients.
- Back up PostgreSQL and MinIO regularly.
- Keep Docker images pinned and update them intentionally.
