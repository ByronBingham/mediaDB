pipeline {
    agent any

    stages {
        stage('Build'){
            steps {
                sh "docker --no-cache build -t bmedia_api -f ./docker/api/ApiDockerfile ."
            }            
        }
        stage('Test'){
            steps {
                echo "TODO..."
            }
        }
        stage('Deploy'){
            steps{
                echo "TODO"
            }            
        }
    }
}