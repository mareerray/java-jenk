pipeline {
    agent any

    /**********************
     * Prevent concurrent builds
     **********************/
    options {
        disableConcurrentBuilds()
        timestamps()
        timeout(time: 1, unit: 'HOURS')
    }

    /**********************
     * Global configuration
     **********************/
    parameters {
        string(name: 'BRANCH', defaultValue: 'main', description: 'Branch to build')
    }

    environment {
        // Credentials
        SLACK_WEBHOOK = credentials('webhook-slack-safe-zone')
        BRANCH = "${env.BRANCH_NAME ?: env.GIT_BRANCH ?: params.BRANCH ?: 'main'}"

        // Image versioning
        VERSION    = "v${env.BUILD_NUMBER}"
        STABLE_TAG = "stable"
    }

    tools {
        maven 'maven-3.9'
        nodejs 'node-20.19.6'
    }

    stages {

        /************
         * Checkout *
         ************/
        stage('Checkout') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: "*/${params.BRANCH}"]],
                    userRemoteConfigs: [[
                        url: 'https://github.com/mareerray/java-jenk.git',
                        credentialsId: 'github-safezone-token'
                    ]]
                ])
            }
        }

        /*************************
         * Backend build (no tests)
         *************************/
        stage('Backend Build - discovery-service') {
            steps {
                dir('backend/discovery-service') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Backend Build - gateway-service') {
            steps {
                dir('backend/gateway-service') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Backend Build - user-service') {
            steps {
                dir('backend/user-service') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Backend Build - product-service') {
            steps {
                dir('backend/product-service') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Backend Build - media-service') {
            steps {
                dir('backend/media-service') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        /***********************
         * Backend unit tests  *
         ***********************/
        stage('Backend Tests - discovery-service') {
            steps {
                dir('backend/discovery-service') {
                    sh 'mvn test'
                }
            }
        }

        stage('Backend Tests - gateway-service') {
            steps {
                dir('backend/gateway-service') {
                    sh 'mvn test'
                }
            }
        }

        stage('Backend Tests - user-service') {
            steps {
                dir('backend/user-service') {
                    sh 'mvn test'
                }
            }
        }

        stage('Backend Tests - product-service') {
            steps {
                dir('backend/product-service') {
                    sh 'mvn test'
                }
            }
        }

        stage('Backend Tests - media-service') {
            steps {
                dir('backend/media-service') {
                    sh 'mvn test'
                }
            }
        }

        /************
         * Frontend *
         ************/
        stage('Frontend - Tests Included') {
            steps {
                dir('frontend') {
                    // nodejs(nodeJSInstallationName: 'node-20.19.6') 
                    
                        sh 'npm ci'
                        sh 'npm test -- --watch=false --browsers=ChromeHeadlessNoSandbox --no-progress'
                        sh 'ls -la test-results/junit/ || echo "No test-results dir"'
                        sh 'npx ng build --configuration production'
                    
                }
            }
        }

        /************
         * Test Failure Handling → Early Slack → Skip Sonar/deploy → Post FAILURE *
         ************/
        stage('Test Summary') {
            steps {
                script {
                    def testFailed = false
                    def cleanBranch = "${BRANCH ?: GIT_BRANCH ?: 'main'}".replaceAll(/^origin\//, '')
                    try {
                        sh 'find . -name "*.xml" -path "*/surefire-reports/*.xml" | head -1 && echo "Tests passed" || testFailed = true'
                    } catch (e) {
                        testFailed = true
                    }
                    if (testFailed) {
                        withCredentials([string(credentialsId: 'webhook-slack-safe-zone', variable: 'SLACK_WEBHOOK')]) {
                            sh '''
                                curl -sS -X POST -H "Content-type: application/json" --data "{
                                    \\"text\\": \\":x: TESTS FAILED!\\n*Job:* ${JOB_NAME}\\n*Build:* ${BUILD_NUMBER}\\n*Branch:* ${cleanBranch}
                                }" "${SLACK_WEBHOOK}"
                            '''
                        }
                        error "Tests failed - aborting deploy"
                    }
                }
            }
        }


        /****************************
        * SonarQube Code Analysis *
        ****************************/
        stage('SonarQube Analysis - Backend') {
            steps {
                script {
                    def scannerHome = tool name: 'SonarQube Scanner', type: 'hudson.plugins.sonar.SonarRunnerInstallation'
                    env.PATH = "${scannerHome}/bin:${env.PATH}"

                    withSonarQubeEnv('SonarQube Dev') {              
                        withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN'), 
                                    string(credentialsId: 'sonarqube-host-url', variable: 'SONAR_HOST')]) {
                            dir('backend/discovery-service') {
                                sh '''
                                    sonar-scanner \
                                        -Dsonar.projectKey=safe-zone-discovery-service \
                                        -Dsonar.projectName="Safe Zone - Discovery Service" \
                                        -Dsonar.sources=src \
                                        -Dsonar.java.binaries=target/classes \
                                        -Dsonar.host.url=${SONAR_HOST} \
                                        -Dsonar.login=${SONAR_TOKEN}
                                '''
                            }
                            dir('backend/gateway-service') {
                                sh '''
                                    sonar-scanner \
                                        -Dsonar.projectKey=safe-zone-gateway-service \
                                        -Dsonar.projectName="Safe Zone - Gateway Service" \
                                        -Dsonar.sources=src \
                                        -Dsonar.java.binaries=target/classes \
                                        -Dsonar.host.url=${SONAR_HOST} \
                                        -Dsonar.login=${SONAR_TOKEN}
                                '''
                            }
                            dir('backend/user-service') {
                                sh '''
                                    sonar-scanner \
                                        -Dsonar.projectKey=safe-zone-user-service \
                                        -Dsonar.projectName="Safe Zone - User Service" \
                                        -Dsonar.sources=src \
                                        -Dsonar.java.binaries=target/classes \
                                        -Dsonar.host.url=${SONAR_HOST} \
                                        -Dsonar.login=${SONAR_TOKEN}
                                '''
                            }
                            dir('backend/product-service') {
                                sh '''
                                    sonar-scanner \
                                        -Dsonar.projectKey=safe-zone-product-service \
                                        -Dsonar.projectName="Safe Zone - Product Service" \
                                        -Dsonar.sources=src \
                                        -Dsonar.java.binaries=target/classes \
                                        -Dsonar.host.url=${SONAR_HOST} \
                                        -Dsonar.login=${SONAR_TOKEN}
                                '''
                            }
                            dir('backend/media-service') {
                                sh '''
                                    sonar-scanner \
                                        -Dsonar.projectKey=safe-zone-media-service \
                                        -Dsonar.projectName="Safe Zone - Media Service" \
                                        -Dsonar.sources=src \
                                        -Dsonar.java.binaries=target/classes \
                                        -Dsonar.host.url=${SONAR_HOST} \
                                        -Dsonar.login=${SONAR_TOKEN}
                                '''
                            }
                        }
                    }
                }
            }
        }

        stage('SonarQube Analysis - Frontend') {
            steps {
                dir('frontend') {
                    script {
                        def scannerHome = tool name: 'SonarQube Scanner', type: 'hudson.plugins.sonar.SonarRunnerInstallation'
                        env.PATH = "${scannerHome}/bin:${env.PATH}"

                        withSonarQubeEnv('SonarQube Dev') {
                            withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN'),
                                        string(credentialsId: 'sonarqube-host-url', variable: 'SONAR_HOST')]) {
                                sh '''
                                    sonar-scanner \
                                        -Dsonar.projectKey=safe-zone-frontend \
                                        -Dsonar.projectName="Safe Zone - Frontend" \
                                        -Dsonar.sources=src \
                                        -Dsonar.exclusions=**/*.spec.ts,node_modules/**,dist/**,coverage/** \
                                        -Dsonar.cpd.exclusions=**/*.spec.ts,node_modules/** \
                                        -Dsonar.host.url=${SONAR_HOST} \
                                        -Dsonar.login=${SONAR_TOKEN}
                                '''
                            }
                        }
                    }
                }
            }
        }

        /****************************
         * Quality Gate Check → Skip deploy → Post FAILURE Slack *
         ****************************/
        stage('Quality Gate Check') {
            steps {
                script {
                    echo 'Checking SonarQube Quality Gate...'
                    timeout(time: 5, unit: 'MINUTES') {
                        waitForQualityGate abortPipeline: true
                    }
                }
            }
        }

        /************************
         * Build Docker images  *
         ************************/
        stage('Build Images') {
            steps {
                script {
                    echo "Building Docker images with tag: ${VERSION}"
                    dir("${env.WORKSPACE}") {
                        withEnv(["IMAGE_TAG=${VERSION}"]) {
                            sh 'docker compose -f docker-compose.yml build'
                        }
                    }
                }
            }
        }

        /******************************
         * Deploy, verify, and rollback
         ******************************/
        stage('Deploy & Verify') {
            steps {
                script {
                    dir("${env.WORKSPACE}") {
                        def cleanBranch = "${BRANCH ?: GIT_BRANCH ?: 'main'}".replaceAll(/^origin\//, '')
                        
                        // Cleanup old containers
                        sh 'docker compose down || true'
                        sleep 3

                        try {
                            echo "Building and tagging ${VERSION} as potential stable"
                            
                            // Build ALL services, tag as VERSION and STABLE
                            withEnv(["IMAGE_TAG=${VERSION}"]) {
                                // Fail fast if frontend can't build/pull
                                sh 'docker compose build frontend || exit 1'
                                sh 'docker compose build --pull'  
                                sh '''
                                    docker tag frontend:${VERSION} frontend:${STABLE_TAG} frontend:build-${BUILD_NUMBER} || true
                                    docker tag discovery-service:${VERSION} discovery-service:${STABLE_TAG} discovery-service:build-${BUILD_NUMBER} || true
                                    docker tag gateway-service:${VERSION} gateway-service:${STABLE_TAG} gateway-service:build-${BUILD_NUMBER} || true
                                    docker tag user-service:${VERSION} user-service:${STABLE_TAG} user-service:build-${BUILD_NUMBER} || true
                                    docker tag product-service:${VERSION} product-service:${STABLE_TAG} product-service:build-${BUILD_NUMBER} || true
                                    docker tag media-service:${VERSION} media-service:${STABLE_TAG} media-service:build-${BUILD_NUMBER} || true
                                '''
                                
                                // Deploy new version for verification
                                sh 'docker compose up -d'
                                sleep 20
                                
                                // Strong health check
                                sh '''
                                    timeout 30 bash -c "until docker compose ps | grep -q Up && curl -f http://localhost:4200 || curl -f http://localhost:8080/health; do sleep 2; done" || exit 1
                                    if docker compose ps | grep -q "Exit"; then exit 1; fi
                                '''
                            }
                            echo "✅ New deploy verified - promoted build-${BUILD_NUMBER} to stable"
                            currentBuild.result = 'SUCCESS'

                        } catch (Exception e) {
                            def stableTag = env.STABLE_TAG ?: 'latest'

                            echo "❌ Deploy failed: ${e.message}"

                            withCredentials([string(credentialsId: 'webhook-slack-safe-zone', variable: 'SLACK_WEBHOOK')]) {
                                sh """
                                    curl -sS -X POST -H 'Content-type: application/json' \\
                                        --data '{\"text\":\"🚨 Rollback #${BUILD_NUMBER} → ${stableTag}\"}' \$SLACK_WEBHOOK 
                                """
                            }

                            
                            // Rollback: Always deploy known stable
                            sh """
                                STABLE_TAG=\${STABLE_TAG:-latest}
                                docker compose down || true
                                IMAGE_TAG=\$STABLE_TAG docker compose up -d --pull never
                                sleep 10
                                docker compose ps  # Verify
                                echo "✅ Rolled back to ${stableTag}"
                            """                       
                        currentBuild.result = 'UNSTABLE'
                        throw e
                        }
                    }
                }
            }
        }
    }
    // end of stages

    post {
        always {
            script {
                
                def buildState = currentBuild.currentResult?.toLowerCase() ?: 'success'
                def ghState = (buildState == 'success') ? 'success' : 'failure'
                def cleanBranch = "${BRANCH ?: GIT_BRANCH ?: 'main'}".replaceAll(/^origin\//, '')

                withCredentials([string(credentialsId: 'webhook-slack-safe-zone', variable: 'SLACK_WEBHOOK')]) {
                    def emoji = (buildState == 'success') ? ':white_check_mark:' : ':x:'
                    sh """
                        curl -sS -X POST \\
                            -H 'Content-type: application/json' \\
                            -d '{"text":"${emoji} *${buildState.toUpperCase()}*\\nJob: ${JOB_NAME}\\nBuild: #${BUILD_NUMBER}\\nBranch: ${cleanBranch}\\nCommit: <https://github.com/mareerray/java-jenk/commit/${GIT_COMMIT}|${GIT_COMMIT[0..7]}>"}' \\
                            \$SLACK_WEBHOOK || true
                    """
                }

                cleanWs notFailBuild: true

                archiveArtifacts artifacts: 'backend/*/target/surefire-reports/*.xml', allowEmptyArchive: true
                archiveArtifacts artifacts: 'frontend/test-results/junit/*.xml', allowEmptyArchive: true
                junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                junit allowEmptyResults: true, testResults: '**/test-results/junit/*.xml'
                
                if (env.GIT_COMMIT) {
                    withCredentials([string(credentialsId: 'github-safezone-token', variable: 'GITHUB_TOKEN')]) {
                        sh """
                            curl -s -H "Authorization: token \${GITHUB_TOKEN}" \\
                                -X POST -H "Accept: application/vnd.github.v3+json" \\
                                -d '{"state":"${ghState}", "context":"safezone", "description":"Jenkins ${buildState}", "target_url":"${BUILD_URL}"}' \\
                                https://api.github.com/repos/mareerray/java-jenk/statuses/\${GIT_COMMIT}
                        """
                        sh """
                            curl -s -H "Authorization: token \${GITHUB_TOKEN}" \\
                                -X POST -H "Accept: application/vnd.github.v3+json" \\
                                -d '{"state":"${ghState}", "context":"safe-quality-gate", "description":"Quality gate ${buildState}"}' \\
                                https://api.github.com/repos/mareerray/java-jenk/statuses/\${GIT_COMMIT}
                        """
                    }
                }
            }
        }
    }
}