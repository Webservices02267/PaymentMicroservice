pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                sh "chmod +x -R ${env.WORKSPACE}"
                sh './update.sh'
                sh './completeBuild.sh'
            }
        }
    }
    post {
        always {
            sh 'echo "pipeline complete"'
        }
    }
}
