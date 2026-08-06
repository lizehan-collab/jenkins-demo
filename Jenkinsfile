pipeline {
    agent any

    environment {
        HARBOR_URL = '192.168.0.175:8091'
        HARBOR_PROJECT = 'jenkins-project'
        APP_NAME = 'springboot-demo'
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        HARBOR_CREDENTIALS_ID = 'harbor-cred'
        KUBE_CONFIG_CREDENTIALS_ID = 'kubeconfig'
        DEPLOY_FILE = 'k8s/deployment.yaml'
    }

    stages {
        stage('拉取代码') {
            steps {
                git branch: 'main', url: 'https://v4.gh-proxy.org/https://github.com/lizehan-collab/jenkins-demo.git'
            }
        }

        stage('Maven 编译打包') {
            steps {
                 bat 'mvn clean package -s D:\\tools\\maven\\settings\\settings-c.xml -DskipTests'
            }
        }

       stage('构建并推送 Docker 镜像到 Harbor') {
           steps {
               // 切换到 default 上下文
               bat 'docker context use default'

               withDockerRegistry([credentialsId: "${HARBOR_CREDENTIALS_ID}", url: "https://${HARBOR_URL}"]) {
                   bat "docker build -t ${HARBOR_URL}/${HARBOR_PROJECT}/${APP_NAME}:${IMAGE_TAG} ."
                   bat "docker push ${HARBOR_URL}/${HARBOR_PROJECT}/${APP_NAME}:${IMAGE_TAG}"
               }
           }
       }

        stage('部署到 Kubernetes') {
            steps {
                // ⚠️ 警告：kubeconfig 明文内联，含 ServiceAccount token，拥有集群访问权限！
                // 切勿 push 到 GitHub 等公开仓库，否则等于泄露集群凭据！仅限本地测试使用。
                writeFile file: 'kubeconfig.yaml', text: '''apiVersion: v1
kind: Config
clusters:
- cluster:
    insecure-skip-tls-verify: true
    server: https://192.168.0.175:36231
  name: my-cluster
contexts:
- context:
    cluster: my-cluster
    user: my-user
  name: my-cluster
current-context: my-cluster
users:
- name: my-user
  user:
    token: eyJhbGciOiJSUzI1NiIsImtpZCI6ImNxSVp5NzFUVE9sZU9qV2NmV1VKbVF1R1JpaHk0SEYtQVhrTTZyd2VUTWsifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJkZWZhdWx0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6Im15LXVzZXItdG9rZW4iLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoibXktdXNlciIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6IjgyNDhlNzA1LTFkZTgtNDM4NS05YjhkLWFhMmE5NzNhMzc1ZiIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDpkZWZhdWx0Om15LXVzZXIifQ.L1Y6uRGzTpp_KevqjYi9yMwK5mmmU1iWsB3eKt1oMfczEB6Yoi_IBCi6CMaWqUAK86AVZ23ppvo70Mvugba3KGkKmRsAvTD4OhEEUyMwK0apTe4wKu0AQGSUQIsQbLK8rHrrqnuMPATRJAUsH91uuGhl02B-T3OUD0CeJSG3gxlY68wbDqYrB1WcIyoWIhjPrr-xbJSiwjC_-OCRt6HqkufQsit0wNUyCtX9V3MFtUCwfMrf86td2B9PjPu-X05zgLhU65y4qNTCW2YrjhtFbSKC-sl6tyu0RXcRIT6O8r680l8ygK-50dgQ8YQER9cRZAZBxIx7r6PkurSJmYuXyg
'''
                withEnv(['KUBECONFIG=kubeconfig.yaml']) {
                    // 使用 PowerShell 替换镜像标签（Windows 兼容）
                    powershell """
                        (Get-Content ${DEPLOY_FILE}) -replace 'image: .*', 'image: ${HARBOR_URL}/${HARBOR_PROJECT}/${APP_NAME}:${IMAGE_TAG}' | Set-Content ${DEPLOY_FILE}
                    """
                    bat "kubectl apply -f ${DEPLOY_FILE}"
                    bat "timeout /t 5"
                    bat "kubectl rollout status deployment/jenkins-demo"
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