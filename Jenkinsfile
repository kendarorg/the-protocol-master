pipeline {
    agent { label 'jdk11'}
   tools {
           maven 'maven399'
    }
    stages {
        stage('Build') {
            steps {
                withMaven {
                    //sh 'mvn --projects protocol-mysql --also-make -B install -DdockerNetworkName=jenkins_jenkins'
                    sh 'mvn install -DdockerNetworkName=jenkins_jenkins'
                }
            }
        }
    }
}