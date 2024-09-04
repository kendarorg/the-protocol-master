pipeline {
    agent { label 'jdk11'}
   tools {
           maven 'maven399'
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