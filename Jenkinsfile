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
                    sh 'mvn --projects protocol-amqp-091 --also-make -B install -DdockerNetworkName=jenkins_jenkins'
                    //sh 'mvn --projects protocol-postgres --also-make -B install -DskipTests'
                    //sh 'mvn install -DskipTests'
                    //sh 'mvn --projects protocol-postgres -B test -DdockerNetworkName=jenkins_jenkins'
                    //sh 'mvn test -DdockerNetworkName=jenkins_jenkins'
                }
            }
        }
    }
}