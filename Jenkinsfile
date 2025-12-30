pipeline {
    agent any

    /**********************
     * Global configuration
     **********************/
    parameters {
        string(name: 'BRANCH', defaultValue: 'main', description: 'Branch to build')
    }

    environment {
        // Credentials
        SLACK_WEBHOOK = credentials('slack-webhook')

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
                        url: 'https://github.com/kurizma/java-jenk.git',
                        credentialsId: 'github-java-jenk'
                    ]]
                ])
            }
        }

        /***********************
         * Backend microservices
         ***********************/
        stage('Backend - discovery-service') {
            steps {
                dir('backend/discovery-service') {
                    sh 'mvn clean verify'
                }
            }
        }

        stage('Backend - gateway-service') {
            steps {
                dir('backend/gateway-service') {
                    sh 'mvn clean verify'
                }
            }
        }

        stage('Backend - user-service') {
            steps {
                dir('backend/user-service') {
                    sh 'mvn clean verify'
                }
            }
        }

        stage('Backend - product-service') {
            steps {
                dir('backend/product-service') {
                    sh 'mvn clean verify'
                }
            }
        }

        stage('Backend - media-service') {
            steps {
                dir('backend/media-service') {
                    sh 'mvn clean verify'
                }
            }
        }

        /************
         * Frontend *
         ************/
        stage('Frontend') {
            steps {
                dir('frontend') {
                    // Use global NodeJS tool
                    nodejs(nodeJSInstallationName: 'node-20.19.6') {
                        sh 'npm ci'
                        // Adjust test command as needed for Karma/Jasmine JUnit output
                        sh 'npm test -- --watch=false --browsers=ChromeHeadlessNoSandbox --no-progress'
                        sh 'npx ng build --configuration production'
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
                            // Ensure docker-compose.yml uses ${IMAGE_TAG} for image tags
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
                        try {
                            echo "Deploying version: ${VERSION}"

                            // Deploy NEW version (do not down first to avoid hard downtime if possible)
                            withEnv(["IMAGE_TAG=${VERSION}"]) {
                                sh 'docker compose -f docker-compose.yml up -d'

                                echo "Waiting for services to stabilize..."
                                sleep 15

                                // Basic health check: fail if any container is in Exit state
                                sh """
                                    if docker compose -f docker-compose.yml ps | grep "Exit"; then
                                        echo "Detected crashed containers!"
                                        exit 1
                                    fi
                                """
                            }

                            echo "Deployment verification passed. Tagging images as stable..."

                            // Tag images with stable tag after successful verification
                            sh """
                                docker tag frontend:${VERSION}          frontend:${STABLE_TAG}          || true
                                docker tag discovery-service:${VERSION} discovery-service:${STABLE_TAG} || true
                                docker tag gateway-service:${VERSION}   gateway-service:${STABLE_TAG}   || true
                                docker tag user-service:${VERSION}      user-service:${STABLE_TAG}      || true
                                docker tag product-service:${VERSION}   product-service:${STABLE_TAG}   || true
                                docker tag media-service:${VERSION}     media-service:${STABLE_TAG}     || true
                            """

                        } catch (Exception e) {
                            echo "Deployment failed or crashed! Initiating rollback..."
                            echo "Reason: ${e.getMessage()}"

                            // ROLLBACK: redeploy previous stable images
                            try {
                                withEnv(["IMAGE_TAG=${STABLE_TAG}"]) {
                                    sh 'docker compose -f docker-compose.yml up -d'
                                }
                                echo "Rolled back to previous stable version."

                                // Slack notification for successful rollback
                                sh """
                                curl -X POST -H 'Content-type: application/json' --data '{
                                    "text": ":information_source: Rollback SUCCESSFUL!\\n*Job:* ${env.JOB_NAME}\\n*Build:* ${env.BUILD_NUMBER}\\n*Branch:* ${params.BRANCH}"
                                }' ${env.SLACK_WEBHOOK}
                                """
                            } catch (Exception rollbackErr) {
                                echo "FATAL: Rollback failed!"
                                echo "Reason: ${rollbackErr.getMessage()}"
                                sh """
                                curl -X POST -H 'Content-type: application/json' --data '{
                                    "text": ":rotating_light: Rollback FAILED!\\n*Reason:* ${rollbackErr.getMessage()}\\n*Job:* ${env.JOB_NAME}\\n*Build:* ${env.BUILD_NUMBER}\\n*Branch:* ${params.BRANCH}\\nManual intervention needed!"
                                }' ${env.SLACK_WEBHOOK}
                                """
                            }

                            // Mark build as failed after rollback attempt
                            error "Deployment failed - rollback executed."
                        }
                    }
                }
            }
        }
    } // <--- end of stages

    post {
        always {
            script {
                // Backend JUnit reports
                junit 'backend/*/target/surefire-reports/*.xml'
                archiveArtifacts artifacts: 'backend/*/target/surefire-reports/*.xml', allowEmptyArchive: true

                // Frontend JUnit-style reports (adjust path to your Karma/JUnit output)
                junit 'frontend/test-results/junit/*.xml'
                archiveArtifacts artifacts: 'frontend/test-results/junit/*.xml', allowEmptyArchive: true

                if (env.WORKSPACE) {
                    cleanWs notFailBuild: true
                } else {
                    echo "No workspace available; skipping cleanWs"
                }
            }
        }

        success {
            echo "Build succeeded! Sending Slack notification..."
            sh """
            curl -v -X POST -H 'Content-type: application/json' --data '{
                "text": ":white_check_mark: Build SUCCESS\\\\n*Job:* ${env.JOB_NAME}\\\\n*Build:* ${env.BUILD_NUMBER}\\\\n*Branch:* ${params.BRANCH}"
            }' ${env.SLACK_WEBHOOK}
            """
        }

        failure {
            echo "Build failed! Sending Slack notification..."
            sh """
            curl -v -X POST -H 'Content-type: application/json' --data '{
                "text": ":x: Build FAILED\\\\n*Job:* ${env.JOB_NAME}\\\\n*Build:* ${env.BUILD_NUMBER}\\\\n*Branch:* ${params.BRANCH}\\\\n*Result:* ${currentBuild.currentResult}"
            }' ${env.SLACK_WEBHOOK}
            """
        }
    }
}
