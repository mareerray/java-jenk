pipeline {
    agent any

    tools {
        maven 'maven-3.9'
    }

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
        stage('Frontend') {
            steps {
                dir('frontend') {
                    nodejs(nodeJSInstallationName: 'node-20.19.6') { // exact NodeJS tool name
                        sh 'npm ci'
                        // sh 'npx ng test --no-watch --no-progress'
                        sh 'npx ng build --configuration production'
                    }
                }
            }
        }
    }
}
