pipeline {
    agent any

    tools {
        maven "M3"
        jdk 'jdk-17'
    }

    environment {
        DOCKER_IMAGE = 'hypersauce/my-app'
        DOCKER_TAG = 'latest'
        REMOTE_SERVER = 'vboxuser@192.168.1.106'
    }

    stages {
        stage('Build') {
            steps {
                git 'https://github.com/HyperWasHere/Project'
                script {
                    if (!isUnix()) {
                        bat "mvn clean package"
                    }
                    else{
                        sh "mvn clean package"
                    }
                }            
            }

            post {
                success {
                    junit '**/target/surefire-reports/TEST-*.xml'
                    archiveArtifacts 'target/*.jar'
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    docker.build("${DOCKER_IMAGE}:${DOCKER_TAG}")
                }
            }
        }

        stage('Push Docker Image to DockerHub') {
            steps {
                script {
                    docker.withRegistry('', 'hypersauce-dockerhub') {
                        docker.image("${DOCKER_IMAGE}:${DOCKER_TAG}").push()
                    }
                }
            }
        }
        stage('Deploy to Remote Server') {
            steps {
                script {
                    sshagent(credentials: ['ssh-credentials']) {
                        sh """
                            # SSH into the remote server and run docker commands
                            ssh -o StrictHostKeyChecking=no ${REMOTE_SERVER}
                                # Pull the Docker image from DockerHub
                                docker pull ${DOCKER_IMAGE}:${DOCKER_TAG}
                                # Run the new container
                                docker run -d -d -p 8081:8081 ${DOCKER_IMAGE}:${DOCKER_TAG}
                        """
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'Build succeeded!'
        }

        failure {
            echo 'Build failed!'
        }
    }
}
