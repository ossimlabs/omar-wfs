properties([
    parameters ([
        string(name: 'BUILD_NODE', defaultValue: 'omar-build', description: 'The build node to run on'),
        booleanParam(name: 'CLEAN_WORKSPACE', defaultValue: true, description: 'Clean the workspace at the end of the run')
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
                        name: 'cypress',
                        image: 'cypress/base:12.14.1',
                        ttyEnabled: true,
                        command: 'cat',
                        privileged: true
                )
        ],
        volumes: [
                hostPathVolume(
                        hostPath: '/var/run/docker.sock',
                        mountPath: '/var/run/docker.sock'
                ),
        ]
)
node("${BUILD_NODE}"){

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

    stage ("Assemble") {
        sh """
        ./gradlew assemble \
            -PossimMavenProxy=${MAVEN_DOWNLOAD_URL}
        """
        archiveArtifacts "plugins/*/build/libs/*.jar"
        archiveArtifacts "apps/*/build/libs/*.jar"
    }

    stage ("Generate Swagger Spec") {
            sh """
            ./gradlew :omar-wfs-plugin:generateSwaggerDocs \
                -PossimMavenProxy=${MAVEN_DOWNLOAD_URL}
            """
            archiveArtifacts "plugins/*/build/swaggerSpec.json"
        }

    stage ("Run Cypress Test") {
        container('cypress') {
            sh """
                        npx cypress run \
                        mochawesome-merge --reportDir mochawesome-report > mochawesome-bundle.json \
                        marge mochawesome-bundle.json -o mochawesome-report/html \
                            -PossimMavenProxy=${MAVEN_DOWNLOAD_URL}
                        """
                        archiveArtifacts "mochawesome-report/html/mochawesome-bundle.html"
        }
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
    }

    stage ("Publish Docker App")
    {
        withCredentials([[$class: 'UsernamePasswordMultiBinding',
                        credentialsId: 'dockerCredentials',
                        usernameVariable: 'DOCKER_REGISTRY_USERNAME',
                        passwordVariable: 'DOCKER_REGISTRY_PASSWORD']])
        {
            // Run all tasks on the app. This includes pushing to OpenShift and S3.
            sh """
            ./gradlew pushDockerImage \
                -PossimMavenProxy=${MAVEN_DOWNLOAD_URL}
            """
        }
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
    } catch (e) {
        echo e.toString()
    }

    stage("Clean Workspace")
    {
        if ("${CLEAN_WORKSPACE}" == "true")
            step([$class: 'WsCleanup'])
    }
}
