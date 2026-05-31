pipeline {
  agent any

  environment {
    REPO_URL = 'https://github.com/esneider1504/microservicios-calendario.git'
    BRANCH   = 'main'
    IMG_MS1  = 'apifestivos:1.0'
    IMG_MS2  = 'apicalendario:1.0'
    RED      = 'redcalendario'
  }

  stages {

    stage('Clonar Repositorio') {
      steps {
        git branch: "${BRANCH}", url: "${REPO_URL}"
      }
    }

    stage('Construir Imágenes Docker') {
      steps {
        sh 'docker build -t ${IMG_MS1} ./validate_holydays_ms'
        sh 'docker build -t ${IMG_MS2} ./consume_holidays_ms'
      }
    }

    stage('Detener Contenedores Anteriores') {
      steps {
        sh '''
          for c in dockerapicalendario dockerapifestivos dockerbdcalendario dockerbdfestivos; do
            docker stop $c 2>/dev/null || true
            docker rm   $c 2>/dev/null || true
          done
        '''
      }
    }

    stage('Preparar Red y Volúmenes') {
      steps {
        sh '''
          docker network inspect ${RED} >/dev/null 2>&1 || docker network create ${RED}
          docker volume create mongodb_data  2>/dev/null || true
          docker volume create postgres_data 2>/dev/null || true
        '''
      }
    }

    stage('Desplegar Bases de Datos') {
      steps {
        sh '''
          docker run -d --name dockerbdfestivos --network ${RED} \
            -e MONGO_INITDB_DATABASE=festivos_db \
            -e MONGO_INITDB_ROOT_USERNAME=admin \
            -e MONGO_INITDB_ROOT_PASSWORD=password123 \
            -v mongodb_data:/data/db \
            mongo:7

          docker run -d --name dockerbdcalendario --network ${RED} \
            -e POSTGRES_DB=calendario_db \
            -e POSTGRES_USER=postgres \
            -e POSTGRES_PASSWORD=postgres \
            -v postgres_data:/var/lib/postgresql/data \
            postgres:16

          echo "Esperando inicialización de BDs (15s)..."
          sleep 15
        '''
      }
    }

    stage('Desplegar APIs') {
      steps {
        sh '''
          docker run -d --name dockerapifestivos --network ${RED} \
            -p 8080:8080 \
            -e PORT=8080 \
            -e MONGODB_URI="mongodb://admin:password123@dockerbdfestivos:27017/festivos_db?authSource=admin" \
            ${IMG_MS1}

          echo "Esperando API Festivos (8s)..."
          sleep 8

          docker run -d --name dockerapicalendario --network ${RED} \
            -p 8082:8082 \
            -e DB_URL="jdbc:postgresql://dockerbdcalendario:5432/calendario_db" \
            -e DB_USER=postgres \
            -e DB_PASSWORD=postgres \
            -e MS1_BASE_URL="http://dockerapifestivos:8080" \
            ${IMG_MS2}
        '''
      }
    }

  }

  post {
    success {
      echo 'Despliegue exitoso. Red redcalendario activa con 4 contenedores.'
    }
    failure {
      echo 'Despliegue fallido. Revisar Console Output en Jenkins (http://localhost:8090).'
    }
  }
}
