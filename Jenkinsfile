pipeline {
    agent any
    tools {
            maven 'maven399'
            jdk 'java-11-openjdk'
        }
    stages {
        stage('Build') {
            steps {
                withMaven {
                    sh 'mvn -B -DskipTests clean package'
                }
            }
        }
    }
}