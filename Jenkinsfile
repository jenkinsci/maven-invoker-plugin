    node('linux') {
      // System Dependent Locations
      def mvntool = tool name: 'maven3', type: 'hudson.tasks.Maven$MavenInstallation'
      def jdktool = tool name: "jdk8", type: 'hudson.model.JDK'
      
        stage('Compile') {
          
            timeout(time: 15, unit: 'MINUTES') {
              withMaven(
                      maven: 'maven3',
                      jdk: "jdk8",
                      mavenLocalRepo: "${env.JENKINS_HOME}/${env.EXECUTOR_NUMBER}") {
                sh "mvn -V -B clean install"
              }
          }
        }      
      
    }    
