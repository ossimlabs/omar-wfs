#ARG BASE_IMAGE
#FROM ${BASE_IMAGE}
#FROM adoptopenjdk/openjdk8:alpine-slim
FROM nexus-docker-private-hosted.ossim.io/openjdk8:alpine-slim

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

#################*NEED TO GET THIS WORKING*#####################
#RUN curl -sL https://rpm.nodesource.com/setup | bash -
#RUN yum install -y nodejs
#RUN npm i -g npx
#RUN npm install cypress --save-dev
#RUN npm install --save-dev mocha cypress-multi-reporters mochawesome
#RUN npm i --save-dev mochawesome-merge
#RUN npm i --save-dev mochawesome-report-generator