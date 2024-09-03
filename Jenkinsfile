pipeline {
    agent { label 'jdk11'}
    tools {
            maven 'maven399'
            jdk 'jdk11'
        }
    stages {
        stage('Build') {
            steps {
                withMaven {
                    sh 'mvn -B install'
                }
            }
        }
    }
}