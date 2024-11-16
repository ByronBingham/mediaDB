pipeline {
    agent any

    stages {
        stage('Build'){
            sh "docker build -t bmedia_api -f ./docker/api/ApiDockerfile ./docker/api/"
        }
        stage('Test'){
            echo "TODO..."
        }
        stage('Deploy'){
            echo "TODO"
        }
    }
}