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
                                -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                                -Dsonar.projectName=${SONAR_PROJECT_NAME} \
                                -Dsonar.java.binaries=. \
                                -Dsonar.login=${SONAR_AUTH_TOKEN}
                                """
                            }
                        }
                    }
                }

                stage("Trivy FS Scan") {
                    steps {
                        sh "trivy fs --security-checks vuln --exit-code 1 --severity CRITICAL ."
                    }
                }
            }
        }

        stage("Quality Gate Check") {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage("Docker Build & Push") {
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
