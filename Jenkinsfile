@Library('Shared') _
parameters {
    string(name: 'DOCKER_TAG', defaultValue: 'v1', description: 'Docker image tag')
    string(name: 'SONAR_PROJECT_KEY', defaultValue: 'bankapp', description: 'SonarQube project key')
    string(name: 'SONAR_PROJECT_NAME', defaultValue: 'bankapp', description: 'SonarQube project name')
}

pipeline {
    agent any
    options {
        timeout(time: 30, unit: 'MINUTES')
    }
    environment {
        SONAR_HOME = tool "sonar"
        DOCKER_IMAGE = "ditiss-project"
        GIT_REPO = "https://github.com/SankalpSingh007/DevSecOps.git"
        GIT_BRANCH = "main"
        // Add SonarQube token as environment variable
        SONAR_TOKEN = credentials('sonar-token') 
    }
    stages {
        stage("Clean Workspace") {
            steps {
                cleanWs()
            }
        }

        stage("Code Clone") {
            steps {
                script {
                    code_checkout("${GIT_REPO}", "${GIT_BRANCH}")
                }
            }
        }

        stage("Security Scans") {
            parallel {
                stage("SonarQube Analysis") {
                    steps {
                        script {
                            withSonarQubeEnv('sonar') {
                                sh """
                                ${SONAR_HOME}/bin/sonar-scanner \
                                -Dsonar.projectKey=${params.SONAR_PROJECT_KEY} \
                                -Dsonar.projectName=${params.SONAR_PROJECT_NAME} \
                                -Dsonar.java.binaries=. \
                                -Dsonar.login=${SONAR_TOKEN}
                                """
                            }
                        }
                    }
                }

                stage("Trivy FS Scan") {
                    steps {
                        script {
                            // Continue on Trivy failure to see all results
                            catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                                sh "trivy fs --scanners vuln --exit-code 1 --severity CRITICAL ."
                            }
                        }
                    }
                }
            }
        }

        stage("Quality Gate Check") {
            steps {
                script {
                    // Only run quality gate if SonarQube succeeded
                    when {
                        expression { currentBuild.resultIsBetterOrEqualTo('UNSTABLE') }
                    }
                    timeout(time: 5, unit: 'MINUTES') {
                        waitForQualityGate abortPipeline: true
                    }
                }
            }
        }

        stage("Docker Build & Push") {
            when {
                expression { currentBuild.resultIsBetterOrEqualTo('UNSTABLE') }
            }
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-creds',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh "docker login -u $DOCKER_USER -p $DOCKER_PASS"
                        docker_build("${DOCKER_IMAGE}", "${params.DOCKER_TAG}", "${DOCKER_USER}")
                        docker_push("${DOCKER_IMAGE}", "${params.DOCKER_TAG}", "${DOCKER_USER}")
                    }
                }
            }
        }
    }
    post {
        always {
            echo "Pipeline execution completed"
            cleanWs()
        }
        success {
            emailext (
                subject: "SUCCESS: Pipeline ${currentBuild.fullDisplayName}",
                body: """<div>...success email template...</div>""",
                to: "sankalpisonn@gmail.com",
                mimeType: 'text/html'
            )
        }
        failure {
            emailext (
                subject: "FAILED: Pipeline ${currentBuild.fullDisplayName}",
                body: """<div>...failure email template...</div>""",
                to: "sankalpisonn@gmail.com",
                mimeType: 'text/html',
                attachLog: true
            )
        }
    }
}
