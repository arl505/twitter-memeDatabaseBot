pipeline {
  agent any

  stages {
    stage('Build') {
      steps {
        sh('./gradlew clean build -x test')
      }
    }
    stage('Deploy') {
      when {
        expression {
           env.BRANCH_NAME == 'main'
        }
      }
      steps {
        sh('cp ./build/libs/memedatabasebot-0.0.1-SNAPSHOT.jar /home/sftp-able/backends/memebot/')
        sh('systemctl restart memebot')
      }
    }
  }
}
