# Safe Zone - Startup Commands

## Startup Order (Sequential)

### Step 1: Start SonarQube First
````
docker-compose -f sonarqube-compose.yml up -d
`````

### Why SonarQube first?

- SonarQube requires PostgreSQL database initialization

- Database startup takes 60+ seconds

- Jenkins will need SonarQube to be fully operational when it starts

- Starting SonarQube first ensures the network java-jenk_sonarnet is created and ready

- Wait 60 seconds after running this command before proceeding to Step 2.

- Verification: Check SonarQube is ready

````
docker logs sonarqube | tail -20
# Look for: "SonarQube is up"
`````

### Step 2: Start Jenkins Second
````
docker-compose up -d
`````

### Why Jenkins second?

- Jenkins container automatically connects to java-jenk_sonarnet (defined in docker-compose.yml)

- Jenkins can now immediately reach SonarQube via hostname sonarqube:9000

- No manual network configuration needed (docker network connect is not required)

- Jenkins startup is faster (30-60 seconds)

- Wait 60 seconds after running this command before accessing Jenkins UI.

- Verification: Check Jenkins is ready

````
docker logs jenkins | grep "Please use the following password"
````

### Network Architecture Explanation
When both containers start in this order:

1. SonarQube starts first → Creates Docker network java-jenk_sonarnet

2. Jenkins starts second → Automatically joins network via docker-compose.yml configuration

Result: Jenkins and SonarQube can communicate internally via Docker DNS:

- Jenkins reaches SonarQube at: http://sonarqube:9000 (hostname resolution)

- NOT at http://localhost:9000 (which would be Jenkins itself)

- NOT at container IP (which changes on restart)

### Access URLs
Once both containers are running:

|Service	|URL	|Credentials|
| ---------- | ------- | ------ |
| Jenkins	| http://localhost:9090	| Check `docker logs jenkins` |
|SonarQube	| http://localhost:9000	| admin / admin |

## Complete Startup Script (One Command)
If you want to start both sequentially in one go:

````
#!/bin/bash

echo "Starting SonarQube..."
docker-compose -f sonarqube-compose.yml up -d

echo "Waiting 60 seconds for SonarQube to initialize..."
sleep 60

echo "Starting Jenkins..."
docker-compose up -d

echo "Waiting 60 seconds for Jenkins to initialize..."
sleep 60

echo ""
echo "✅ All services started successfully!"
echo ""
echo "📊 SonarQube: http://localhost:9000 (admin/admin)"
echo "🚀 Jenkins: http://localhost:9090"
echo ""
echo "Jenkins initial password:"
docker logs jenkins | grep "Please use the following password" || echo "Check docker logs jenkins"
````

Save this as `start-services.sh` and run:

````
chmod +x start-services.sh
./start-services.sh
````

### Verification Commands

Check all containers are running
````
docker ps
````

### Expected output (7 containers total):

- jenkins (port 9090:8080)

- sonarqube (port 9000:9000)

- sonarqube-db (PostgreSQL, no public port)

### Check network is created
````
docker network ls | grep java-jenk_sonarnet
Check containers are on the network
bash
docker network inspect java-jenk_sonarnet
Should show all 3 containers listed.

Check Jenkins can reach SonarQube (from inside Jenkins container)
bash
docker exec jenkins curl -s http://sonarqube:9000/api/system/status | grep status
Expected output: "status":"UP"

View logs
bash
# SonarQube logs
docker logs sonarqube

# Jenkins logs
docker logs jenkins

# Database logs
docker logs sonarqube-db
Troubleshooting Startup Issues
Problem: "Cannot connect to Docker daemon"
Solution: Ensure Docker is running

bash
docker --version
docker ps
Problem: "Port 9090 already in use"
Solution: Change port in docker-compose.yml

text
ports:
  - "9091:8080"  # Change 9090 to 9091
Problem: "Port 9000 already in use"
Solution: Change port in sonarqube-compose.yml

text
ports:
  - "9001:9000"  # Change 9000 to 9001
Problem: "Insufficient memory"
Solution: Increase Docker desktop memory to 4GB+

Docker Desktop → Preferences → Resources → Memory: 4GB

Problem: Network not found after restart
Solution: Restart both containers in order

bash
docker-compose down
docker-compose -f sonarqube-compose.yml down
# Then restart with Step 1 and Step 2 above
Problem: Jenkins can't reach SonarQube (403/connection timeout)
Solution:

Verify both containers are running: docker ps

Verify they're on the same network: docker network inspect java-jenk_sonarnet

Wait additional 60 seconds (SonarQube might still initializing)

Check SonarQube logs: docker logs sonarqube | tail -20

Cleanup Commands
Stop all containers (keep data)
bash
docker-compose down
docker-compose -f sonarqube-compose.yml down
Remove all containers and volumes (delete data)
bash
docker-compose down -v
docker-compose -f sonarqube-compose.yml down -v
Remove images (optional)
bash
docker rmi jenkins:latest sonarqube:community postgres:15-alpine
Summary Table
Step	Command	Wait Time	Purpose
1	docker-compose -f sonarqube-compose.yml up -d	60s	Initialize SonarQube + PostgreSQL + Network
2	docker-compose up -d	60s	Start Jenkins (auto-connects to network)
3	Access Jenkins at http://localhost:9090	-	View pipeline, trigger builds
4	Access SonarQube at http://localhost:9000	-	View code quality analysis
Last Updated: January 6, 2026

