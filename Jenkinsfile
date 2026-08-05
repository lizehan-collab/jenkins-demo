pipeline {
    agent any

    parameters {
        string(name: 'BRANCH', defaultValue: 'main', description: '代码分支')
        string(name: 'ENV', defaultValue: 'test', description: '部署环境')
    }

    stages {
        stage('拉取代码') {
            steps {
                // 1. 从 Git 拉取指定分支的代码
                git branch: "${params.BRANCH}", url: 'https://你的Git仓库地址.git'
            }
        }

        stage('Maven 编译打包') {
            steps {
                // 2. 执行 Maven 打包（跳过测试，加快速度）
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('部署到服务器') {
            steps {
                // 3. 把生成的 jar 包传到目标服务器（假设是 Linux）
                // 注意：这里的用户名、密码需要配置在 Jenkins 的凭据中
                sh '''
                    scp target/*.jar root@你的服务器IP:/app/my-app.jar
                    ssh root@你的服务器IP "systemctl restart my-app"
                '''
                echo "部署完成！环境：${params.ENV}，分支：${params.BRANCH}"
            }
        }
    }
}