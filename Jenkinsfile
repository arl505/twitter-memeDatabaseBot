pipeline {
  agent any

  stages {
    stage('Build and Test') {
      steps {
        sh('./gradlew clean build')
      }
    }
    stage('Deploy') {
      when {
        expression {
           env.BRANCH_NAME == 'master' || true
        }
      }
      steps {
        sh('cp ./build/libs/memedatabasebot-0.0.1-SNAPSHOT.jar /writeToFolder')
        sh('systemctl restart memebot')
      }
    }
  }
}
