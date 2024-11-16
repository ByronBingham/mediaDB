pipeline {
    agent any

    stages {
        stage('Build'){
            sh "docker --no-cache build -t bmedia_api -f ./docker/api/ApiDockerfile ."
        }
        stage('Test'){
            echo "TODO..."
        }
        stage('Deploy'){
            echo "TODO"
        }
    }
}