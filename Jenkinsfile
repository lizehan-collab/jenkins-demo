pipeline {
    agent any

    environment {
        HARBOR_URL = '192.168.101.133:8091'
        HARBOR_PROJECT = 'jenkins-project'
        APP_NAME = 'springboot-demo'
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        HARBOR_CREDENTIALS_ID = 'harbor-cred'
        KUBE_CONFIG_CREDENTIALS_ID = 'kubeconfig'
        DEPLOY_FILE = 'k8s-deployment.yaml'
    }

    stages {
        stage('拉取代码') {
            steps {
                git branch: 'main', url: 'https://github.com/lizehan-collab/jenkins-demo.git'
            }
        }

        stage('Maven 编译打包') {
            steps {
                bat 'mvn clean package -DskipTests'
            }
        }

        stage('构建并推送 Docker 镜像到 Harbor') {
            steps {
                withDockerRegistry([credentialsId: "${HARBOR_CREDENTIALS_ID}", url: "http://${HARBOR_URL}"]) {
                    bat "docker build -t ${HARBOR_URL}/${HARBOR_PROJECT}/${APP_NAME}:${IMAGE_TAG} ."
                    bat "docker push ${HARBOR_URL}/${HARBOR_PROJECT}/${APP_NAME}:${IMAGE_TAG}"
                }
            }
        }

        stage('部署到 Kubernetes') {
            steps {
                withKubeConfig([credentialsId: "${KUBE_CONFIG_CREDENTIALS_ID}"]) {
                    // 使用 PowerShell 替换镜像标签（Windows 兼容）
                    powershell """
                        (Get-Content ${DEPLOY_FILE}) -replace 'image: .*', 'image: ${HARBOR_URL}/${HARBOR_PROJECT}/${APP_NAME}:${IMAGE_TAG}' | Set-Content ${DEPLOY_FILE}
                    """
                    bat "kubectl apply -f ${DEPLOY_FILE}"
                    bat "kubectl rollout status deployment/springboot-app"
                }
            }
        }
    }

    post {
        always {
            echo 'Pipeline finished.'
        }
    }
}