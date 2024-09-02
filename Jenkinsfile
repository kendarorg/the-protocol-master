pipeline {
    agent { label 'jdk11'}
    tools {
            maven 'maven399'
            jdk 'javajdk'
        }
    stages {
        stage('Build') {
            steps {
                withMaven {
                    sh 'mvn -B clean install'
                }
            }
        }
    }
}