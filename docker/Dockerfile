#ARG BASE_IMAGE
#FROM ${BASE_IMAGE}
#FROM adoptopenjdk/openjdk8:alpine-slim
FROM nexus-docker-public-hosted.ossim.io/openjdk8:alpine-slim

ENV HOME /home/omar
RUN adduser -D -h $HOME -s /sbin/nologin -u 1001 omar
COPY run.sh $HOME
RUN chown 1001:0 -R $HOME && \
    chmod 777 $HOME && \
    chmod 777 $HOME/*.sh
USER 1001

ENV SPRING_CLOUD_CONFIG_ENABLED=true \
    SPRING_CLOUD_DISCOVERY_ENABLED=true
WORKDIR $HOME
EXPOSE 8080
COPY *.jar $HOME/
CMD ["/bin/sh", "/home/omar/run.sh"]

ENV NODE_VERSION=12.6.0
RUN apt install -y curl
RUN curl -o- https://raw.githubusercontent.com/creationix/nvm/v0.34.0/install.sh | bash
ENV NVM_DIR=/root/.nvm
RUN . "$NVM_DIR/nvm.sh" && nvm install ${NODE_VERSION}
RUN . "$NVM_DIR/nvm.sh" && nvm use v${NODE_VERSION}
RUN . "$NVM_DIR/nvm.sh" && nvm alias default v${NODE_VERSION}
ENV PATH="/root/.nvm/versions/node/v${NODE_VERSION}/bin/:${PATH}"

#################*NEED TO GET THIS WORKING*#####################
#RUN curl -sL https://rpm.nodesource.com/setup | bash -
#RUN yum install -y nodejs
RUN npm i -g npx
RUN npm install cypress --save-dev
RUN npm install --save-dev mocha cypress-multi-reporters mochawesome
RUN npm i --save-dev mochawesome-merge
RUN npm i --save-dev mochawesome-report-generator