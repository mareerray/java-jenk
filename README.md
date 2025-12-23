# Buy-One E‑commerce Platform (CI/CD)

This repository contains the **Buy-One** e‑commerce platform, implemented as a Spring Boot microservices backend and an Angular frontend, designed to be deployed through a Jenkins-based CI/CD pipeline.

## Tech stack

- **Backend**
    - Java + Spring Boot (multiple microservices)
    - Maven (using `mvnw` wrapper)
    - JUnit for automated tests
- **Frontend**
    - Angular (Angular CLI)
    - Jasmine/Karma for unit tests
- **Infrastructure**
    - Docker for containerization
    - Docker Compose for local orchestration
    - Jenkins (planned) for CI/CD

## Repository structure

From the repository root:

- `backend/`
    - `discovery-service/`
    - `gateway-service/`
    - `media-service/`
    - `product-service/`
    - `user-service/`
- `frontend/` – Angular client
- `docker-compose.yml` – local stack (backends + frontend + dependencies)
- `start-app.sh`, `start-backend.sh`, `start-frontend.sh`, `stop-app.sh` – local helper scripts

Each backend service is an independent Spring Boot Maven project with its own `pom.xml` and `mvnw` wrapper.

## Branching and workflow

This repo is structured for a simple, CI-friendly workflow:

- `main`
    - Always stable and deployable.
    - Jenkins runs **full pipeline** (build + test all microservices and frontend, then deploy).
- `feature/*`
    - Used for feature development.
    - Jenkins runs **build + tests only**, no deployment.

Pull requests into `main` should only be merged when the pipeline is green.

## Build and test commands

The CI pipeline and local developers should use the same commands.

### Backend (Spring Boot microservices)

From repository root, per service:

```bash
cd backend/discovery-service      && ./mvnw clean verify
cd backend/gateway-service       && ./mvnw clean verify
cd backend/media-service         && ./mvnw clean verify
cd backend/product-service       && ./mvnw clean verify
cd backend/user-service          && ./mvnw clean verify
```

Conceptually, CI will iterate over these services and run `./mvnw clean verify` for each. Any failing service should fail the pipeline.

For tests only (if needed):

```bash
cd backend/<service-name> && ./mvnw test
```

### Frontend (Angular)

From repository root:

Install dependencies:

```bash
cd frontend
npm ci
```

Run unit tests (CI-friendly, non-watch mode):

```bash
cd frontend
ng test --no-watch --no-progress
```

Build production bundle:

```bash
cd frontend
ng build --configuration production
```

These are the same commands that the Jenkins pipeline will run in the frontend stages.

## Local development

### Running backend services

For development, you can run services individually with:

```bash
cd backend/<service-name>
./mvnw spring-boot:run
```

Or use the provided scripts (if configured):

```bash
./start-backend.sh
./stop-app.sh
```

### Running frontend

For local development with live reload:

```bash
cd frontend
npm ci
npm start
```

Check `proxy.conf.json` and environment files for API base URLs and ports.

### Docker Compose

When fully wired, the application can be started with:

```bash
docker-compose up --build
```

This will build and run the backend microservices and frontend according to `docker-compose.yml`.

## CI/CD plan (Jenkins)

This section describes the intended Jenkins pipeline behavior.

### Stages (high level)

1. **Checkout**
    - Pull code from primary remote (Gitea).
2. **Backend build & test**
    - For each service in `backend/`:
        - Run `./mvnw clean verify`.
3. **Frontend build & test**
    - `npm ci`
    - `ng test --no-watch --no-progress`
    - `ng build --configuration production`
4. **Package & deploy**
    - Build Docker images for services and frontend.
    - Deploy to target environment (local server / Docker host / cloud).
5. **Post actions**
    - Notify team (email or Slack) on success or failure.
    - If deployment fails, trigger a rollback to the last known good version.

Deployment and rollback scripts will be added under a `ci/` or `scripts/` directory as the pipeline evolves.

## Remotes and hosting

- **Primary remote**: self-hosted Git server (Gitea), used by Jenkins as the main source of truth.
- **Secondary remote**: GitHub, used as an external mirror for backup and remote access.

Local Git example:

```bash
git remote add origin  <gitea-url>
git remote add github  <github-url>
git push origin main
git push github main
```

## Contribution and future work

Planned improvements:

- Add Jenkinsfile with full CI pipeline (build, test, deploy, notify).
- Add environment-specific profiles for staging/production.
- Parameterized builds (select environment, subset of services).
- Distributed builds using multiple Jenkins agents.

---