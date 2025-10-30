pipeline {
  agent {
    kubernetes {
      inheritFrom 'maven'
      defaultContainer 'maven'
    }
  }

  options { timestamps(); timeout(time: 20, unit: 'MINUTES') }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Build & Test') {
      steps {
        container('maven') {
          sh 'mvn -B -ntp clean package'
        }
      }
    }

    stage('Archive') {
      steps {
        archiveArtifacts artifacts: 'target/*.jar, target/*.war', fingerprint: true
        junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
      }
    }
  }
}
