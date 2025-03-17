pipeline {
    agent any

    environment {
        PROJECT_ID = 'consummate-rig-453502-q2'
        REGION = 'us-central1'
        FUNCTION_NAME = 'static-web-function'
        REPO_NAME = 'private-image-repo'
        IMAGE_NAME = 'static-web-app'
        SONAR_HOST_URL = 'http://localhost:9000'
        SCANNER_HOME = tool 'sonar-scanner'
        SONARQUBE_TOKEN = credentials('sonarqube-GCP-TOKEN')
    }

    stages {

        stage('Clone Repository') {
            steps {
                git branch: 'main', url: 'https://github.com/GitCosmicray/gcp-cloud-functions.git'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withCredentials([string(credentialsId: 'sonarqube-GCP-TOKEN', variable: 'SONARQUBE_TOKEN')]) {
                    script {
                        def scannerHome = tool name: 'sonar-scanner', type: 'hudson.plugins.sonar.SonarRunnerInstallation'
                        withEnv(["PATH+SCANNER=${scannerHome}/bin"]) {
                            sh '''
                            sonar-scanner \
                            -Dsonar.projectKey=GCP-cloud-function-static-web \
                            -Dsonar.sources=. \
                            -Dsonar.host.url=$SONAR_HOST_URL \
                            -Dsonar.login=$SONARQUBE_TOKEN
                            '''
                        }
                    }
                }
            }
        }

        stage('Authenticate with GCP') {
            steps {
                withCredentials([file(credentialsId: 'GCP-jenkins-json', variable: 'GOOGLE_CREDENTIALS_FILE')]) {
                    sh '''
                    export GOOGLE_APPLICATION_CREDENTIALS=$GOOGLE_CREDENTIALS_FILE
                    gcloud auth activate-service-account --key-file=$GOOGLE_APPLICATION_CREDENTIALS
                    gcloud config set project $PROJECT_ID
                    gcloud auth configure-docker $REGION-docker.pkg.dev
                    '''
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    sh '''
                    docker build -t $REGION-docker.pkg.dev/$PROJECT_ID/$REPO_NAME/$IMAGE_NAME:latest .
                    '''
                }
            }
        }

        stage('Push to Artifact Registry') {
            steps {
                script {
                    sh '''
                    docker push $REGION-docker.pkg.dev/$PROJECT_ID/$REPO_NAME/$IMAGE_NAME:latest
                    '''
                }
            }
        }

        stage('Deploy to Cloud Functions') {
            steps {
                script {
                    sh '''
                    gcloud functions deploy $FUNCTION_NAME \
                        --runtime python312 \
                        --region $REGION \
                        --trigger-http \
                        --allow-unauthenticated \
                        --source . \
                        --entry-point main \
                        --memory 512MB
                    '''
                }
            }
        }
    }
}
