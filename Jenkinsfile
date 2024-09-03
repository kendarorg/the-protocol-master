pipeline {
    agent any
    tools {
            maven 'maven399'
            jdk 'javajdk'
        }
         environment {
                DOCKER_ARGS = "-v /var/run/docker.sock:/var/run/docker.sock"
            }
    stages {
        stage('Build') {
            agent {
                        dockerContainer {
                            image 'openjdk:11.0-jdk-slim'
                        }
                    }
            steps {
                withMaven {
                    sh 'mvn -B clean install -Djunit.jupiter.execution.parallel.enabled=false'
                }
            }
        }
    }
}