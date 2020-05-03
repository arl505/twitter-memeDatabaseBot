pipeline {
  agent any

  stages {
    stage('Build and Test') {
      steps {
        sh('./gradlew clean build')
        echo env
        sh('printenv')
      }
    }
    if(env.BRANCH_NAME == 'master') {
      stage('Deploy') {
        steps {
          sh('ln -s -f ./build/libs/memedatabasebot-0.0.1-SNAPSHOT.jar /etc/init.d/memebot')
          sh('service memebot start')
        }
      }
    }
  }
}