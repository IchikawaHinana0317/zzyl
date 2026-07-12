pipeline {
    agent any

    options {
        // 控制台日志显示时间
        timestamps()

        // 防止两次部署同时修改同一个容器
        disableConcurrentBuilds()

        /*
         * Pipeline script from SCM 在执行流水线前，
         * 会先拉一次代码来读取 Jenkinsfile。
         *
         * 这里禁止后续自动 checkout，
         * 由“拉取Git代码”阶段统一完成业务代码检出。
         */
        skipDefaultCheckout(true)
    }

    tools {
        /*
         * 必须与：
         * Manage Jenkins -> Tools
         * 中配置的名称完全一致
         */
        maven 'maven'
        jdk 'jdk11'
    }

    environment {
        IMAGE_NAME = 'zzyl-admin'
        CONTAINER_NAME = 'zzyl-admin'

        // Windows本机端口
        HOST_PORT = '9000'

        // Spring Boot容器内部端口
        CONTAINER_PORT = '9000'
    }

    stages {

        stage('清理工作空间') {
            steps {
                cleanWs()
            }
        }

        stage('拉取Git代码') {
            steps {
                echo "仓库地址：${params.GIT_URL}"
                echo "构建分支：${params.GIT_BRANCH}"
                echo "镜像版本：${params.DOCKER_TAG}"

                checkout([
                    $class: 'GitSCM',

                    branches: [[
                        name: params.GIT_BRANCH
                    ]],

                    doGenerateSubmoduleConfigurations: false,

                    extensions: [
                        /*
                         * 避免工作空间中出现过深目录；
                         * 也确保每次检出更干净。
                         */
                        [$class: 'CleanBeforeCheckout']
                    ],

                    submoduleCfg: [],

                    userRemoteConfigs: [[
                        /*
                         * 必须与你在 Jenkins Credentials
                         * 中填写的 ID 完全一致。
                         */
                        credentialsId: 'github-token',
                        url: params.GIT_URL
                    ]]
                ])

                bat(
                    encoding: 'UTF-8',
                    script: '''
                        git branch -a
                        git log -1 --oneline
                        dir
                    '''
                )
            }
        }

        stage('检查构建环境') {
            steps {
                bat(
                    encoding: 'UTF-8',
                    script: '''
                        echo 当前Jenkins身份：
                        whoami

                        echo Git版本：
                        git --version

                        echo Java版本：
                        java -version

                        echo Maven版本：
                        mvn -version

                        echo Docker版本：
                        docker version
                    '''
                )
            }
        }

        stage('Maven打包') {
            steps {
                echo '开始执行 Maven 多模块打包...'

                bat(
                    encoding: 'UTF-8',
                    script: '''
                        mvn clean install -DskipTests
                    '''
                )
            }
        }

        stage('准备Docker构建产物') {
            steps {
                /*
                 * 不假设最终jar一定叫zzyl-admin.jar。
                 *
                 * 从target中寻找：
                 * 1. 非original开头
                 * 2. 非sources、javadoc
                 * 3. 最大的jar
                 *
                 * 然后统一复制成target/app.jar，
                 * Dockerfile只复制这个固定文件。
                 */
                powershell(
                    encoding: 'UTF-8',
                    script: '''
                        $ErrorActionPreference = "Stop"

                        $TargetDirectory = Join-Path `
                            $env:WORKSPACE `
                            "zzyl-admin\\target"

                        if (-not (Test-Path $TargetDirectory)) {
                            throw "不存在target目录：$TargetDirectory"
                        }

                        $Jar = Get-ChildItem `
                            -Path $TargetDirectory `
                            -Filter "*.jar" |
                            Where-Object {
                                $_.Name -notlike "original-*" -and
                                $_.Name -notlike "*-sources.jar" -and
                                $_.Name -notlike "*-javadoc.jar" -and
                                $_.Name -ne "app.jar"
                            } |
                            Sort-Object Length -Descending |
                            Select-Object -First 1

                        if ($null -eq $Jar) {
                            throw "zzyl-admin/target中没有找到可执行jar"
                        }

                        $AppJar = Join-Path `
                            $TargetDirectory `
                            "app.jar"

                        Copy-Item `
                            -Path $Jar.FullName `
                            -Destination $AppJar `
                            -Force

                        Write-Host "原始Jar：$($Jar.FullName)"
                        Write-Host "Docker Jar：$AppJar"
                        Write-Host "Jar大小：$($Jar.Length) 字节"
                    '''
                )

                bat(
                    encoding: 'UTF-8',
                    script: '''
                        dir zzyl-admin\\target
                        if not exist zzyl-admin\\target\\app.jar (
                            echo 未找到 app.jar
                            exit /b 1
                        )
                    '''
                )
            }
        }

        stage('构建Docker镜像') {
            steps {
                echo "构建镜像：${env.IMAGE_NAME}:${params.DOCKER_TAG}"

                bat(
                    encoding: 'UTF-8',
                    script: """
                        docker build ^
                          --pull ^
                          --tag ${env.IMAGE_NAME}:${params.DOCKER_TAG} ^
                          --file zzyl-admin\\Dockerfile ^
                          zzyl-admin
                    """
                )
            }
        }

        stage('部署服务') {
            steps {
                echo "部署镜像：${env.IMAGE_NAME}:${params.DOCKER_TAG}"

                powershell(
                    encoding: 'UTF-8',
                    script: """
                        & ".\\zzyl-admin\\deploy.ps1" `
                          -ImageName "${env.IMAGE_NAME}" `
                          -ImageTag "${params.DOCKER_TAG}" `
                          -ContainerName "${env.CONTAINER_NAME}" `
                          -HostPort ${env.HOST_PORT} `
                          -ContainerPort ${env.CONTAINER_PORT}
                    """
                )
            }
        }

        stage('查看镜像和容器') {
            steps {
                bat(
                    encoding: 'UTF-8',
                    script: """
                        echo Docker镜像：
                        docker images ${env.IMAGE_NAME}

                        echo Docker容器：
                        docker ps -a --filter "name=${env.CONTAINER_NAME}"

                        echo 容器最近50行日志：
                        docker logs --tail 50 ${env.CONTAINER_NAME}
                    """
                )
            }
        }
    }

    post {

        success {
            echo '========================================'
            echo '流水线执行成功'
            echo "镜像：${env.IMAGE_NAME}:${params.DOCKER_TAG}"
            echo "容器：${env.CONTAINER_NAME}"
            echo "后端地址：http://localhost:${env.HOST_PORT}"
            echo '========================================'
        }

        failure {
            echo '流水线执行失败，请检查失败阶段和控制台日志'

            script {
                bat(
                    returnStatus: true,
                    encoding: 'UTF-8',
                    script: """
                        docker ps -a --filter "name=${env.CONTAINER_NAME}"
                        docker logs --tail 200 ${env.CONTAINER_NAME}
                    """
                )
            }
        }

        always {
            echo '任务构建完毕'
        }
    }
}