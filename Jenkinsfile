node('linux') {
  stage('scm'){
    checkout scm
  }
  stage('Compile') {
      timeout(time: 30, unit: 'MINUTES') {
        withMaven(
                maven: 'maven3',
                jdk: "jdk8",
                mavenLocalRepo: "${env.JENKINS_HOME}/${env.EXECUTOR_NUMBER}") {
          sh "mvn -V -B clean install"
        }
    }
  }

}
