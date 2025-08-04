def call(String projectName, String imageTag, String dockerHubUser) {
  withCredentials([usernamePassword(credentialsId: 'dockerHub', passwordVariable: 'dockerHubPass', usernameVariable: 'dockerHubUser')]) {
    // Login to DockerHub
    sh "docker login -u ${dockerHubUser} -p ${dockerHubPass}"
    
    // Tag the Docker image
    sh "docker image tag ${dockerHubUser}/${projectName}:${imageTag} ${dockerHubUser}/${projectName}:${imageTag}"
    
    // Push the Docker image
    sh "docker push ${dockerHubUser}/${projectName}:${imageTag}"
    
   
  }
}
