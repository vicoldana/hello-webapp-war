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
    APP_NAME = "hello-webapp"
    SERVICE_PORT = "8085"
    CONTAINER_PORT = "8080"
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

    stage('Deploy to Kubernetes (Tomcat Deployment + Service)') {
      steps {
        echo '🚀 Deploy în Kubernetes...'
        sh '''
          set -e

          WAR_FILE=$(ls target/*.war | head -n 1)
          echo "📄 WAR detectat: $WAR_FILE"

          # Instalăm kubectl
          echo "📦 Instalăm kubectl..."
          curl -LO "https://dl.k8s.io/release/v1.29.0/bin/linux/amd64/kubectl"
          chmod +x kubectl && mv kubectl /tmp/kubectl

          # Creăm manifestul complet (Deployment + Service)
          cat > deploy.yaml <<YAML
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${APP_NAME}
  labels:
    app: ${APP_NAME}
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ${APP_NAME}
  template:
    metadata:
      labels:
        app: ${APP_NAME}
    spec:
      containers:
        - name: tomcat
          image: tomcat:10.1-jdk17
          ports:
            - containerPort: ${CONTAINER_PORT}
          volumeMounts:
            - name: webapps
              mountPath: /usr/local/tomcat/webapps
      volumes:
        - name: webapps
          emptyDir: {}
---
apiVersion: v1
kind: Service
metadata:
  name: ${APP_NAME}-svc
  labels:
    app: ${APP_NAME}
spec:
  type: NodePort
  selector:
    app: ${APP_NAME}
  ports:
    - port: ${CONTAINER_PORT}
      targetPort: ${CONTAINER_PORT}
      nodePort: ${SERVICE_PORT}
YAML

          echo "📤 Aplicăm manifestul..."
          /tmp/kubectl -n "${K8S_NAMESPACE}" delete deployment ${APP_NAME} --ignore-not-found=true
          /tmp/kubectl -n "${K8S_NAMESPACE}" delete svc ${APP_NAME}-svc --ignore-not-found=true
          /tmp/kubectl -n "${K8S_NAMESPACE}" apply -f deploy.yaml

          echo "⏳ Așteptăm ca podul să fie Ready..."
          /tmp/kubectl -n "${K8S_NAMESPACE}" rollout status deployment/${APP_NAME} --timeout=120s || true
          POD=$(/tmp/kubectl -n "${K8S_NAMESPACE}" get pods -l app=${APP_NAME} -o jsonpath="{.items[0].metadata.name}")

          echo "📥 Copiem fișierul WAR în Tomcat..."
          /tmp/kubectl -n "${K8S_NAMESPACE}" cp "$WAR_FILE" $POD:/usr/local/tomcat/webapps/ROOT.war

          echo "✅ Deploy complet! Tomcat va încărca aplicația automat."
        '''
      }
    }
  }

  post {
    success {
      echo '✅ Build + Deploy reușit! Aplicația rulează în Tomcat.'
      echo 'ℹ️ Jenkins rulează în namespace-ul ${K8S_NAMESPACE}.'
      echo '🌐 Aplicația este expusă pe portul ${SERVICE_PORT}.'
      echo '👉 Deschide în browser: http://127.0.0.1:${SERVICE_PORT}'
    }
    failure {
      echo '❌ Build sau Deploy eșuat. Verifică logurile Jenkins.'
    }
  }
}
