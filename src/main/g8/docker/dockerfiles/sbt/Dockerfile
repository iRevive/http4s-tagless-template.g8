FROM eed3si9n/sbt:sbt1.3.4-jdk11-alpine

RUN set -x && \
    apk --update add --no-cache docker && \
    apk add --no-cache py-pip python-dev libffi-dev openssl-dev gcc libc-dev make && \
    pip install --upgrade pip && \
    pip install docker-compose

## Fetch application dependencies

RUN mkdir project

COPY build.sbt .scalafmt.conf /opt/workspace/
COPY project/build.properties /opt/workspace/project
COPY project/Dependencies.scala /opt/workspace/project
COPY project/DockerEnvironment.scala /opt/workspace/project
COPY project/plugins.sbt /opt/workspace/project

RUN sbt update

RUN rm -f .scalafmt.conf && rm -rf /opt/workspace/*

ENTRYPOINT ["sbt"]