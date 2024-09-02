pipeline {
    agent { label 'docker'}
    tools {
            maven 'maven399'
            jdk 'jdk11'
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