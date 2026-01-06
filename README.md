# Safe Zone - Microservices CI/CD Pipeline

Complete automated CI/CD pipeline with Jenkins and SonarQube code quality analysis.

# Project Overview

**Backend Services** (Spring Boot + Maven):
- Discovery Service (service registry)
- Gateway Service (API gateway)
- User Service (user management)
- Product Service (product catalog)
- Media Service (media handling)

**Frontend** (Angular + npm):
- Web UI for the Safe Zone application

**CI/CD Pipeline**:
- Automated builds and tests
- Code quality analysis with SonarQube
- Docker image building
- Automated deployment
- Slack notifications

# Prerequisites

- Docker (version 20.10+)
- Docker Compose (version 1.29+)
- Ports 9090 (Jenkins) and 9000 (SonarQube) available
- 4GB RAM minimum

# Installation & Startup

### Step 1: Start SonarQube (First - it takes longer to initialize)
````
docker-compose -f sonarqube-compose.yml up -d
````

- Wait 60 seconds for SonarQube to initialize.

- URL: http://localhost:9000

>Username: admin
Password: admin

### Step 2: Start Jenkins (Second - it will auto-connect to SonarQube)

````
docker-compose up -d
````
- Wait 60 seconds for Jenkins to start and automatically connect to SonarQube network.

- URL: http://localhost:9090

>Initial Setup: First login will prompt for initial password (check docker logs jenkins)

## Running the Pipeline
- Open Jenkins: http://localhost:9090

- Select the safe-zone pipeline job

- Click "Build Now"

- Monitor the pipeline in Pipeline Overview

Pipeline Stages
The build will execute these stages automatically:

````
✓ Checkout                                    (get code from Git)
  ├─ ✓ Backend Build - discovery-service
  ├─ ✓ Backend Build - gateway-service
  ├─ ✓ Backend Build - user-service
  ├─ ✓ Backend Build - product-service
  └─ ✓ Backend Build - media-service
  
  ├─ ✓ Backend Tests - discovery-service
  ├─ ✓ Backend Tests - gateway-service
  ├─ ✓ Backend Tests - user-service
  ├─ ✓ Backend Tests - product-service
  └─ ✓ Backend Tests - media-service
  
  └─ ✓ Frontend - Tests Included
  
  ├─ ✓ SonarQube Analysis - Backend          (analyzes all 5 services)
  └─ ✓ SonarQube Analysis - Frontend         (analyzes Angular app)
  
  ├─ ✓ Build Images                          (Docker image building)
  ├─ ✓ Deploy & Verify                       (deployment + health checks)
  └─ ✓ Post Actions                          (Slack notifications)
  ````
Build Time: Approximately 2-3 minutes for full pipeline

## Viewing Results
#### Jenkins Pipeline
- URL: http://localhost:9090/job/safe-zone/

- View all builds, logs, and stage details

- Click on any build number to see details

#### SonarQube Code Quality
- URL: http://localhost:9000/projects

- Projects Analyzed:

    - Safe Zone - Discovery Service

    - Safe Zone - Gateway Service

    - Safe Zone - User Service

    - Safe Zone - Product Service

    - Safe Zone - Media Service

    - Safe Zone - Frontend

- Each project shows:

    - ✅ Code quality metrics

    - ✅ Security vulnerabilities

    - ✅ Code coverage

    - ✅ Maintainability ratings

    - ✅ Test coverage

# Architecture
### Docker Network Setup
Jenkins and SonarQube are automatically connected via the java-jenk_sonarnet Docker network:

- Jenkins Container: jenkins (http://jenkins:8080 internally)

- SonarQube Container: sonarqube (http://sonarqube:9000 internally)

- Database Container: sonarqube-db (PostgreSQL)

- Network: java-jenk_sonarnet (bridge network)

>Key: No manual docker network connect commands needed - fully automated in docker-compose.yml

### How Jenkins Talks to SonarQube
Inside the pipeline, Jenkins:

1. Compiles Java code with Maven → generates target/classes

2. Runs tests → generates test reports

3. Executes sonar-scanner with compiled classes

4. Sends analysis to SonarQube via: http://sonarqube:9000 (internal Docker DNS)

5. SonarQube stores results in PostgreSQL database

6. Results visible in SonarQube dashboard

### Configuration Files
- docker-compose.yml (Jenkins)
````
services:
  jenkins:
    build: .                              # Builds from Dockerfile
    networks:
      - java-jenk_sonarnet              # Auto-connects to SonarQube network
`````

- sonarqube-compose.yml (SonarQube)
````
services:
  sonarqube:
    image: sonarqube:community
    networks:
      - java-jenk_sonarnet
  sonarqube-db:
    image: postgres:15-alpine
    networks:
      - java-jenk_sonarnet

networks:
  java-jenk_sonarnet:
    driver: bridge
`````

- Jenkinsfile (Pipeline Definition)
    - Defines all pipeline stages

    - Configures SonarQube Scanner

    - Sets up Maven builds and tests

    - Handles Docker image building

    - Manages deployment and notifications

# Troubleshooting
### 1. Containers won't start
````
# Check logs
docker logs jenkins
docker logs sonarqube

# Verify ports are available
docker ps
`````

### 2. Jenkins can't reach SonarQube
This is typically a network issue. Verify:

````
# Check network exists
docker network ls | grep java-jenk_sonarnet

# Check containers are on network
docker network inspect java-jenk_sonarnet
`````

#### Common causes:

- SonarQube not fully initialized (wait 60+ seconds)

- Containers on different networks (restart both)

- Port 9000 or 9090 already in use

### 3. SonarQube Analysis fails with "java.io.IOException"
Ensure all 3 containers are running:

````
docker ps
`````

Should show:

- jenkins container (port 9090)

- sonarqube container (port 9000)

- sonarqube-db container (internal)

### 4. SonarQube Analysis fails with "compiled classes not found"
This is already fixed in the Jenkinsfile with:

```groovy
-Dsonar.java.binaries=target/classes
`````

The -Dsonar.java.binaries parameter tells SonarQube where compiled Java classes are located.

# Cleanup
## Stop all containers
````
docker-compose down
docker-compose -f sonarqube-compose.yml down
`````

## Remove all data (databases, volumes)
````
docker-compose down -v
docker-compose -f sonarqube-compose.yml down -v
`````

## Remove images (optional)
````
docker rmi jenkins:latest
docker rmi sonarqube:community
docker rmi postgres:15-alpine
`````

# Technology Stack
|Component	|Technology	|Version |
| ---------- | --------- | ------- |
|CI/CD	| Jenkins	|Latest |
|Code Quality	|SonarQube Community	|v25.12.0|
|Backend Framework	|Spring Boot	|3.x|
|Backend Build	|Maven	|3.9|
|Frontend Framework	|Angular	|17+|
|Frontend Build	|npm	|10+|
|Database (SonarQube)	|PostgreSQL	|15|
|Containerization	|Docker	|20.10+|
|Orchestration	|Docker Compose	|1.29+|


# Key Features
✅ Fully Automated: No manual network configuration needed

✅ Parallel Builds: All services build simultaneously (faster CI)

✅ Code Quality: All 6 projects analyzed by SonarQube

✅ Security Scanning: Vulnerabilities detected automatically

✅ Instant Feedback: Developers get analysis results 
immediately

✅ Reproducible: Same setup works on any machine

✅ Auditor-Friendly: No manual steps, just docker-compose up

# Audit Readiness
This project is production-ready for audits and evaluations:

✅ All services containerized with Docker

✅ No manual configuration steps required

✅ Fully automated CI/CD pipeline

✅ Code quality metrics tracked

✅ Security vulnerabilities identified

✅ Clear documentation provided

✅ Reproducible setup with docker-compose

To evaluate: Just run docker-compose up and watch the pipeline!


# Created By

- **Mayuree Reunsati** - [GitHub](https://github.com/mareerray)
- **Joon Kim** - [GitHub](https://github.com/kurizma)

Last Updated: January 6, 2026