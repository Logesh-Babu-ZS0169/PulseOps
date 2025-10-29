pipeline {
    agent any
    
    environment {
        DOCKER_REGISTRY = 'docker.io/zsubscription'
        DOCKER_CREDENTIALS_ID = 'docker-registry-credentials'
        BACKEND_IMAGE = "${DOCKER_REGISTRY}/ingestionmetrics-backend"
        FRONTEND_IMAGE = "${DOCKER_REGISTRY}/ingestionmetrics-frontend"
        GIT_CREDENTIALS_ID = 'git-credentials'
        KUBECONFIG_CREDENTIALS_ID = 'kubeconfig-credentials'
        VERSION = ${env.VERSION}
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = sh(
                        returnStdout: true,
                        script: 'git rev-parse --short HEAD'
                    ).trim()
                    env.IMAGE_TAG = "${env.VERSION}-${env.GIT_COMMIT_SHORT}"
                }
            }
        }
        
        stage('Build Backend') {
            steps {
                dir('backend') {
                    sh '''
                        echo "Building Backend Application..."
                        mvn clean package -DskipTests
                    '''
                }
            }
        }
        
        stage('Build Docker Images') {
            parallel {
                stage('Build Backend Image') {
                    steps {
                        script {
                            docker.withRegistry("https://${DOCKER_REGISTRY}", "${DOCKER_CREDENTIALS_ID}") {
                                def backendImage = docker.build(
                                    "${BACKEND_IMAGE}:${IMAGE_TAG}",
                                    "./backend"
                                )
                                backendImage.push()
                                backendImage.push('latest')
                            }
                        }
                    }
                }
                
                stage('Build Frontend Image') {
                    steps {
                        script {
                            docker.withRegistry("https://${DOCKER_REGISTRY}", "${DOCKER_CREDENTIALS_ID}") {
                                def frontendImage = docker.build(
                                    "${FRONTEND_IMAGE}:${IMAGE_TAG}",
                                    "./frontend"
                                )
                                frontendImage.push()
                                frontendImage.push('latest')
                            }
                        }
                    }
                }
            }
        }
        
        stage('Update Kubernetes Manifests') {
            steps {
                script {
                    sh """
                        sed -i 's|newTag:.*|newTag: ${IMAGE_TAG}|g' k8s/overlays/production/kustomization.yaml
                    """
                }
            }
        }
        
        stage('Commit and Push Manifests') {
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: "${GIT_CREDENTIALS_ID}",
                        usernameVariable: 'GIT_USER',
                        passwordVariable: 'GIT_PASS'
                    )]) {
                        sh """
                            git config user.email "jenkins@ci.com"
                            git config user.name "Jenkins CI"
                            git add k8s/overlays/production/kustomization.yaml
                            git commit -m "Update image tags to ${IMAGE_TAG}" || echo "No changes to commit"
                            git push https://${GIT_USER}:${GIT_PASS}@github.com/your-org/metrics-dashboard.git HEAD:main || echo "No changes to push"
                        """
                    }
                }
            }
        }
        
        stage('Deploy to Kubernetes') {
            steps {
                script {
                    withKubeConfig([credentialsId: "${KUBECONFIG_CREDENTIALS_ID}"]) {
                        sh '''
                            kubectl apply -k k8s/overlays/production
                            kubectl rollout status deployment/metrics-dashboard-backend -n metrics-dashboard --timeout=5m
                            kubectl rollout status deployment/metrics-dashboard-frontend -n metrics-dashboard --timeout=5m
                        '''
                    }
                }
            }
        }
        
        stage('Verify Deployment') {
            steps {
                script {
                    withKubeConfig([credentialsId: "${KUBECONFIG_CREDENTIALS_ID}"]) {
                        sh '''
                            echo "Checking Backend Pods..."
                            kubectl get pods -n metrics-dashboard -l component=backend
                            
                            echo "Checking Frontend Pods..."
                            kubectl get pods -n metrics-dashboard -l component=frontend
                            
                            echo "Checking Services..."
                            kubectl get svc -n metrics-dashboard
                            
                            echo "Checking Ingress..."
                            kubectl get ingress -n metrics-dashboard
                        '''
                    }
                }
            }
        }
    }
    
    post {
        success {
            echo "✅ Deployment successful! Version: ${IMAGE_TAG}"
        }
        failure {
            echo "❌ Deployment failed!"
        }
        always {
            cleanWs()
        }
    }
}
