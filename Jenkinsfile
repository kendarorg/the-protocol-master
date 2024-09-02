pipeline {
    agent { label 'jdk11'}
    options {
        skipDefaultCheckout true
      }
    tools {
            maven 'maven399'
            jdk 'javajdk'
        }
    stages {
        stage('clean_workspace_and_checkout_source') {
          steps {
            sh 'chmod 777 /var/run/docker.sock'
            checkout scm
          }
        }
        stage('Build') {
            steps {
                withMaven {
                    sh 'mvn -B clean install'
                }
            }
        }
    }
}