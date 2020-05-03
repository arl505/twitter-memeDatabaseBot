pipeline {
  agent any
  
  stages {
    stage('Build and Test') {
      steps {
        sh('./gradlew clean build')
      }
    stage('Deploy') {
      steps {
        sh(java -jar build/libs/memedatabasebot-0.0.1-SNAPSHOT.jar')
      }
   }
  }
}
