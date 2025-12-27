pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/main']],
                    userRemoteConfigs: [[
                        url: 'https://github.com/kurizma/java-jenk.git',
                        credentialsId: 'github-java-jenk'
                    ]]
                ])
            }
        }

        stage('Backend - discovery-service') {
            steps {
                dir('backend/discovery-service') {
                    sh './mvnw clean verify'
                }
            }
        }

        stage('Backend - gateway-service') {
            steps {
                dir('backend/gateway-service') {
                    sh './mvnw clean verify'
                }
            }
        }

        stage('Backend - user-service') {
            steps {
                dir('backend/user-service') {
                    sh './mvnw clean verify'
                }
            }
        }
       stage('Backend - product-service') {
            steps {
                dir('backend/product-service') {
                    sh './mvnw clean verify'
                }
            }
        }

        stage('Backend - media-service') {
            steps {
                dir('backend/media-service') {
                    sh './mvnw clean verify'
                }
            }
        }
        stage('Frontend') {
            steps {
                dir('frontend') {
                    script {
                        docker.image('node:20-alpine').inside {
                            sh 'npm ci'
                            sh 'npx ng test --no-watch --no-progress'
                            sh 'npx ng build --configuration production'
                        }
                    }
                }
            }
        }
    }
}
