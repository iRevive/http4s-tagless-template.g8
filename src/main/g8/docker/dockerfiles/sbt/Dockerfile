FROM eed3si9n/sbt:sbt1.3.2-jdk11-alpine

USER root
RUN set -x && \
    apk --update add --no-cache docker && \
    apk add --no-cache py-pip python-dev libffi-dev openssl-dev gcc libc-dev make && \
    pip install --upgrade pip && \
    pip install docker-compose

USER 1001

## Fetch application dependencies

RUN mkdir project

COPY --chown=1001:0 build.sbt .scalafmt.conf /opt/workspace/
COPY --chown=1001:0 project/build.properties /opt/workspace/project
COPY --chown=1001:0 project/Dependencies.scala /opt/workspace/project
COPY --chown=1001:0 project/DockerEnvironment.scala /opt/workspace/project
COPY --chown=1001:0 project/plugins.sbt /opt/workspace/project

RUN sbt update

RUN rm -f .scalafmt.conf && rm -rf /opt/workspace/*