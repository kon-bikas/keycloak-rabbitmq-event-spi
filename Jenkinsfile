pipeline {
    agent any

    environment {
        DOCKER_TOKEN = credentials('docker-push-secrer')
        DOCKER_USER = 'kon-bikas'
        DOCKER_SERVER = 'ghcr.io'
        DOCKER_PREFIX = 'ghcr.io/kon-bikas/keycloak-event-spi'
    }

    stages {
        stage('Creating new jar to test') {
            steps {
                sh '''
                    ./mvnw clean package -Dmaven.test.skip
                '''
            }
        }
        stage('Testing project') {
            options {
                timeout(time: 10, unit: 'MINUTES')
            }
            steps {
                sh '''
                    echo "Starting tests..."
                    ./mvnw test
                '''
            }
        }
        stage('Build and push docker image') {
            options {
                timeout(time: 30, unit: 'MINUTES')
            }
            steps {
                sh '''
                    docker build --rm -t $DOCKER_PREFIX:latest -f with-builder.Dockerfile . 
                '''

                sh '''
                    echo $DOCKER_TOKEN | docker login $DOCKER_SERVER -u $DOCKER_USER --password-stdin
                    docker push $DOCKER_PREFIX --all-tags 
                '''
            }
        }
    }

    post {
        always {
            echo 'Slack Notification!'
            slackSend (
                    channel: '#new-channel',
                    message: "*${currentBuild.currentResult}:* Job ${env.JOB_NAME} + \n" +
                            "build ${env.BUILD_NUMBER} \n" +
                            "more info at ${env.BUILD_URL}}"
            )
        }
    }
}