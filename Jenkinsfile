pipeline {
    agent { label 'docker' }

    stages {
        stage('Build'){
            steps {
                checkout scm
                script {
                    def props = readProperties file: '.properties'
                    version = props.version
                }
                sh "docker build -t ${LOCAL_REG_URL}/bmedia_api:SNAPSHOT -f ./docker/api/ApiDockerfile ."
                sh "docker push ${LOCAL_REG_URL}/bmedia_api:${version}_SNAPSHOT"
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