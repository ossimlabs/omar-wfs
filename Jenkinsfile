properties([
    parameters ([
        string(name: 'BUILD_NODE', defaultValue: 'POD_LABEL', description: 'The build node to run on'),
        booleanParam(name: 'CLEAN_WORKSPACE', defaultValue: true, description: 'Clean the workspace at the end of the run'),
        string(name: 'DOCKER_REGISTRY_DOWNLOAD_URL', defaultValue: 'nexus-docker-private-group.ossim.io', description: 'Repository of docker images')
    ]),
    pipelineTriggers([
            [$class: "GitHubPushTrigger"]
    ]),
    [$class: 'GithubProjectProperty', displayName: '', projectUrlStr: 'https://github.com/ossimlabs/omar-wfs'],
    buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '3', daysToKeepStr: '', numToKeepStr: '20')),
    disableConcurrentBuilds()
])
podTemplate(
  containers: [
    containerTemplate(
      name: 'docker',
      image: 'docker:19.03.11',
      ttyEnabled: true,
      command: 'cat',
      privileged: true
    ),
    containerTemplate(
      image: "${DOCKER_REGISTRY_DOWNLOAD_URL}/omar-builder:1.0.0",
      name: 'builder',
      command: 'cat',
      ttyEnabled: true
    ),
    containerTemplate(
      image: "${DOCKER_REGISTRY_DOWNLOAD_URL}/alpine/helm:3.2.3",
      name: 'helm',
      command: 'cat',
      ttyEnabled: true
    ),
  ],
  volumes: [
    hostPathVolume(
      hostPath: '/var/run/docker.sock',
      mountPath: '/var/run/docker.sock'
    ),
  ]
)
{
  node(POD_LABEL){

      stage("Checkout branch $BRANCH_NAME")
      {
          checkout(scm)
      }
      stage("Load Variables")
      {
        withCredentials([string(credentialsId: 'o2-artifact-project', variable: 'o2ArtifactProject')]) {
          step ([$class: "CopyArtifact",
            projectName: o2ArtifactProject,
            filter: "common-variables.groovy",
            flatten: true])
          }
          load "common-variables.groovy"
      }
      stage('Build') {
        container('builder') {
          sh """
          ./gradlew assemble \
              -PossimMavenProxy=${MAVEN_DOWNLOAD_URL}
          ./gradlew copyJarToDockerDir \
              -PossimMavenProxy=${MAVEN_DOWNLOAD_URL}
          """
          archiveArtifacts "plugins/*/build/libs/*.jar"
          archiveArtifacts "apps/*/build/libs/*.jar"
        }
      }
    stage ("Publish Nexus"){	
      container('builder'){
          withCredentials([[$class: 'UsernamePasswordMultiBinding',
                          credentialsId: 'nexusCredentials',
                          usernameVariable: 'MAVEN_REPO_USERNAME',
                          passwordVariable: 'MAVEN_REPO_PASSWORD']])
          {
        load "common-variables.groovy"
    }
    stage('Docker build') {
      container('docker') {
        withDockerRegistry(credentialsId: 'dockerCredentials', url: "https://${DOCKER_REGISTRY_DOWNLOAD_URL}") {  //TODO
          sh """
            docker build --network=host -t "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-wfs-app:${BRANCH_NAME} ./docker
          """
    }}}

    stage ("Assemble") {
        sh """
        ./gradlew assemble \
            -PossimMavenProxy=${MAVEN_DOWNLOAD_URL}
        """
        archiveArtifacts "plugins/*/build/libs/*.jar"
        archiveArtifacts "apps/*/build/libs/*.jar"
    }
      stage('Docker push'){
        container('docker') {
          withDockerRegistry(credentialsId: 'dockerCredentials', url: "https://${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}") {
          sh """
              docker push "${DOCKER_REGISTRY_PUBLIC_UPLOAD_URL}"/omar-wfs-app:${BRANCH_NAME}
          """
          }

    stage ("Generate Swagger Spec") {
            sh """
            ./gradlew :omar-wfs-plugin:generateSwaggerDocs \
                -PossimMavenProxy=${MAVEN_DOWNLOAD_URL}
            """
            archiveArtifacts "plugins/*/build/swaggerSpec.json"
        }
      }
      stage('Package chart'){
        container('helm') {
          sh """
              mkdir packaged-chart
              helm package -d packaged-chart chart
              """

    stage ("Run Cypress Test") {
            sh """
            docker run -v $PWD:/e2e -w /e2e cypress/included:3.2.0 > testData.txt\
                -PossimMavenProxy=${MAVEN_DOWNLOAD_URL}
            """
            archiveArtifacts "testData.txt"
    }

    stage ("Publish Nexus")
    {
        withCredentials([[$class: 'UsernamePasswordMultiBinding',
                        credentialsId: 'nexusCredentials',
                        usernameVariable: 'MAVEN_REPO_USERNAME',
                        passwordVariable: 'MAVEN_REPO_PASSWORD']])
        {
            sh """
            ./gradlew publish \
                -PossimMavenProxy=${MAVEN_DOWNLOAD_URL}
            """
    }
    try {
    stage('SonarQube analysis') {
        withSonarQubeEnv(credentialsId: '3a6154edb38172a82ad75d6529fd0e7b706a0179', installationName: 'SonarQubeOssim') {
            sh 'mvn org.sonarsource.scanner.maven:sonar-maven-plugin:3.6.0.1398:sonar'
        }
    }
   }
   catch(Throwable e) {
   //Ignoring errors, SonarQube stage is optional.
   e.printStackTrace()
   }


    try {
        stage ("OpenShift Tag Image")
        {
            withCredentials([[$class: 'UsernamePasswordMultiBinding',
                            credentialsId: 'openshiftCredentials',
                            usernameVariable: 'OPENSHIFT_USERNAME',
                            passwordVariable: 'OPENSHIFT_PASSWORD']])
            {
                // Run all tasks on the app. This includes pushing to OpenShift and S3.
                sh """
                    ./gradlew openshiftTagImage \
                        -PossimMavenProxy=${MAVEN_DOWNLOAD_URL}

                """
            }
        }
      }
      stage('Upload chart'){
        container('builder') {
          withCredentials([usernameColonPassword(credentialsId: 'helmCredentials', variable: 'HELM_CREDENTIALS')]) {
            sh "curl -u ${HELM_CREDENTIALS} ${HELM_UPLOAD_URL} --upload-file packaged-chart/*.tgz -v"
          }
        }
      }
    }
    stage("Clean Workspace"){
      if ("${CLEAN_WORKSPACE}" == "true")
        step([$class: 'WsCleanup'])
    }
  }
}