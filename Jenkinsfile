pipeline {
    agent any
    tools {
            maven 'maven399'
            jdk 'javajdk'
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