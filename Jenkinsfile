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

        // SONAR_TOKEN = credentials('sonarqube-token')
        // SONAR_HOST_URL = credentials('sonarqube-host-url')

        // Image versioning
        VERSION    = "v${env.BUILD_NUMBER}"
        STABLE_TAG = "stable"
    }

    // tools {
    //     maven 'maven-3.9'
    //     nodejs 'node-20.19.6'
    // }

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
                        credentialsId: 'github-safe-zone-token'
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
                        sh 'npx ng build --configuration production'
                    
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
                                    npm ci
                                    sonar-scanner \
                                        -Dsonar.projectKey=safe-zone-frontend \
                                        -Dsonar.projectName="Safe Zone - Frontend" \
                                        -Dsonar.sources=src \
                                        -Dsonar.java.binaries=target/classes \
                                        -Dsonar.exclusions=**/*.spec.ts,node_modules/** \
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
         * Quality Gate Check       *
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
                        // Cleanup old containers
                        echo "Cleaning up old versioned containers..."
                        sh '''
                            docker compose -f docker-compose.yml down || true
                            sleep 3
                        '''

                        try {
                            echo "Deploying version: ${VERSION}"

                            // Now deploy the new version
                            withEnv(["IMAGE_TAG=${VERSION}"]) {
                                sh 'docker compose -f docker-compose.yml up -d'

                                echo "Waiting for services to stabilize..."
                                sleep 15

                                sh """
                                    if docker compose -f docker-compose.yml ps | grep "Exit"; then
                                        echo "Detected crashed containers!"
                                        exit 1
                                    fi
                                """
                            }

                            echo "Deployment verification passed. Tagging images as stable..."

                            sh """
                                docker tag frontend:${VERSION}          frontend:${STABLE_TAG}          || true
                                docker tag discovery-service:${VERSION} discovery-service:${STABLE_TAG} || true
                                docker tag gateway-service:${VERSION}   gateway-service:${STABLE_TAG}   || true
                                docker tag user-service:${VERSION}      user-service:${STABLE_TAG}      || true
                                docker tag product-service:${VERSION}   product-service:${STABLE_TAG}   || true
                                docker tag media-service:${VERSION}     media-service:${STABLE_TAG}     || true
                            """

                            echo "Re-deploying using stable tag..."

                            withEnv(["IMAGE_TAG=${STABLE_TAG}"]) {
                                sh 'docker compose -f docker-compose.yml up -d'
                            }

                        } catch (Exception e) {
                            echo "Deployment failed or crashed! Initiating rollback..."
                            echo "Reason: ${e.getMessage()}"

                            try {
                                withEnv(["IMAGE_TAG=${STABLE_TAG}"]) {
                                    sh 'docker compose -f docker-compose.yml up -d'
                                }
                                echo "Rolled back to previous stable version."

                                withCredentials([string(credentialsId: 'webhook-slack-safe-zone', variable: 'SLACK_WEBHOOK_URL')]) {
                                    sh '''
                                    curl -sS -X POST -H "Content-type: application/json" --data "{
                                        \\"text\\": \\":information_source: Rollback SUCCESSFUL!\\n*Job:* ${JOB_NAME}\\n*Build:* ${BUILD_NUMBER}\\n*Branch:* ${BRANCH}\\"
                                    }" "${SLACK_WEBHOOK_URL}" || echo "Slack notification failed (non-fatal)"
                                    '''
                                }
                            } catch (Exception rollbackErr) {
                                echo "FATAL: Rollback failed!"
                                echo "Reason: ${rollbackErr.getMessage()}"

                                withCredentials([string(credentialsId: 'webhook-slack-safe-zone', variable: 'SLACK_WEBHOOK_URL')]) {
                                    sh '''
                                    curl -sS -X POST -H "Content-type: application/json" --data "{
                                        \\"text\\": \\":rotating_light: Rollback FAILED\\n*Job:* ${JOB_NAME}\\n*Build:* ${BUILD_NUMBER}\\n*Branch:* ${BRANCH}\\n*Reason:* see Jenkins logs\\"
                                    }" "${SLACK_WEBHOOK_URL}" || echo "Slack notification failed (non-fatal)"
                                    '''
                                }
                            }

                            error "Deployment failed - rollback executed."
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
                // junit 'backend/*/target/surefire-reports/*.xml'
                // archiveArtifacts artifacts: 'backend/*/target/surefire-reports/*.xml', allowEmptyArchive: true

                // junit allowEmptyResults: true, testResults: 'frontend/test-results/junit/*.xml'
                // archiveArtifacts artifacts: 'frontend/test-results/junit/*.xml', allowEmptyArchive: true

                if (env.WORKSPACE) {
                    cleanWs notFailBuild: true
                } else {
                    echo "No workspace available; skipping cleanWs"
                }
            }
        }

        success {
            echo "Build succeeded! Sending Slack notification..."
            withCredentials([string(credentialsId: 'webhook-slack-safe-zone', variable: 'SLACK_WEBHOOK_URL')]) {
                sh """
                    curl -sS -X POST -H 'Content-type: application/json' --data '{
                        "text": ":white_check_mark: Build SUCCESS\\n*Job:* ${env.JOB_NAME}\\n*Build:* ${env.BUILD_NUMBER}\\n*Branch:* ${params.BRANCH}"
                    }' "${SLACK_WEBHOOK_URL}" || echo "Slack notification failed (non-fatal)"
                """
            }
        }

        failure {
            echo "Build failed! Sending Slack notification..."
            withCredentials([string(credentialsId: 'webhook-slack-safe-zone', variable: 'SLACK_WEBHOOK_URL')]) {
                sh """
                    curl -sS -X POST -H 'Content-type: application/json' --data '{
                        "text": ":x: Build FAILED\\n*Job:* ${env.JOB_NAME}\\n*Build:* ${env.BUILD_NUMBER}\\n*Branch:* ${params.BRANCH}\\n*Result:* ${currentBuild.currentResult}"
                    }' "${SLACK_WEBHOOK_URL}" || echo "Slack notification failed (non-fatal)"
                """
            }
        }
    }
}