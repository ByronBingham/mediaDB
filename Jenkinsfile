pipeline {
    agent { label 'docker' }

    stages {
        stage('Build'){
            steps {
                checkout scm
                sh "docker build -t ${LOCAL_REG_URL}/bmedia_api:SNAPSHOT -f ./docker/api/ApiDockerfile ."
                sh "docker push ${LOCAL_REG_URL}/bmedia_api:SNAPSHOT"
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