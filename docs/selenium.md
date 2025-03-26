

```

pipeline {
    agent { label 'dockeragent'}
   tools {
           maven 'maven399'
           jdk 'jdk17'
    }
	environment {
		CHROME_BIN='/usr/bin/google-chrome-stable'
	}
    stages {
        stage('Build') {
            steps {
                 sh '''apt-get update  > /dev/null
                    apt-get install -y --no-install-recommends fonts-ipafont-gothic \
						fonts-kacst fonts-freefont-ttf dbus dbus-x11 apt-transport-https \
						ca-certificates curl gnupg2 software-properties-common wget  > /dev/null
					wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add -
					echo "deb http://dl.google.com/linux/chrome/deb/ stable main" > \
						/etc/apt/sources.list.d/google.list
					apt-get update	
					apt-get install -y google-chrome-stable xvfb
					'''
			     git branch: 'ui-improvment2',
                    url:'https://github.com/kendarorg/the-protocol-master.git'
                 withMaven {
                    sh 'mvn install -DtestFailureIgnore=true -Dmaven.test.failure.ignore=true'
                    //sh 'mvn install -pl jacoco -am -DtestFailureIgnore=true -Dmaven.test.failure.ignore=true'
                    //sh 'mvn test -Dtest=UiTest -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false' 
                    //sh 'mvn install -Dmaven.test.skip -DskipTests'
                    //sh 'mvn test -Dtest=UiSeleniumTest -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false'
                }
            }
        }
    }
    post {
        always {
            // archiveArtifacts artifacts: "**/protocol-runner.jar, **/coverage-report-html.zip"
            sh '''
                current_dir=$(pwd)
                echo "The script is running from: $current_dir"
            '''
            junit '**/target/surefire-reports/*.xml'
        }  
    }  
}


```