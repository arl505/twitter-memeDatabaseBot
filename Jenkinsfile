pipeline {
  agent any
  
  stages {
    stage('Build and Test') {
      steps {
        sh('./gradlew clean build')
      }
    }
    stage('Deploy') {
      steps {
      }
    }
  }
}
