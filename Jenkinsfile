pipeline {
  agent any

  tools {
    jdk 'jdk17'
    maven 'Maven_3.9.9'
  }

  options {
    ansiColor('xterm')
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }

  environment {
    K8S_NAMESPACE = "jenkins"
  }

  stages {

    stage('Checkout') {
      steps {
        echo '📦 Descărcăm codul sursă...'
        checkout scm
      }
    }

    stage('Build WAR') {
      steps {
        echo '🏗️ Construim aplicația WAR...'
        sh '''
          if [ -x ./mvnw ]; then
            ./mvnw -B -e -Dmaven.javadoc.skip=true clean package
          else
            mvn -B -e -Dmaven.javadoc.skip=true clean package
          fi
        '''
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
        }
      }
    }

    stage('Archive Artifact') {
      steps {
        echo '💾 Salvăm fișierul WAR...'
        archiveArtifacts artifacts: 'target/*.war', fingerprint: true
      }
    }

    stage('Deploy to Kubernetes (Tomcat)') {
      steps {
        echo '🚀 Deploy în Kubernetes (Tomcat)...'
        sh '''
          set -e

          # 1️⃣ Detectăm fișierul WAR
          WAR_FILE=$(ls target/*.war | head -n 1)
          echo "📄 WAR detectat: $WAR_FILE"

          # 2️⃣ Instalăm kubectl (local în /tmp)
          echo "📦 Instalăm kubectl..."
          curl -LO "https://dl.k8s.io/release/v1.29.0/bin/linux/amd64/kubectl"
          chmod +x kubectl && mv kubectl /tmp/kubectl

          # 3️⃣ Creăm manifestul YAML pentru Tomcat
          cat > deploy.yaml <<'YAML'
apiVersion: v1
kind: Pod
metadata:
  name: hello-webapp
  labels:
    app: hello-webapp
spec:
  containers:
    - name: tomcat
      image: tomcat:10.1-jdk17
      ports:
        - containerPort: 8080
      volumeMounts:
        - name: webapps
          mountPath: /usr/local/tomcat/webapps
  volumes:
    - name: webapps
      emptyDir: {}
YAML

          # 4️⃣ Deploy Tomcat
          echo "📤 Deploy Tomcat..."
          /tmp/kubectl -n "${K8S_NAMESPACE}" delete pod hello-webapp --ignore-not-found=true
          /tmp/kubectl -n "${K8S_NAMESPACE}" apply -f deploy.yaml

          # 5️⃣ Așteptăm pornirea completă a Tomcat
          echo "⏳ Așteptăm ca Tomcat să fie READY..."
          /tmp/kubectl -n "${K8S_NAMESPACE}" wait --for=condition=Ready pod/hello-webapp --timeout=120s || true
          sleep 5
          /tmp/kubectl -n "${K8S_NAMESPACE}" get pod hello-webapp -o wide || true

          # 6️⃣ Copiem WAR-ul în Tomcat
          echo "📥 Copiem aplicația WAR în container..."
          /tmp/kubectl -n "${K8S_NAMESPACE}" cp "$WAR_FILE" hello-webapp:/usr/local/tomcat/webapps/ROOT.war

          echo "✅ Deploy complet! Tomcat va încărca aplicația automat."
        '''
      }
    }
  }

  post {
    success {
      echo '✅ Build + Deploy reușit! Aplicația rulează în Tomcat.'
      echo 'ℹ️ Jenkins rulează în namespace-ul ${K8S_NAMESPACE}.'
      echo '👉 Pentru acces local:'
      echo '   kubectl -n ${K8S_NAMESPACE} port-forward pod/hello-webapp 8085:8080'
      echo '🔗 Apoi deschide în browser: http://localhost:8085'
    }
    failure {
      echo '❌ Build sau Deploy eșuat. Verifică logurile Jenkins.'
    }
  }
}
