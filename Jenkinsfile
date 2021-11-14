pipeline {
  agent any

  stages {
    stage('Build & Test') {
      steps {
        sh('./gradlew clean build')
      }
    }
    stage('Deploy') {
      when {
        expression {
           env.BRANCH_NAME == 'main'
        }
      }
      steps {
        sh('cp ./build/libs/memedatabasebot-0.0.1-SNAPSHOT.jar /home/projects/memebot/')
        sh('systemctl restart memebot')
      }
    }
  }
}
