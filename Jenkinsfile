pipeline {
    agent any
    tools {
            maven 'maven399'
            jdk 'javajdk'
        }
    stages {
        stage('Build') {
            agent {
                        dockerContainer {
                            reuseNode true
                            image 'openjdk:11.0-jdk-slim'
                            args  '-v /var/run/docker.sock:/var/run/docker.sock --group-add 992'
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