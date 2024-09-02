pipeline {
    agent any
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