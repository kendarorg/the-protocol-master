pipeline {
    agent { label 'jdk11'}
   tools {
           maven 'maven399'
    }
    stages {
        stage('Build') {
            steps {
                withMaven {
                    //sh 'mvn --projects protocol-mysql --also-make -B install -DnotRunWithJenkins=true -DdockerNetworkName=jenkins_jenkins'
                    //sh 'mvn --projects protocol-mqtt --also-make -B install -DnotRunWithJenkins=true -DdockerNetworkName=jenkins_jenkins'
                    sh 'mvn install -DnotRunWithJenkins=true -DdockerNetworkName=jenkins_jenkins'
                }
            }
        }
    }
}