pipeline {
    agent { label 'dockeragent'}
   tools {
           maven 'maven399'
           jdk 'jdk11'
    }
    stages {
        stage('Build') {
            steps {
                withMaven {
                    //sh 'mvn --projects protocol-mysql --also-make -B install -DnotRunWithJenkins=true -DdockerNetworkName=jenkins_jenkins'
                    sh 'mvn --projects protocol-postgres --also-make -B install -DskipTests'
                    //sh 'mvn install -DnotRunWithJenkins=true -DdockerNetworkName=jenkins_jenkins'
                }
                 withMaven {
                    sh 'mvn --projects protocol-postgres -B test -DdockerNetworkName=jenkins_jenkins'
                }
            }
        }
    }
}