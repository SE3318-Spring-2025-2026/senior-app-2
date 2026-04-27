# Contributing

Thanks for contributing to Senior App.

## Development Setup

- Backend: `cd backend && env -u MYSQL_PASSWORD mvn -Dmaven.test.skip=true spring-boot:run`
- Frontend: `cd frontend && npm run dev`
- Default local DB config is MySQL on port `3333` with `root` and empty password.

## Branching

- Create a descriptive branch from `main`:
  - `feat/<short-name>`
  - `fix/<short-name>`
  - `chore/<short-name>`
- Keep PRs focused and small when possible.

## Commit Messages

- Prefer clear, imperative messages.
- Examples:
  - `fix: allow students to access own project detail`
  - `chore: add contribution guide`

## Code Guidelines

- Keep changes minimal and aligned with existing style.
- Add/adjust authorization rules carefully; prefer least-privilege access.
- Reuse service-layer validation for role and ownership checks.
- Avoid unrelated refactors in the same PR.

## Verification Before PR

- Backend compile: `cd backend && mvn -q -Dmaven.test.skip=true compile`
- Frontend build: `cd frontend && npm run build`
- Manually smoke test changed user flows (especially auth/role-specific screens).

## Pull Request Checklist

- Include a short summary of **what** changed and **why**.
- Add test steps with expected outcomes.
- Mention any DB/config assumptions.
- Ensure no secrets or local-only credentials are committed.
